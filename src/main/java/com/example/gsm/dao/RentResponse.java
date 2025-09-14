package com.example.gsm.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RentResponse {
    private RevenueMetrics revenueMetrics;
    private List<RentInfo> rentInfos;
    private List<ChartData> successRefundChart;
    private List<TimeSeriesItem> revenueChart;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RevenueMetrics {
        private double totalRevenue;
        private double netRevenue;
        private double commission;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RentInfo {
        private String usernames;
        private String type ="OTP";
        private String status;
        private String accountId;
        private String phoneNumber;
        private String serviceCode;
        private String cost;
        private String createdAt;
        private String expiredAt;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChartData {
        private String serviceCode;
        private double successCount;
        private double refundCount;
        private double revenue;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TimeSeriesItem {
        private String label;
        private long success;
        private long refund;
        private long total;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RevenueData {
        private String label;
        private double totalRevenue;
    }
}
