package com.example.gsm.services.impl;

import com.example.gsm.entity.Sim;
import com.example.gsm.entity.SmsCampaign;
import com.example.gsm.entity.SmsMessageWsk;
import com.example.gsm.entity.SmsSession;
import com.example.gsm.entity.repository.*;
import com.example.gsm.services.ClaimService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class SmsSenderServiceImpl {

    private final ClaimService claimService;
    private final SmsMessageWskRepository messageRepo;
    private final SmsCampaignRepository campaignRepo;
    private final SimpMessagingTemplate messagingTemplate;
    private final SimRepository simRepository;
    private final OrderRepository orderRepository;
    private final int batchSize = 100;
    private final String workerId = UUID.randomUUID().toString();
    private final SmsSessionRepository smsSessionRepository;

    public void processBatch() {
        List<SmsMessageWsk> batch = claimService.claimBatch(batchSize, workerId);
        if (batch.isEmpty()) {
//            log.info("⚠️ Worker {} không claim được tin nào", workerId);
            return;
        }
        log.info("✅ Worker {} claim được {} tin nhắn", workerId, batch.size());

// Group batch theo country
        Map<String, List<SmsMessageWsk>> groupedByCountry =
                batch.stream().collect(Collectors.groupingBy(SmsMessageWsk::getCountry));

        groupedByCountry.forEach((country, messages) -> {
            if (messages.isEmpty()) return;

            String jobId = UUID.randomUUID().toString();
            String campaignId = messages.get(0).getCampaignId();
            log.info("🌍 Country={} campaignId={} có {} tin nhắn sẽ xử lý (jobId={})",
                    country, campaignId, messages.size(), jobId);

            // Lấy tất cả SIM active
            List<Sim> availableSims = selectAvailableSims(country);
            log.info("🔎 Found {} SIM active cho country={}", availableSims.size(), country);

            if (availableSims.isEmpty()) {
                log.warn("❌ Không có SIM khả dụng cho country={}, campaignId={}", country, campaignId);
                messages.forEach(msg -> {
                    msg.setStatus("FAILED_NO_SIM");
                    msg.setUpdatedAt(LocalDateTime.now());
                });
                messageRepo.saveAll(messages);
                return;
            }

            // Shuffle tin nhắn để random thứ tự
            Collections.shuffle(messages);

            // Gán SIM round-robin
            int simCount = availableSims.size();
            for (int i = 0; i < messages.size(); i++) {
                SmsMessageWsk msg = messages.get(i);
                Sim sim = availableSims.get(i % simCount);

                log.info("📩 Assign message={} cho SIM={} ({}), vòng={} ",
                        msg.getLocalMsgId(), sim.getPhoneNumber(), sim.getDeviceName(), (i % simCount));

                // Check session active
                SmsSession session = smsSessionRepository.findActiveBySimId(sim.getId());

                // Lấy campaign
                SmsCampaign campaign = campaignRepo.findById(campaignId).orElseThrow();

                if (session == null) {
                    session = new SmsSession();
                    session.setSimId(sim.getId());
                    session.setCampaignId(campaignId);
                    session.setDeviceName(sim.getDeviceName());
                    session.setComPort(sim.getComName());
                    session.setPhoneNumber(sim.getPhoneNumber());
                    session.setStartTime(LocalDateTime.now());
                    session.setEndTime(campaign.getEndTime());
                    session.setActive(true);
                    session.setLastActivityAt(LocalDateTime.now());
                    smsSessionRepository.save(session);
                    log.info("➕ Created new session={} cho SIM={}", session.getId(), sim.getPhoneNumber());
                } else {
                    session.setLastActivityAt(LocalDateTime.now());
                    smsSessionRepository.save(session);
                    log.info("♻️ Update lastActivity cho session={} SIM={}", session.getId(), sim.getPhoneNumber());
                }

                // Gắn sessionId cho message
                msg.setChatSessionId(session.getId());
                msg.setStatus("PENDING");
                msg.setSentAt(LocalDateTime.now());
                msg.setUpdatedAt(LocalDateTime.now());

                // Tạo job
                Map<String, Object> job = new HashMap<>();
                job.put("action", "SEND_GSM_SMS");
                job.put("jobId", jobId);
                job.put("campaignId", campaignId);
                job.put("country", country);
                job.put("phoneNumber", msg.getPhoneNumber());
                job.put("content", msg.getContent());
                job.put("simId", sim.getId());
                job.put("simPhoneNumber", sim.getPhoneNumber());
                job.put("deviceName", sim.getDeviceName());
                job.put("comName", sim.getComName());
                job.put("localMsgId", msg.getLocalMsgId());
                job.put("sessionId", session.getId());
                job.put("campaignStartTime", campaign.getStartTime() != null ? campaign.getStartTime().toString() : null);
                job.put("campaignEndTime", campaign.getEndTime() != null ? campaign.getEndTime().toString() : null);
                job.put("smsType", campaign.getType() != null ? campaign.getType() : "ONE_WAY");
                job.put("timeDuration", calculateDuration(campaign.getStartTime(), campaign.getEndTime()));

                // Push job
                messagingTemplate.convertAndSend("/topic/sms-job-topic", job);
                log.info("📡 Push job={} cho msg={} SIM={} campaign={}", jobId, msg.getLocalMsgId(), sim.getPhoneNumber(), campaignId);
            }

            messageRepo.saveAll(messages);
            log.info("💾 Saved {} messages (campaignId={}, country={})", messages.size(), campaignId, country);
        });

    }

    /**
     * Tính thời lượng campaign (phút)
     */
    private long calculateDuration(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return 0L;
        }
        Duration duration = Duration.between(startTime, endTime);
        return duration.toMinutes(); // Trả về số phút
    }
    /**
     * Lấy danh sách SIM active theo country, lọc SIM đã hết hạn order
     */
    private List<Sim> selectAvailableSims(String countryCode) {
        List<Sim> sims = simRepository
                .findByCountryCodeAndStatusIgnoreCaseOrderByRevenueDesc(countryCode, "ACTIVE");

        Date now = new Date();
        return sims.stream()
                .filter(sim -> sim.getPhoneNumber() != null && !sim.getPhoneNumber().isBlank())
                .filter(sim -> {
                    // Kiểm tra xem SIM còn ít nhất 1 order SMS chưa expire
                    boolean hasActiveOrder = orderRepository.findByPhoneAndServiceCodes(sim.getPhoneNumber(), List.of("SMS"))
                            .stream()
                            .filter(order -> order.getStock() != null)
                            .flatMap(order -> order.getStock().stream())
                            .anyMatch(stock -> stock.getExpiredAt() != null && stock.getExpiredAt().after(now));
                    return hasActiveOrder;
                })
                .collect(Collectors.toList());
    }

}