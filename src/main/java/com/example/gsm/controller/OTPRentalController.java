package com.example.gsm.controller;

import com.example.gsm.dao.RentSimRequest;
import com.example.gsm.dao.RentSimResponse;
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
    public ResponseEntity<RentSimResponse> rentSim(@RequestBody RentSimRequest req,
                                                   Authentication authentication) {
         if (authentication == null) {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
         }
        String username = (String) authentication.getPrincipal();

        UserAccount user = userAccountRepository.findByWebInfoUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        Long accountId = user.getAccountId();

        RentSimResponse resp = simRentalService.rentSim(
                accountId,
                req.getStatusCode(),
                req.getPlatForm(),
                req.getCountryCode(),
                req.getTotalCost(),
                req.getRentDuration(),
                req.getServiceCodes(),
                req.getProvider(),
                req.getType()
        );

        return ResponseEntity.ok(resp);
    }

    @GetMapping("/order")
    public ResponseEntity<Map<String, List<Order>>> getOrdersGroupedByType(Authentication authentication,
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

        Map<String, List<Order>> groupedOrders = simRentalService.getOrdersGroupedByType(accountId, page, size);

        return ResponseEntity.ok(groupedOrders);
    }
    
}
