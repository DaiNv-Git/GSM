package com.example.gsm.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "sms_session")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmsSession {
    @Id private String id;
    private String campaignId;
    private String simId;
    private String deviceName;
    private String comPort;
    private String phoneNumber;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private boolean active;

    // NEW: thời gian hoạt động gần nhất (khi có inbound/outbound SMS)
    private LocalDateTime lastActivityAt;
}
