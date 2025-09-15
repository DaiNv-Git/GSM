package com.example.gsm.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CallRequest
{
    private TimeType timeType;
    private Integer year;
    private Integer month;
    private String countryCode;
}
