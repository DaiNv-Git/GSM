package com.example.gsm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestPushController {

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @GetMapping("/test-push")
    public String testPush() {
        String msg = """
        {
          "_id": "13ec223a-965e-4a34-b056-bee301aa2903",
          "phoneNumber": "07093658251",
          "status": "ACTIVE",
          "countryCode": "JPN",
          "deviceName": "DESKTOP-NQ4T5M8",
          "comName": "COM110",
          "content": "hello dâidj"
        }
        """;

        // Gửi message tới topic
        simpMessagingTemplate.convertAndSend("/topic/sms-job-topic", msg);

        return "✅ Test message pushed to /topic/sms-job-topic";
    }
}
