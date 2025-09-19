package com.example.gsm.services.impl;

import com.example.gsm.configurations.SequenceGeneratorService;
import com.example.gsm.dao.RegisterUserRequest;
import com.example.gsm.entity.UserAccount;
import com.example.gsm.entity.UserAccount.WebInfo;
import com.example.gsm.entity.repository.UserAccountRepository;
import com.example.gsm.exceptions.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.example.gsm.exceptions.ErrorCode.USER_EXISTED;
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAccountService {

    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final SequenceGeneratorService sequenceGeneratorService;


    public UserAccount register(RegisterUserRequest req) {
        if (repository.existsByWebInfoUsername(req.getUsername())) {
            throw new BadRequestException(USER_EXISTED);
        }

        WebInfo webInfo = new WebInfo();
        webInfo.setUsername(req.getUsername());
        webInfo.setPassword(passwordEncoder.encode(req.getPassword()));
        String accountId = sequenceGeneratorService.getNextSequence("user_accountId");

        UserAccount ua = UserAccount.builder()
                .accountId(Long.valueOf(accountId))
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .isActive(true)
                .isAdmin(false)
                .isAgent(false)
                .isDev(false)
                .balanceAmount(0d)
                .isPartner(false)
                .permission(List.of())
                .webInfo(webInfo)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return repository.save(ua);
    }
    public UserAccount deposit(Long accountId, Double amount) {
        log.info("Depositing {} to accountId {}", amount, accountId);

        Optional<UserAccount> user = repository.findByAccountId(accountId);
        if (!user.isPresent()) {
            throw new RuntimeException("User account not found");
        }
        UserAccount userAccount = user.get();
        // Nếu balance null thì set 0 trước
        if (userAccount.getBalanceAmount() == null) {
            userAccount.setBalanceAmount(0.0);
        }

        double newBalance = userAccount.getBalanceAmount() + amount;
        userAccount.setBalanceAmount(newBalance);
        userAccount.setUpdatedAt(Instant.now());

        UserAccount saved = repository.save(userAccount);

        log.info("Deposited successfully. New balance: {}", saved.getBalanceAmount());
        return saved;
    }
}
