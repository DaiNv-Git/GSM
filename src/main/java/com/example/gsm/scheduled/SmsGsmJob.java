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
        log.info("[SmsGsmJob] Start job sendSmsGsm...");
        smsSenderService.processBatch();
        log.info("[SmsGsmJob] End job sendSmsGsm.");
    }

    /**
     * Định kỳ quét session đang active và close nếu quá timeout
     */
    @Scheduled(fixedDelay = 60000) // mỗi phút
    public void autoCloseExpiredSessions() {
        log.info("[SmsGsmJob] Start job autoCloseExpiredSessions...");
        smsSessionService.autoCloseExpiredSessions();
        log.info("[SmsGsmJob] End job autoCloseExpiredSessions.");
    }
}
