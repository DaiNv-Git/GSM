package com.example.gsm.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RentServicesResponse {
    private String id;
    private String code;                   // service code
    private String text;                   // service name
    private String image;                  // logo
    private Map<String, Integer> prices;   // bảng giá theo label
}
