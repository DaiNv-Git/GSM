package com.example.gsm.services.impl;

import com.example.gsm.dao.request.LoginRequest;
import com.example.gsm.dao.response.LoginResponse;
import com.example.gsm.dao.request.RefreshRequest;
import com.example.gsm.entity.UserAccount;
import com.example.gsm.entity.repository.UserAccountRepository;
import com.example.gsm.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
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
            throw new RuntimeException("passWord");
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

    @Override
    public LoginResponse refresh(RefreshRequest req) {
        String token = req.getRefreshToken();
        String username = jwtService.extractUsername(token);

        // Kiểm tra token hợp lệ
        if (!jwtService.isValid(token, username)) {
            throw new RuntimeException("Refresh token không hợp lệ hoặc đã hết hạn");
        }

        UserAccount u = repo.findByWebInfoUsername(username).orElseThrow();

        // Lấy roles từ user
        List<String> roles = new ArrayList<>();
        if (Boolean.TRUE.equals(u.isAdmin())) roles.add("ADMIN");
        if (Boolean.TRUE.equals(u.isDev())) roles.add("DEV");
        if (Boolean.TRUE.equals(u.isPartner())) roles.add("PARTNER");
        if (Boolean.TRUE.equals(u.isAgent())) roles.add("AGENT");
        if (roles.isEmpty()) roles.add("USER");

        // Sinh access token mới
        String newAccessToken = jwtService.generateToken(username, roles, accessExpMs);
        long newAccessExpAt = System.currentTimeMillis() + accessExpMs;

        // Giữ nguyên refresh token cũ
        Date refreshExp = jwtService.extractExpiration(token);
        long refreshExpAt = refreshExp.getTime();

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .accessTokenExpiresAt(newAccessExpAt)
                .refreshToken(token)
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
