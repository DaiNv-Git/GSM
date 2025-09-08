package com.example.gsm.dao;

import lombok.Data;

@Data
public class DashboardRequest {
    private TimeType timeType;
    private Integer year;
    private Integer month;
}
