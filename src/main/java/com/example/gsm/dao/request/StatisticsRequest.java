package com.example.gsm.dao.request;

import com.example.gsm.dao.TimeType;
import lombok.Data;

@Data
public class StatisticsRequest {
    private TimeType timeType;
    private Integer year;
    private Integer month;
}
