package com.example.gsm.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor

public class SmsResponse {

    private RevenueMetrics revenueMetrics;
    private List<TimeSeriesItem> lineChar;

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
    public static class TimeSeriesItem {
        private String label;
        private long success;
        private long refund;
        private long total;
    }
}
