package com.example.gsm.dao;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class TypeTotalsResponse {
    private List<TypeSeries> seriesByType;
    private List<String> labels;

    @Data
    @Builder
    public static class TypeSeries {
        private String type;
        private double totalAmount;     // tổng cost của type trong kỳ
        private List<Double> amounts;   // mảng theo labels (đủ length)
    }
}
