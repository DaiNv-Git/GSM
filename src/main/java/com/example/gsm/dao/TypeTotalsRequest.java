package com.example.gsm.dao;

import lombok.Data;

@Data
public class TypeTotalsRequest {
    private TimeType timeType; // DAY | WEEK | MONTH
    private Integer year;      // bắt buộc
    private Integer month;     // bắt buộc nếu DAY hoặc WEEK
}
