package com.example.gsm.dao;

import lombok.Data;
import java.util.List;

@Data
public class DashboardResponse {
    private String filter; 
    private double revenue;
    private Summary summary;
    private List<ServiceInfo> services;
    private List<ChartPoint> chart;

    @Data
    public static class Summary {
        private long totalOtp;
        private long totalRent;
        private long totalSms;
        private long totalCall;
        private long proxy;
        private String growthRate;
        private String trend;
        private long country;
    }

    @Data
    public static class ServiceInfo {
        private String name;
        private String code;
        private double amount;
        private long success;
        private long failed;
        private String growthRate;
        private String trend;
    }

    @Data
    public static class ChartPoint {
        private String label; 
        private long count;
    }
}

