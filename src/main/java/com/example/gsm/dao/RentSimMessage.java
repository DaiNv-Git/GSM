package com.example.gsm.dao;

import lombok.Data;

@Data
public class RentSimMessage {
    private String phoneNumber;
    private String comNumber;
    private String customerId;
    private String serviceCode;
    private int waitingTime;
}
