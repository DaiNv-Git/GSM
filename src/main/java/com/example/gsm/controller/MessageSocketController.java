package com.example.gsm.controller;

import com.example.gsm.dao.MessageRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
public class MessageSocketController {

    @MessageMapping("/incoming")
    @SendTo("/topic/sms")
    public MessageRequest receiveFromSocket(MessageRequest request) {
        log.info("Received from WebSocket: {}111111", request);
        return request;
    }
}
