package com.example.gsm.dao;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OtpResponse {
    private RevenueMetrics revenueMetrics;
    private List<PlatformBreakdown> barChart;
    private List<TimeSeriesItem> lineChart;

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
    public static class PlatformBreakdown {
        private String platformName;
        private long orderCountSuccess;
        private long orderCountRefund;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TimeSeriesItem {
        private String label;
        private double totalRevenue;
    }
}

