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
    private List<String> usernames;
    private List<ChartData> successRefundChart;
    private List<RevenueData> revenueChart;

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
    public static class ChartData {
        private String serviceCode;
        private double successCount;
        private double refundCount;
        private double revenue;
    }
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RevenueData {
        private String label;
        private double totalRevenue;
    }
}
