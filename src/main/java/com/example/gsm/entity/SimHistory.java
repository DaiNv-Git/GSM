package com.example.gsm.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "sim_history")
public class SimHistory {

    @Id
    private String id;

    private Long accountId;
    private String phoneNumber;
    private String serviceCode;
    private String countryCode;
    private Double cost;
    private String status;
    private Date startTime;
    private Date expiredAt;
    private String orderId;
    private Long requestTime;
    private String otp;
}
