package com.example.gsm.dao.request;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class CreateOrderRequest {
    private String type;
    private Double cost;
    private Long accountId;
    private String countryCode;
    private String statusCode;
    private String platform;
    private List<StockRequest> stock;

    @Data
    public static class StockRequest {
        private String phone;
        private String provider;
        private String serviceCode;
        private List<String> messages;
        private Date expiredAt;
    }
}

