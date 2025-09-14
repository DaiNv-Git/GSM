package com.example.gsm.services.impl;

import com.example.gsm.dao.RentSimResponse;
import com.example.gsm.dao.StatusCode;
import com.example.gsm.entity.*;

import com.example.gsm.entity.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SimRentalService {

    private final SimRepository simRepository;
    private final UserAccountRepository userAccountRepository;
    private final OrderRepository orderRepository;
    private final ServiceRepository serviceRepository;
    private final CountryRepository countryRepository;
    @Transactional
    public RentSimResponse rentSim(Long accountId,
                                   StatusCode statusCode,
                                   String flatform,
                                   String countryCode,
                                   Double totalCost,
                                   int rentDuration,
                                   List<String> services,
                                   String provider,String type) {
    
        UserAccount user = userAccountRepository.findByAccountId(accountId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
    
        if (user.getBalanceAmount() < totalCost) {
            throw new RuntimeException("Số dư không đủ");
        }
        
        List<Sim> sims = simRepository.findByCountryCode(countryCode);
        Sim selectedSim = null;
        for (Sim sim : sims) {
            long rented = orderRepository.countByPhoneAndServiceCodesAndExpiredAtAfter(sim.getPhoneNumber(), services,new Date());
            if (rented <= 0) {
                selectedSim = sim;
                break;
            }
        }
        if (selectedSim == null) {
            throw new RuntimeException("Không còn sim khả dụng");
        }

        Double currentBalance = user.getBalanceAmount();

        if (statusCode == StatusCode.SUCCESS) {
            user.setBalanceAmount(currentBalance - totalCost);
        } else if (statusCode == StatusCode.REFUNDED) {
            user.setBalanceAmount(currentBalance + totalCost);
        }

        userAccountRepository.save(user);

        
        Order order = new Order();
        order.setAccountId(accountId);
        order.setType(type);
        order.setPlatform(flatform);
        order.setCost(totalCost);
        order.setCountryCode(countryCode);
        order.setStatusCode(statusCode.toString());
        order.setCreatedAt(new Date());
        order.setUpdatedAt(new Date());
        
        long expiredMillis = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(rentDuration);
    
        Sim finalSelectedSim = selectedSim;
        List<Order.Stock> stockList = services.stream().map(serviceCode -> {
            Order.Stock stock = new Order.Stock();
            stock.setPhone(finalSelectedSim.getPhoneNumber());
            stock.setServiceCode(serviceCode);
            stock.setProvider(provider);
            stock.setExpiredAt(new Date(expiredMillis));
            return stock;
        }).toList();
    
        order.setStock(stockList);
        order = orderRepository.save(order);
    
        Country foundCountry = countryRepository.findByCountryCode(countryCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy countryCode: " + countryCode));
    
        String combinedServiceNames = services.stream()
                .map(code -> serviceRepository.findByCode(code)
                        .map(ServiceEntity::getText)
                        .orElse("Không rõ dịch vụ"))
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    
        return new RentSimResponse(
                order.getId(),
                selectedSim.getPhoneNumber(),
                combinedServiceNames,     
                String.join(",", services),
                foundCountry.getCountryName(),
                rentDuration,
                foundCountry.getCountryCode()
        );
    }

    public Map<String, List<Order>> getOrdersGroupedByType(Long accountId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        // Chỉ lấy statusCode SUCCESS
        Page<Order> orderPage = orderRepository.findByAccountIdAndStatusCode(accountId, "SUCCESS", pageable);

        Date now = new Date();

        List<Order> filteredOrders = orderPage.getContent().stream()
                .filter(order -> order.getStock() != null && order.getStock().stream()
                        .anyMatch(s -> s.getExpiredAt() != null && s.getExpiredAt().after(now)))
                .collect(Collectors.toList());

        Comparator<Order> comparator = (o1, o2) -> {
            Date o1MaxExpired = o1.getStock().stream()
                    .filter(s -> s.getExpiredAt() != null)
                    .map(Order.Stock::getExpiredAt)
                    .max(Date::compareTo)
                    .orElse(new Date(0));
            Date o2MaxExpired = o2.getStock().stream()
                    .filter(s -> s.getExpiredAt() != null)
                    .map(Order.Stock::getExpiredAt)
                    .max(Date::compareTo)
                    .orElse(new Date(0));

            boolean o1Expired = o1MaxExpired.after(now);
            boolean o2Expired = o2MaxExpired.after(now);

            if (o1Expired && !o2Expired) return -1;
            if (!o1Expired && o2Expired) return 1;

            return o2.getCreatedAt().compareTo(o1.getCreatedAt());
        };

        List<Order> sorted = filteredOrders.stream()
                .sorted(comparator)
                .collect(Collectors.toList());

        // Group theo type
        Map<String, List<Order>> grouped = sorted.stream()
                .collect(Collectors.groupingBy(Order::getType, LinkedHashMap::new, Collectors.toList()));

        return grouped;
    }
    
    }
