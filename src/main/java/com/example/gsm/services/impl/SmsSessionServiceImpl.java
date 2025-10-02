package com.example.gsm.services.impl;

import com.example.gsm.entity.SmsSession;
import com.example.gsm.entity.repository.SmsSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsSessionServiceImpl {

    private final SmsSessionRepository sessionRepo;

    // Timeout cho session (5 phút)
    private static final long SESSION_TIMEOUT_MINUTES = 5;

    /**
     * Định kỳ quét session đang active và close nếu quá timeout
     */
    @Scheduled(fixedDelay = 60000) // mỗi phút
    public void autoCloseExpiredSessions() {
        List<SmsSession> activeSessions = sessionRepo.findByIsActiveTrue();

        LocalDateTime now = LocalDateTime.now();
        for (SmsSession session : activeSessions) {
            if (session.getLastActivityAt() != null &&
                    session.getLastActivityAt().plusMinutes(5).isBefore(now)) {
                session.setActive(false);
                session.setEndTime(now);
                sessionRepo.save(session);
                log.info("Closed session {} for phone {} due to inactivity",
                        session.getId(), session.getPhoneNumber());
            }
        }
    }
}
