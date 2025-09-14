package com.example.gsm.dao;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
public class RentSimResponse {
    private String orderNumber;
    private String phoneNumber;
    private String serviceName;
    private String serviceCode;
    private String countryName;
    private int rentDuration;
    private String countryCode;
}
