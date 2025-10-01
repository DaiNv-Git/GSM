package com.example.gsm.entity.repository;

import com.example.gsm.entity.UserAccount;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserAccountRepository extends MongoRepository<UserAccount, String> {
    Optional<UserAccount> findByWebInfoUsername(String username);
    boolean existsByWebInfoUsername(String username);
    Optional<UserAccount> findByAccountId(Long accountId);


}
