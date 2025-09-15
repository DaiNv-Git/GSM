package com.example.gsm.controller;
import com.example.gsm.dao.OtpMessage;
import com.example.gsm.dao.SendRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    // Nhận dữ liệu từ client (sdt + otp) => push ra topic cho các client khác
    @MessageMapping("/receive-otp") // client gửi đến /app/receive-otp
    public void receiveOtp(OtpMessage message) {
        System.out.println("Received OTP message: " + message);
        // push lại cho tất cả client đang subscribe topic /topic/otp
        messagingTemplate.convertAndSend("/topic/otp", message);
    }

    @MessageMapping("/send-request") // client gửi đến /app/send-request
    public void sendRequest(SendRequest request) {
        System.out.println("Received SendRequest: " + request);
        // push lại cho tất cả client đang subscribe topic /topic/request
        messagingTemplate.convertAndSend("/topic/request", request);
    }
}
