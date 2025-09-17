package com.example.gsm.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.UUID;

/**
 * Bảng SIM chứa danh sách sim đang có trong hệ thống.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "sims")
public class Sim {

    @Id
    private String id = UUID.randomUUID().toString();

    /** Số điện thoại SIM */
    private String phoneNumber;

    /** Tổng doanh thu từ SIM này */
    private Double revenue;

    /** Trạng thái SIM: active, inactive */
    private String status;

    /** Mã quốc gia */
    private String countryCode ="JVM";

    private String deviceName;
    private String comName;
    private String simProvider;
    private String ccid;
    private String content;

    private Date lastUpdated;
    private Date activeDate;
}
