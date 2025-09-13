package com.example.gsm.dao;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class OrderResponse {
    private String id;
    private String type;
    private Double cost;
    private Long accountId;
    private String countryCode;
    private String statusCode;
    private String platform;
    private Boolean isRefund;
    private Boolean isActive;
    private Date createdAt;
    private Date updatedAt;
    private List<StockResponse> stock;

    @Data
    public static class StockResponse {
        private String phone;
        private String provider;
        private String serviceCode;
        private List<String> messages;
        private Date expiredAt;
    }
}
