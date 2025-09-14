package com.example.gsm.dao;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RentSimResponse {
    private String orderNumber;
    private String phoneNumber;
    private String serviceCode;
    private String serviceName;
    private String countryName;
    private int rentDuration;
    private String countryCode;

}
