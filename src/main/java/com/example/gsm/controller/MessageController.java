package com.example.gsm.controller;

import com.example.gsm.dao.MessageRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/messages")
public class MessageController {
    private final SimpMessagingTemplate messagingTemplate;

    public MessageController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping
    public String receiveMessage(@RequestBody MessageRequest request) {
        log.info("Received message: {}", request);

        messagingTemplate.convertAndSend("/topic/sms", request);

        return "Message received successfully";
    }
}
