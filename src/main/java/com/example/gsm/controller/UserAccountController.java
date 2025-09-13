package com.example.gsm.controller;

import com.example.gsm.dao.LoginRequest;
import com.example.gsm.dao.LoginResponse;
import com.example.gsm.dao.RegisterUserRequest;
import com.example.gsm.dao.UserMeResponse;
import com.example.gsm.entity.UserAccount;
import com.example.gsm.entity.repository.UserAccountRepository;
import com.example.gsm.services.AuthService;
import com.example.gsm.services.impl.UserAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        String username = (String) auth.getPrincipal();
        UserAccount user = repo.findByWebInfoUsername(username)
                .orElseThrow();

        List<String> roles = user.isAdmin() ? List.of("ADMIN") : List.of("USER");

        var res = UserMeResponse.builder()
                .id(user.getId())
                .username(username)
                .balanceAmount(user.getBalanceAmount())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .isActive(user.isActive())
                .roles(roles)
                .build();

        return ResponseEntity.ok(res);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
