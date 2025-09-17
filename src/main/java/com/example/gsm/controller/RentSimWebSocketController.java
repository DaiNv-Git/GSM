package com.example.gsm.controller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@Slf4j
@RequiredArgsConstructor
public class RentSimWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/receive-otp")
    public void handleRentSim(@Payload String message) {
        log.info("=== [RentSimWebSocketController] Nhận request /api/message ===");
        log.debug("Payload JSON: " + message);
        System.out.println("📩 Nhận từ client: " + message);
//        messagingTemplate.convertAndSend("/topic/send-otp", message);
    }
}
