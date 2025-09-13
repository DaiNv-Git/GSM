package com.example.gsm.services.impl;

import com.example.gsm.dao.RegisterUserRequest;
import com.example.gsm.entity.UserAccount;
import com.example.gsm.entity.UserAccount.WebInfo;
import com.example.gsm.entity.repository.UserAccountRepository;
import com.example.gsm.exceptions.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

import static com.example.gsm.exceptions.ErrorCode.USER_EXISTED;

@Service
@RequiredArgsConstructor
public class UserAccountService {

    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;


    public UserAccount register(RegisterUserRequest req) {
        if (repository.existsByWebInfoUsername(req.getUsername())) {
            throw new BadRequestException(USER_EXISTED);
        }

        WebInfo webInfo = new WebInfo();
        webInfo.setUsername(req.getUsername());
        webInfo.setPassword(passwordEncoder.encode(req.getPassword()));

        UserAccount ua = UserAccount.builder()
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .isActive(true)
                .isAdmin(false)
                .isAgent(false)
                .isDev(false)
                .isPartner(false)
                .permission(List.of())
                .webInfo(webInfo)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return repository.save(ua);
    }
}
