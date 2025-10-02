package com.example.gsm.services.impl;

import com.example.gsm.entity.SmsSession;
import com.example.gsm.entity.repository.SmsSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsSessionServiceImpl {

    private final SmsSessionRepository sessionRepo;

    // Timeout cho session (5 phút)
    private static final long SESSION_TIMEOUT_MINUTES = 5;

    public void autoCloseExpiredSessions() {
        List<SmsSession> activeSessions = sessionRepo.findByIsActiveTrue();

        LocalDateTime now = LocalDateTime.now();
        for (SmsSession session : activeSessions) {
            if (session.getLastActivityAt() != null &&
                    session.getLastActivityAt().plusMinutes(SESSION_TIMEOUT_MINUTES).isBefore(now)) {
                session.setActive(false);
                session.setEndTime(now);
                sessionRepo.save(session);
                log.info("Closed session {} for phone {} due to inactivity",
                        session.getId(), session.getPhoneNumber());
            }
        }
    }
}
