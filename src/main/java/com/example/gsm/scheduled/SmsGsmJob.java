package com.example.gsm.scheduled;

import com.example.gsm.services.impl.SmsSenderServiceImpl;
import com.example.gsm.services.impl.SmsSessionServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmsGsmJob {

    private final SmsSenderServiceImpl smsSenderService;
    private final SmsSessionServiceImpl smsSessionService;

    /**
     * Định kỳ quét gửi sms
     */
    @Scheduled(fixedDelayString = "1000")
    public void sendSmsGsm() {
        smsSenderService.processBatch();
    }

    /**
     * Định kỳ quét session đang active và close nếu quá hạn.
     */
    @Scheduled(fixedDelay = 600000) // mỗi 10 phút
    public void autoCloseExpiredSessions() {
        log.info("[SmsGsmJob] Start job autoCloseExpiredSessions...");
        smsSessionService.autoCloseExpiredSessions();
        log.info("[SmsGsmJob] End job autoCloseExpiredSessions.");
    }
}
