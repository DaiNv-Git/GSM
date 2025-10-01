package com.example.gsm.services;

import com.example.gsm.dao.request.LoginRequest;
import com.example.gsm.dao.response.LoginResponse;
import com.example.gsm.dao.request.RefreshRequest;

public interface AuthService {
    LoginResponse login(LoginRequest req);
    LoginResponse refresh(RefreshRequest req);
}
