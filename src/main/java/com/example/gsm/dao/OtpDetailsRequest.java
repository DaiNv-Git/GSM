package com.example.gsm.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtpDetailsRequest {
    private TimeType timeType;
    private Integer year;
    private Integer month;
    private Integer pageSize;
    private Integer pageNumber;
}
