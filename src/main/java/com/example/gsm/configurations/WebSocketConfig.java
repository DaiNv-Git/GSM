package com.example.gsm.configurations;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // endpoint cho client connect ws://14.225.205.10:8081/ws-endpoint
        registry.addEndpoint("/ws-endpoint")
                .setAllowedOriginPatterns("*") // cho phép mọi origin
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // client subscribe các topic này
        registry.enableSimpleBroker("/topic");
        // client gửi lên server phải prefix bằng /app
        registry.setApplicationDestinationPrefixes("/app");
    }
}
