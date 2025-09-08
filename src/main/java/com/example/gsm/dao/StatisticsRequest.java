package com.example.gsm.dao;

import lombok.Data;

@Data
public class StatisticsRequest {
    private TimeType timeType;
    private Integer year;
    private Integer month;
}
