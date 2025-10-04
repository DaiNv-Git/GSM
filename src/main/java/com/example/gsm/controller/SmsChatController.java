package com.example.gsm.controller;

import com.example.gsm.entity.SmsMessageWsk;
import com.example.gsm.services.ChatHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class SmsChatController {
    private final ChatHistoryService chatHistoryService;

    // Lấy lịch sử chat theo số điện thoại
    @GetMapping("/history/sdt")
    public ResponseEntity<List<SmsMessageWsk>> getHistoryByPhone(
            @RequestParam String phoneNumber) {
        return ResponseEntity.ok(chatHistoryService.getChatHistoryByPhone(phoneNumber));
    }

    // Lấy lịch sử chat theo sessionId (nếu bạn có chia session)
    @GetMapping("/history/session")
    public ResponseEntity<List<SmsMessageWsk>> getHistoryBySession(
            @RequestParam String sessionId) {
        return ResponseEntity.ok(chatHistoryService.getChatHistoryBySession(sessionId));
    }

    @GetMapping("/history/campaign")
    public ResponseEntity<List<SmsMessageWsk>> getHistoryByCampaign(
            @RequestParam String campaignId) {
        return ResponseEntity.ok(chatHistoryService.getChatHistoryByCampaign(campaignId));
    }
}

