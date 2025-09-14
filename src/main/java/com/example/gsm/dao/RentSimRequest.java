package com.example.gsm.dao;
import lombok.Data;

@Data
public class RentSimRequest {
    private String serviceCode;
    private String countryCode;
    private Double totalCost;
    private int rentDuration; 
}
