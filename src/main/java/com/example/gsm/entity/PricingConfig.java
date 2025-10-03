package com.example.gsm.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "pricing_config")
public class PricingConfig {

    @Id
    private String id;

    private String smsType;       // ONE_WAY | TWO_WAY
    private double pricePerSms;   // giá / tin nhắn
    private double pricePerMinute;// giá / phút (áp dụng cho 2 chiều)

    private LocalDateTime validFrom;
    private LocalDateTime validTo;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
