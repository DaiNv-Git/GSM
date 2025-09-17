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
    public void handleRentSim(@Payload RentSimMessage message) {
        // Xử lý logic bạn muốn khi nhận message từ client
        System.out.println("Nhận từ client: " + message);

        // Ví dụ: broadcast message nhận được tới tất cả client
        messagingTemplate.convertAndSend("/topic/send-otp", message);
    }
}
