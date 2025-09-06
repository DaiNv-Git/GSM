package com.example.gsm.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Data
@Document(collection = "orders")
public class Order {

    @Id
    private String id;

    private String type;
    private Double cost;
    private Stock stock;
    private Long accountId;
    private Boolean isRefund;
    private Boolean isActive;
    private Date createdAt;
    private Date updatedAt;
    private Integer discountRate;
    private String countryCode;
    private String statusCode;
    private String platform;
    private Integer __v;

    @Data
    public static class Stock {
        private String phone;
        private String provider;
        private String serviceCode;
        private List<String> messages;
        private Date expiredAt;
    }
}
