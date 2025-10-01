package com.example.gsm.dao.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CountryPriceDTO {
    private String serviceCode;
    private String serviceName;
    private String serviceImage;
    private String countryCode;
    private String countryName;
    private String flagImage;

    private double minPrice;
    private double maxPrice;
    private double pricePerDay;
}

