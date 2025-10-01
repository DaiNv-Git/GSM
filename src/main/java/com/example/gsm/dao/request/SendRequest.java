package com.example.gsm.dao.request;

// Gửi đi: số điện thoại, customerId, mã quốc gia, mã dịch vụ
import lombok.Data;

@Data
public class SendRequest {
    private String phoneNumber;
    private String customerId;
    private String countryCode;
    private String serviceCode;
}
