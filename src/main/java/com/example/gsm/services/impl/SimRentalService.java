package com.example.gsm.services.impl;

import com.example.gsm.dao.RentSimResponse;
import com.example.gsm.entity.*;

import com.example.gsm.entity.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SimRentalService {

    private final SimRepository simRepository;
    private final SimHistoryRepository simHistoryRepository;
    private final UserAccountRepository userAccountRepository;
    private final OrderRepository orderRepository;
    private final ServiceRepository serviceRepository;
    private final CountryRepository countryRepository;
//    private final OtpWebSocketClient otpWebSocketClient; // đã cấu hình WebSocket client
public RentSimResponse rentSim(Long accountId, String serviceCode, String countryCode, Double totalCost,int rentDuration) {
    
    UserAccount user = userAccountRepository.findByAccountId(accountId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

    if (user.getBalanceAmount() < totalCost) {
        throw new RuntimeException("Số dư không đủ");
    }

    List<Sim> sims = simRepository.findByCountryCode(countryCode); 

    Sim selectedSim = null;
    for (Sim sim : sims) {
        boolean rented = simHistoryRepository.existsByPhoneNumberAndExpiredAtAfter(sim.getPhoneNumber(), new Date());
        if (!rented) {
            selectedSim = sim;
            break;
        }
    }
    if (selectedSim == null) {
        throw new RuntimeException("Không còn sim khả dụng");
    }

    user.setBalanceAmount(user.getBalanceAmount() - totalCost);
    userAccountRepository.save(user);

    // 4. Tạo Order
    Order order = new Order();
    order.setAccountId(accountId);
    order.setType("buy.otp.service");
    order.setCost(totalCost);
    order.setCountryCode(countryCode);
    order.setStatusCode("SUCCESS");
    order.setCreatedAt(new Date());
    order.setUpdatedAt(new Date());

    Order.Stock stock = new Order.Stock();
    stock.setPhone(selectedSim.getPhoneNumber());
    stock.setServiceCode(serviceCode);
    stock.setProvider("ProviderName");
    stock.setExpiredAt(new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(rentDuration)));


    order.setStock(List.of(stock));
    order = orderRepository.save(order);

    Long requestTime = System.currentTimeMillis();
    SimHistory history = SimHistory.builder()
            .accountId(accountId)
            .phoneNumber(selectedSim.getPhoneNumber())
            .serviceCode(serviceCode)
            .countryCode(countryCode)
            .cost(totalCost)
            .status("active")
            .startTime(new Date())
            .expiredAt(stock.getExpiredAt())
            .orderId(order.getId())
            .requestTime(requestTime)
            .build();
    simHistoryRepository.save(history);

    // 6. Gửi request sang WebSocket để nhận OTP (bật khi cần)
//    otpWebSocketClient.sendRequest(
//            countryCode,
//            selectedSim.getPhoneNumber(),
//            serviceCode,
//            requestTime
//    );

    String serviceName = serviceRepository.findByCode(serviceCode)
            .map(ServiceEntity::getText)
            .orElse("Không rõ dịch vụ");

    Country foundCountry = countryRepository.findByCountryCode(countryCode)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy countryCode: " + countryCode));

    return new RentSimResponse(
            order.getId(),
            selectedSim.getPhoneNumber(),
            serviceCode,
            serviceName,
            foundCountry.getCountryName(),
            rentDuration,
            foundCountry.getCountryCode()
    );

}

}
