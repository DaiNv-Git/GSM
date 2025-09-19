package com.example.gsm.services;

import com.example.gsm.entity.UserAccount;

public interface UserAccountService {
    UserAccount deposit(Long accountId, Double amount);
}
