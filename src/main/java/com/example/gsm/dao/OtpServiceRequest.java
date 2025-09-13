package com.example.gsm.dao;

import com.example.gsm.entity.ServiceEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OtpServiceRequest {
    private String code;
    private String text;
    private double price;
    private String image;
    private boolean invertLogo;
    private boolean isActive;
    private boolean isPrivate;
    private double pricePerDay;
    private String countryCode;
    private int messageLimit;
    private double saleOffValue;

    private ServiceEntity.SupportFeatures supportFeatures;
    private List<ServiceEntity.RentDurationPrice> rentDurationPrices;
}
