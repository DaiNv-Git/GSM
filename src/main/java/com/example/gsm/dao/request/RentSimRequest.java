package com.example.gsm.dao.request;
import com.example.gsm.dao.StatusCode;
import lombok.Data;

import java.util.List;

@Data
public class RentSimRequest {
    private String type;
    private String countryCode;
    private Double totalCost;
    private int rentDuration;
    private String provider;
    private String platForm;
    private StatusCode statusCode;
    private int quantity;
    private List<String> serviceCodes;
}
