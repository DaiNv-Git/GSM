package com.example.gsm.dao;

import lombok.Data;

@Data
public class OtpMessage {
    private String phoneNumber;
    private String otp;
}
