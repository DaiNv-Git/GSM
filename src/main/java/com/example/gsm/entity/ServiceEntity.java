package com.example.gsm.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.annotation.Id;

import java.util.Date;
import java.util.List;

@Document(collection = "services")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceEntity {
    @Id
    private String id;

    private int index;
    private String code;         // LINE, WHATSAPP...
    private String text;         // tên hiển thị
    private double price;        // giá cơ bản
    private String image;        // logo
    private boolean invertLogo;
    private List<String> matches;
    private boolean isActive;
    private boolean isPrivate;
    private List<String> privatePartners;
    private double pricePerDay;
    private String countryCode;

    private SupportFeatures supportFeatures;
    private List<RentDurationPrice> rentDurationPrices;
    private List<String> callCenters;
    private int messageLimit;
    private double saleOffValue;

    private Date createdAt;
    private Date updatedAt;

    // nested class
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupportFeatures {
        private String partnerCode;
        private String partnerData;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RentDurationPrice {
        private String label;
        private String days;   // string or int? (dữ liệu có lúc "1", lúc 7)
        private double price;
    }
}

