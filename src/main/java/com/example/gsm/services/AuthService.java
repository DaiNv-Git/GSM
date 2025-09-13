package com.example.gsm.services;

import com.example.gsm.dao.LoginRequest;
import com.example.gsm.dao.LoginResponse;
import com.example.gsm.dao.RefreshRequest;

public interface AuthService {
    LoginResponse login(LoginRequest req);
    LoginResponse refresh(RefreshRequest req);
}
