package com.example.gsm.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "sms_campaigns")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmsCampaign {
    @Id private String id;
    private String name;
    private String type; // ONE_WAY | TWO_WAY
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime startTime;
    private LocalDateTime endTime; // nếu TWO_WAY, thời hạn thuê
    private String status; // NEW, RUNNING, COMPLETED, EXPIRED
    private String autoReplyTemplate; // template trả lời tự động (nếu có)
    private Integer totalMessages;
    private String country;
}