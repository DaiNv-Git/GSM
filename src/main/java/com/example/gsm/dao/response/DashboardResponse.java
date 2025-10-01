package com.example.gsm.dao.response;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DashboardResponse {

    private Overview overview;
    private List<StatusBox> statusBoxes;

    @Data
    @Builder
    public static class Overview {
        private long totalOtp;
        private long totalRent;
        private long totalSms;
        private long totalCall;
        private long totalProxy;
        private long countryCount;
        private double totalAmountAll;
        private double totalAmountSuccess;
        private String growthPercent;
    }

    @Data
    @Builder
    public static class StatusBox {
        private String type;
        private long success;
        private long fail;
        private String growthPercent;
    }
}
