package com.example.gsm.services;

import com.example.gsm.entity.SmsMessageWsk;

import java.util.List;

public interface ChatHistoryService {

    List<SmsMessageWsk> getChatHistoryByPhone(String phoneNumber);

    List<SmsMessageWsk> getChatHistoryBySession(String sessionId) ;
}
