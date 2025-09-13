package com.example.gsm.dao;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CountryPriceDTO {
    private String countryCode;
    private String countryName;
    private String flagImage;

    private double minPrice;
    private double maxPrice;
    private double pricePerDay;
}

