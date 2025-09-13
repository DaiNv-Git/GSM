package com.example.gsm.services.impl;

import com.example.gsm.dao.LoginRequest;
import com.example.gsm.dao.LoginResponse;
import com.example.gsm.entity.UserAccount;
import com.example.gsm.entity.repository.UserAccountRepository;
import com.example.gsm.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserAccountRepository repo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${app.jwt.access-exp-ms}")
    private long accessExpMs;
    @Value("${app.jwt.refresh-exp-ms}")
    private long refreshExpMs;
    @Override
    public LoginResponse login(LoginRequest req) {
        UserAccount u = repo.findByWebInfoUsername(req.getUsername())
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        if (!passwordEncoder.matches(req.getPassword(), u.getWebInfo().getPassword())) {
            throw new RuntimeException("Sai mật khẩu");
        }

        List<String> roles = u.isAdmin() ? List.of("ADMIN") : List.of("USER");

        String accessToken = jwtService.generateToken(req.getUsername(), roles, accessExpMs);
        String refreshToken = jwtService.generateToken(req.getUsername(), roles, refreshExpMs);

        long accessExpAt  = System.currentTimeMillis() + accessExpMs;
        long refreshExpAt = System.currentTimeMillis() + refreshExpMs;

        return LoginResponse.builder()
                .accessToken(accessToken)
                .accessTokenExpiresAt(accessExpAt)
                .refreshToken(refreshToken)
                .refreshTokenExpiresAt(refreshExpAt)
                .id(u.getId())
                .username(u.getWebInfo().getUsername())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .roles(roles)
                .isDev(u.isDev())
                .isAdmin(u.isAdmin())
                .isPartner(u.isPartner())
                .isAgent(u.isAgent())
                .build();
    }
}
