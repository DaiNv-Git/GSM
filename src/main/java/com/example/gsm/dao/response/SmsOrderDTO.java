package com.example.gsm.dao.response;

import lombok.Data;

import java.time.Instant;

@Data
public class SmsOrderDTO {
    private String phone;
    private Integer durationMinutes;
    private Instant timestamp;
    private String content;
    private String serviceCode;
    private String otpCode; // sẽ nhận từ otpCodeObj.match
}
