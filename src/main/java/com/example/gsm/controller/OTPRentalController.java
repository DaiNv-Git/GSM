package com.example.gsm.controller;

import com.example.gsm.dao.request.RentSimRequest;
import com.example.gsm.dao.response.RentSimResponse;
import com.example.gsm.entity.Order;
import com.example.gsm.entity.UserAccount;
import com.example.gsm.entity.repository.UserAccountRepository;
import com.example.gsm.services.impl.SimRentalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/otp")
@RequiredArgsConstructor
public class OTPRentalController {

    private final SimRentalService simRentalService;
    private final UserAccountRepository userAccountRepository;
    @PostMapping("")
    public ResponseEntity<List<RentSimResponse>> rentSim(@RequestBody RentSimRequest req,
                                                         Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = (String) authentication.getPrincipal();

        UserAccount user = userAccountRepository.findByWebInfoUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        Long accountId = user.getAccountId();

        // service giờ trả về List
        List<RentSimResponse> respList = simRentalService.rentSim(
                accountId,
                req.getPlatForm(),
                req.getCountryCode(),
                req.getTotalCost(),
                req.getRentDuration(),
                req.getServiceCodes(),
                req.getProvider(),
                req.getType(),
                req.getQuantity(),
                req.getRecord()
        );

        return ResponseEntity.ok(respList);
    }


    @GetMapping("/order")
    public ResponseEntity<Map<String, List<Order>>> getOrdersGroupedByType(Authentication authentication,
                                                                           String phoneNumber,
                                                                           @RequestParam(defaultValue = "0") int page,
                                                                           @RequestParam(defaultValue = "10") int size) {
        // Lấy accountId từ Authentication
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String username = (String) authentication.getPrincipal();
        UserAccount user = userAccountRepository.findByWebInfoUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        Long accountId = user.getAccountId();

        Map<String, List<Order>> groupedOrders = simRentalService.getOrdersGroupedByType(accountId,phoneNumber, page, size);

        return ResponseEntity.ok(groupedOrders);
    }

    @PostMapping("/order/{orderId}/success")
    public ResponseEntity<Void> updateSuccess(@PathVariable String orderId) {
        simRentalService.updateOrderSuccess(orderId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/order/{orderId}/refund")
    public ResponseEntity<Void> updateRefund(@PathVariable String orderId) {
        simRentalService.updateOrderRefund(orderId);
        return ResponseEntity.ok().build();
    }

}
