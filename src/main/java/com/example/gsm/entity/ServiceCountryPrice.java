package com.example.gsm.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "service_country_prices")
public class ServiceCountryPrice {
    @Id
    private String id= UUID.randomUUID().toString();

    private String serviceCode;
    private String countryCode;
    private double minPrice;
    private double maxPrice;
    private double pricePerDay;
    private long time;
}
