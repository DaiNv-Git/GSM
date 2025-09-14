package com.example.gsm.controller;

import com.example.gsm.dao.RentSimRequest;
import com.example.gsm.dao.RentSimResponse;
import com.example.gsm.entity.UserAccount;
import com.example.gsm.entity.repository.UserAccountRepository;
import com.example.gsm.services.impl.SimRentalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/otp")
@RequiredArgsConstructor
public class OTPRentalController {

    private final SimRentalService simRentalService;
    private final UserAccountRepository userAccountRepository;
    @PostMapping("")
    public ResponseEntity<RentSimResponse> rentSim(@RequestBody RentSimRequest req,
                                                   Authentication authentication) {
        String username = (String) authentication.getPrincipal();

        UserAccount user = userAccountRepository.findByWebInfoUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        Long accountId = user.getAccountId();

        RentSimResponse resp = simRentalService.rentSim(
                accountId,
                req.getServiceCode(),
                req.getCountryCode(),
                req.getTotalCost(),
                req.getRentDuration()
        );

        return ResponseEntity.ok(resp);
    }
}
