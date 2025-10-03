package com.example.gsm.dao.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServicePrice {
    private String serviceCode;  // ví dụ: TINDER, LINE, TELEGRAM
    private Double price;        // giá thuê cho dịch vụ này
}