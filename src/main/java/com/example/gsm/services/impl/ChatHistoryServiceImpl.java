package com.example.gsm.services.impl;

import com.example.gsm.entity.SmsMessageWsk;
import com.example.gsm.entity.repository.SmsMessageWskRepository;
import com.example.gsm.services.ChatHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
@RequiredArgsConstructor
public class ChatHistoryServiceImpl implements ChatHistoryService {
    private final SmsMessageWskRepository repository;

    @Override
    public List<SmsMessageWsk> getChatHistoryByPhone(String phoneNumber) {
        return repository.findByPhoneNumberOrderByCreatedAtAsc(phoneNumber);
    }
    @Override
    public List<SmsMessageWsk> getChatHistoryBySession(String sessionId) {
        return repository.findByChatSessionIdOrderByCreatedAtAsc(sessionId);
    }
}
