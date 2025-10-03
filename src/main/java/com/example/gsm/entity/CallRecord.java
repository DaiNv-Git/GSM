package com.example.gsm.entity;


import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "call_records")
public class CallRecord {
    @Id
    private String id;

    private String orderId;
    private Long customerId;

    private String fromNumber;   // số gọi tới
    private String toNumber;     // số SIM của mình
    private String deviceName;
    private String comPort;

    private String recordFile;   // link file record (nếu có)
    private String status;       // RECEIVED / ERROR / MISSED

    private Instant callStartTime;
    private Instant callEndTime;
    private Instant expireAt;
    private Instant createdAt;   // ✅ Thời gian lưu record vào DB

}
