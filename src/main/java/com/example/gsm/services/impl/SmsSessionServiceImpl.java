package com.example.gsm.services.impl;

import com.example.gsm.entity.SmsSession;
import com.example.gsm.entity.repository.SmsSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsSessionServiceImpl {

    private final SmsSessionRepository sessionRepo;

    /**
     * Đóng các session đã hết thời gian thuê
     */
    @Transactional
    public void autoCloseExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        log.info("[SmsSessionService] Auto closing expired sessions at {}", now);

        List<SmsSession> expiredSessions = sessionRepo.findByIsActiveTrueAndEndTimeBefore(now);

        for (SmsSession session : expiredSessions) {
            session.setActive(false);
            session.setEndTime(now); // chốt lại thời điểm kết thúc thực tế
            log.info("Closed expired session id={} phone={}", session.getId(), session.getPhoneNumber());
        }

        if (!expiredSessions.isEmpty()) {
            sessionRepo.saveAll(expiredSessions);
        }
    }

}
