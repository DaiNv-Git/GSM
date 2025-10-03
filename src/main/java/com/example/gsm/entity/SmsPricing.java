package com.example.gsm.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "sms_pricing")
public class SmsPricing {
    @Id
    private String id;
    private String type; // ONE_WAY, TWO_WAY
    private double pricePerSms;
    private double pricePerMinute; // cho 2 chiều
    private LocalDateTime createdAt = LocalDateTime.now();
}
