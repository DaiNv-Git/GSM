package com.example.gsm.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document("sms_reply")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SmsReply {
    @Id private String id;
    private String campaignId;
    private String sessionId; // có thể null nếu không thuộc session
    private String phoneNumber;
    private String content;
    private LocalDateTime receivedAt;
    private boolean autoReplied;
    private String autoReplyContent;
    private LocalDateTime autoRepliedAt;
}
