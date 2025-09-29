package com.example.gsm.dao.response;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SmsInfoResponse {
    private String simPhone;
    private String serviceCode;
    private String otp;
    private int durationMinutes;
}
