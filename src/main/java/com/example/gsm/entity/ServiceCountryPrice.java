package com.example.gsm.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "service_country_prices")
public class ServiceCountryPrice {
    @Id
    private String id;

    private String serviceCode;
    private String countryCode;

    private double minPrice;
    private double maxPrice;
    private double pricePerDay;
}
