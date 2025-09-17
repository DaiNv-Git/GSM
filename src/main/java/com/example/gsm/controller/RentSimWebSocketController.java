package com.example.gsm.controller;

import com.example.gsm.dao.RentSimMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class RentSimWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/receive-otp")
    public void handleRentSim(@Payload String message) {
        System.out.println("📩 Nhận từ client: " + message);
//        messagingTemplate.convertAndSend("/topic/send-otp", message);
    }
}
