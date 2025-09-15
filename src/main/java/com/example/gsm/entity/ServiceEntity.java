package com.example.gsm.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Document(collection = "services")
public class ServiceEntity {
    @Id
    private String id= UUID.randomUUID().toString();
    private int index;
    private String code;
    private String text;
    private double price;
    private String image;
    private boolean invertLogo;
    private List<String> matches;
    private boolean isActive;
    private boolean isPrivate;
    private List<String> privatePartners;
    private double pricePerDay;
    private String countryCode;
    private Map<String, Boolean> supportFeatures;
    private String partnerCode;
    private String partnerData;
    private List<RentDurationPrice> rentDurationPrices;
    private List<String> callCenters;
    private int messageLimit;
    private double saleOffValue;
    private String createdAt;
    private String updatedAt;

    @Data
    public static class RentDurationPrice {
        private String label;
        private String days;
        private double price;
    }
}
