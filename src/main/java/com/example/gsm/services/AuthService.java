package com.example.gsm.services;

import com.example.gsm.dao.LoginRequest;
import com.example.gsm.dao.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest req);
}
