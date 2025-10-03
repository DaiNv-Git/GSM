package com.example.gsm.dao.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
public class RentSimResponse {
    private String orderId;          // id của order
    private List<String> phones;     // danh sách số thuê được
    private String serviceName;      // tên hiển thị dịch vụ
    private String serviceCode;      // mã dịch vụ
    private String countryName;      // tên quốc gia
    private int rentDuration;        // thời gian thuê (phút)
    private String countryCode;      // mã quốc gia
    private Double price;            // giá cho service này

}
