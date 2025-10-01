// StatisticsSimpleResponse.java
package com.example.gsm.dao.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class StatisticsSimpleResponse {
    private Overview overview;                       // ✅ có revenue tổng & revenue theo type
    private List<TypeGroup> byAppType;
    private List<TypeSeries> timeSeries;

    @Data @Builder
    public static class Overview {
        private List<TypeTotal> types;               // 5 phần tử (OTP/Rent/SMS/Call/Proxy)
        private long countryCount;                   // distinct country
        private long successTotal;                   // tổng count SUCCESS
        private long refundTotal;                    // tổng count REFUNDED
        private long total;                          // tổng mọi trạng thái (count)
        private double revenueTotal;                 // ✅ tổng doanh thu toàn kỳ (sum(cost))
    }

    @Data @Builder
    public static class TypeTotal {
        private String type;                         // buy.otp.service...
        private long success;                        // count
        private long refund;                         // count
        private long total;                          // count
        private double revenue;                      // ✅ doanh thu của type (sum(cost))
    }

    @Data @Builder
    public static class TypeGroup {
        private String type;
        private long success;
        private long refund;
        private long total;
        private List<AppBreakdown> apps;
    }

    @Data @Builder
    public static class AppBreakdown {
        private String app;
        private long success;
        private long refund;
        private long total;
    }

    @Data @Builder
    public static class TypeSeries {
        private String type;
        private List<TimeSeriesItem> points;
    }

    @Data @Builder
    public static class TimeSeriesItem {
        private String label;
        private long success;
        private long refund;
        private long total;
    }
}
