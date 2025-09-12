package com.example.gsm.dao;

import lombok.Data;

@Data
public class MessageRequest {
    private String serviceName;   // tên service
    private String messageContent; // nội dung tin nhắn
    private String phoneNumber;    // số điện thoại gọi đến
}
