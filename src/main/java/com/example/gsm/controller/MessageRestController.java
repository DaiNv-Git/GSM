package com.example.gsm.controller;

import com.example.gsm.dao.MessageRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/messages")
public class MessageRestController {

    private final SimpMessagingTemplate messagingTemplate;

    public MessageRestController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping
    public String receiveMessage(@RequestBody MessageRequest request) {
        log.info("Received message via REST API: {}", request);
        messagingTemplate.convertAndSend("/topic/sms", request);
        return "Message received successfully";
    }
}
