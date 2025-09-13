package com.example.gsm.dao;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpRequest {
    private TimeType timeType;
    private Integer year;
    private Integer month;
}
