package com.example.gsm.controller;

import com.example.gsm.configurations.AuthUtils;
import com.example.gsm.dao.*;
import com.example.gsm.entity.UserAccount;
import com.example.gsm.entity.repository.UserAccountRepository;
import com.example.gsm.services.AuthService;
import com.example.gsm.services.impl.UserAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserAccountController {
    private final UserAccountService service;
    private final AuthService authService;
    private final UserAccountRepository repo;
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterUserRequest request) {
        UserAccount ua = service.register(request);
        ua.getWebInfo().setPassword(null);
        return ResponseEntity.ok(ua);
    }
    @GetMapping("/info")
    public ResponseEntity<UserMeResponse> me(Authentication auth) {
        if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        String username = (String) auth.getPrincipal();
        UserAccount user = repo.findByWebInfoUsername(username)
                .orElseThrow();

        List<String> roles = Boolean.TRUE.equals(user.isAdmin())
                ? List.of("ADMIN")
                : List.of("USER");

        var res = UserMeResponse.builder()
                .id(user.getId())
                .accountId(user.getAccountId())
                .username(username)
                .balanceAmount(user.getBalanceAmount())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .isActive(user.isActive())        
                .roles(roles)
                .isDev(user.isDev())              
                .isAdmin(user.isAdmin())           
                .isPartner(user.isPartner())       
                .isAgent(user.isAgent())           
                .build();

        return ResponseEntity.ok(res);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/deposit")
    public ResponseEntity<UserAccount> deposit(@Valid @RequestBody DepositRequest request) {
        log.info("API Deposit: accountId={}, amount={}", request.getAccountId(), request.getAmount());

        UserAccount updatedUser = service.deposit(request.getAccountId(), request.getAmount());

        return ResponseEntity.ok(updatedUser);
    }
}
