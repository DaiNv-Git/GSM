package com.example.gsm.dao.response;

import lombok.Data;

import java.time.Instant;

@Data
public class SmsOrderDTO {
    private String phone;          // số điện thoại SIM
    private int durationMinutes;   // thời gian thuê
    private Instant timestamp;     // ngày nhận SMS
    private String content;        // nội dung SMS
    private String serviceCode;    // dịch vụ
    private String otpCode;        // mã OTP trích từ content
}
