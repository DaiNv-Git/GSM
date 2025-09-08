package com.example.gsm.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Data
@Document(collection = "application_configs")
public class ApplicationConfig {
    @Id
    private String id = UUID.randomUUID().toString();
    private String code;

    private String applicationName;
    private Double price;
    private Integer maxSms;
    private Boolean isTopSale;
    private Double min;
    private Double max;
    private Double average;
    @CreatedDate
    private Instant createdAt;
}