package com.example.gsm.dao.response;

import lombok.Data;

@Data
public class OtpMessage {
    private String phoneNumber;
    private String otp;
}
