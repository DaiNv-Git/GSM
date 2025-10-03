package com.example.gsm.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "sms_message_wsk")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmsMessageWsk {
    @Id
    private String id;
    private String campaignId;
    private String phoneNumber;
    private String content;
    private String direction; // OUTBOUND | INBOUND
    private String status; // WAIT | SENDING | SENT | DELIVERED | FAILED | DLQ
    private String gsmMsgId; // id trả về từ GSM
    private String localMsgId; // id tạm để map khi gửi
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime updatedAt;
    private String lockedBy; // để claim
    private LocalDateTime lockedAt;
    private int retryCount;
    private String country;
    private String errorMsg;
    private String chatSessionId;
}
