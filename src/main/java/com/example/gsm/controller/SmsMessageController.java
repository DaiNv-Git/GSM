package com.example.gsm.controller;

import com.example.gsm.dao.response.SmsInfoResponse;
import com.example.gsm.entity.SmsMessage;
import com.example.gsm.entity.repository.SmsMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sms")
@RequiredArgsConstructor
public class SmsMessageController {

    private final SmsMessageRepository smsMessageRepository;

    
    @GetMapping("/by-order/{orderId}")
    public List<SmsInfoResponse> getByOrderId(@PathVariable String orderId) {
        List<SmsMessage> messages = smsMessageRepository.findByOrderId(orderId);

        return messages.stream()
                .collect(Collectors.groupingBy(SmsMessage::getSimPhone))
                .values().stream()
                .map(list -> list.stream()
                        .max(Comparator.comparing(SmsMessage::getTimestamp))
                        .orElse(null)
                )
                .filter(m -> m != null)
                .map(msg -> {
                    String otp = extractOtp(msg.getContent());
                    return new SmsInfoResponse(
                            msg.getSimPhone(),
                            msg.getServiceCode(),
                            otp,
                            msg.getDurationMinutes()
                    );
                })
                .collect(Collectors.toList());
    }


    private String extractOtp(String content) {
        if (content == null) return null;
        Matcher m = Pattern.compile("\\b\\d{4,8}\\b").matcher(content);
        return m.find() ? m.group() : null;
    }
}
