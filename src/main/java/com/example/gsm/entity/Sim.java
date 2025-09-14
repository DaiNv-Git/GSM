package com.example.gsm.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

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
    private String id;

    /** Số điện thoại SIM */
    private String phoneNumber;

    /** Tổng doanh thu từ SIM này */
    private Double revenue;

    /** Trạng thái SIM: active, inactive */
    private String status;

    /** Mã quốc gia */
    private String countryCode;
}
