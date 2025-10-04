package com.example.gsm.controller;

import com.example.gsm.entity.*;
import com.example.gsm.entity.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Controller
@RequiredArgsConstructor
public class SmsGSMController {

    private final SmsMessageWskRepository messageRepo;
    private final SmsSessionRepository sessionRepo;
    private final SmsReplyRepository replyRepo;
    private final SimRepository simRepository;
    private final PricingConfigRepository pricingRepo;
    private final SimpMessagingTemplate messagingTemplate;
    private final SmsCampaignRepository campaignRepo;
    private final ExecutorService retryExecutor = Executors.newFixedThreadPool(10);

    /**
     * GSM client gửi kết quả gửi tin nhắn
     * WAIT → khi BE mới tạo message, chưa gửi.
     * PENDING → GSM Client đã nhận job nhưng chưa gửi.
     * SENT → GSM đã gửi thành công ra mạng (chưa chắc người nhận nhận được).
     * DELIVERED → mạng viễn thông báo nhận (DLR).
     * FAILED → gửi lỗi (SIM lỗi, nội dung lỗi, số không hợp lệ, modem mất kết nối...).
     * DLQ → thất bại sau 3 lần retry.
     */

    /**
     * GSM phản hồi trạng thái tin nhắn (ACK)
     */
    @MessageMapping("/sms-response")
    public void handleResponse(@Payload Map<String, Object> response) {
        String localMsgId = (String) response.get("localMsgId");
        String status = (String) response.get("status");
        String errorMsg = (String) response.getOrDefault("errorMsg", null);
        String simId = (String) response.get("simId");
        String campaignId = (String) response.get("campaignId");
        String sessionId = (String) response.get("sessionId");

        // Validate bắt buộc
        if (localMsgId == null || localMsgId.isBlank() ||
                status == null || status.isBlank() ||
                simId == null || simId.isBlank() ||
                campaignId == null || campaignId.isBlank() ||
                sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("Missing required fields in GSM response");
        }

        messageRepo.findByLocalMsgId(localMsgId).ifPresent(msg -> {
            // Tìm campaign để lấy smsType
            SmsCampaign campaign = campaignRepo.findById(campaignId)
                    .orElseThrow(() -> new IllegalArgumentException("Campaign not found: " + campaignId));

            // Cập nhật message
            msg.setStatus(status);
            msg.setUpdatedAt(LocalDateTime.now());
            msg.setCampaignId(campaignId);
            msg.setChatSessionId(sessionId);
            if ("FAILED".equalsIgnoreCase(status)) {
                msg.setErrorMsg(errorMsg);
            }
            if ("DELIVERED".equalsIgnoreCase(status)) {
                msg.setDeliveredAt(LocalDateTime.now());
            }
            messageRepo.save(msg);

            // Cộng revenue cho SIM (khi SENT hoặc DELIVERED)
            if ("SENT".equalsIgnoreCase(status) || "DELIVERED".equalsIgnoreCase(status)) {
                simRepository.findById(simId).ifPresent(sim -> {
                    double oldRevenue = sim.getRevenue() != null ? sim.getRevenue() : 0.0;
                    String smsType = campaign.getType(); // ONE_WAY hoặc TWO_WAY
                    PricingConfig pricing = pricingRepo
                            .findActivePricing(smsType, LocalDateTime.now())
                            .orElse(PricingConfig.builder()
                                    .pricePerSms(1.0)
                                    .pricePerMinute(1.0)
                                    .build());

                    if ("ONE_WAY".equalsIgnoreCase(smsType)) {
                        sim.setRevenue(oldRevenue + pricing.getPricePerSms());
                    } else if ("TWO_WAY".equalsIgnoreCase(smsType) && msg.getChatSessionId() != null) {
                        sessionRepo.findById(msg.getChatSessionId()).ifPresent(session -> {
                            long minutes = Duration.between(session.getStartTime(), LocalDateTime.now()).toMinutes();
                            sim.setRevenue(oldRevenue + minutes * pricing.getPricePerMinute());
                        });
                    }
                    sim.setLastUpdated(new Date());
                    simRepository.save(sim);
                });
            }

            // Retry nếu FAILED
            if ("FAILED".equalsIgnoreCase(status) && msg.getRetryCount() < 3) {
                retryExecutor.submit(() -> {
                    try {
                        Thread.sleep(5000); // Retry sau 5s
                        msg.setRetryCount(msg.getRetryCount() + 1);
                        msg.setStatus("PENDING");
                        msg.setSentAt(LocalDateTime.now());
                        msg.setUpdatedAt(LocalDateTime.now());
                        messageRepo.save(msg);
                        Map<String, Object> retryJob = createRetryJob(msg, campaign);
                        messagingTemplate.convertAndSend("/topic/sms-job-topic", retryJob);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            } else if ("FAILED".equalsIgnoreCase(status)) {
                // Set status DLQ
                msg.setStatus("DLQ");
                msg.setUpdatedAt(LocalDateTime.now());
                messageRepo.save(msg);
                Map<String, Object> dlqPayload = new HashMap<>();
                dlqPayload.put("localMsgId", msg.getLocalMsgId());
                dlqPayload.put("status", "DLQ");
                dlqPayload.put("campaignId", campaignId);
                dlqPayload.put("smsType", campaign.getType());
                dlqPayload.put("errorMsg", msg.getErrorMsg());
                messagingTemplate.convertAndSend("/topic/fe-updates", dlqPayload);
            }

            // Push update cho FE
            Map<String, Object> updatePayload = new HashMap<>();
            updatePayload.put("localMsgId", msg.getLocalMsgId());
            updatePayload.put("status", msg.getStatus());
            updatePayload.put("campaignId", msg.getCampaignId());
            updatePayload.put("smsType", campaign.getType());
            updatePayload.put("errorMsg", msg.getErrorMsg());
            messagingTemplate.convertAndSend("/topic/fe-updates", updatePayload);
            messagingTemplate.convertAndSend("/topic/chat/" + msg.getPhoneNumber(), msg);
        });
    }

    /**
     * FE gửi SMS outbound (2 chiều chat)
     */
    @MessageMapping("/send-sms")
    public void sendSms(@Payload Map<String, Object> payload) {
        String phoneNumber = (String) payload.get("phoneNumber");
        String content = (String) payload.get("content");
        String campaignId = (String) payload.get("campaignId");
        String sessionId = (String) payload.get("sessionId");
        String simId = (String) payload.get("simId");

        // Validate input
        if (phoneNumber == null || phoneNumber.isBlank() ||
                content == null || content.isBlank() ||
                campaignId == null || campaignId.isBlank() ||
                simId == null || simId.isBlank()) {
            throw new IllegalArgumentException("Missing required fields in payload");
        }

        // Tìm campaign
        SmsCampaign campaign = campaignRepo.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found: " + campaignId));

        // Kiểm tra campaign status
        if ("EXPIRED".equals(campaign.getStatus()) || "COMPLETED".equals(campaign.getStatus())) {
            Map<String, Object> errorPayload = Map.of(
                    "phoneNumber", phoneNumber,
                    "status", "FAILED",
                    "errorMsg", "Campaign expired or completed",
                    "campaignId", campaignId,
                    "smsType", campaign.getType()
            );
            messagingTemplate.convertAndSend("/topic/fe-updates", errorPayload);
            return;
        }

        // Lấy SIM từ simId (không kiểm tra ACTIVE/order vì processBatch đã phân chia)
        Sim sim = simRepository.findById(simId)
                .orElseThrow(() -> {
                    SmsMessageWsk msg = SmsMessageWsk.builder()
                            .phoneNumber(phoneNumber)
                            .content(content)
                            .direction("OUTBOUND")
                            .status("FAILED")
                            .campaignId(campaignId)
                            .localMsgId(UUID.randomUUID().toString())
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .errorMsg("SIM not found for simId: " + simId)
                            .build();
                    messageRepo.save(msg);
                    Map<String, Object> errorPayload = new HashMap<>();
                    errorPayload.put("localMsgId", msg.getLocalMsgId());
                    errorPayload.put("status", "FAILED");
                    errorPayload.put("campaignId", campaignId);
                    errorPayload.put("smsType", campaign.getType());
                    errorPayload.put("errorMsg", msg.getErrorMsg());
                    messagingTemplate.convertAndSend("/topic/fe-updates", errorPayload);
                    messagingTemplate.convertAndSend("/topic/chat/" + phoneNumber, msg);
                    throw new IllegalArgumentException("SIM not found: " + simId);
                });

        // Tìm hoặc tạo session
        SmsSession session = sessionRepo.findByCampaignIdAndPhoneNumber(campaignId, phoneNumber)
                .orElseGet(() -> {
                    SmsSession newSession = SmsSession.builder()
                            .campaignId(campaignId)
                            .phoneNumber(phoneNumber)
                            .simId(sim.getId())
                            .deviceName(sim.getDeviceName())
                            .comPort(sim.getComName())
                            .startTime(LocalDateTime.now())
                            .lastActivityAt(LocalDateTime.now())
                            .endTime(campaign.getEndTime())
                            .active(true)
                            .build();
                    return sessionRepo.save(newSession);
                });

        // Update last activity
        session.setLastActivityAt(LocalDateTime.now());
        session.setActive(true);
        sessionRepo.save(session);

        // Tạo message
        SmsMessageWsk msg = SmsMessageWsk.builder()
                .phoneNumber(phoneNumber)
                .content(content)
                .direction("OUTBOUND")
                .status("WAIT")
                .campaignId(campaignId)
                .country(campaign.getCountry())
                .localMsgId(UUID.randomUUID().toString())
                .chatSessionId(session.getId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        messageRepo.save(msg);

        // Chuyển status sang PENDING
        msg.setStatus("PENDING");
        msg.setSentAt(LocalDateTime.now());
        messageRepo.save(msg);

        // Tạo job JSON đầy đủ
        String jobId = UUID.randomUUID().toString();
        Map<String, Object> job = new HashMap<>();
        job.put("action", "SEND_GSM_SMS");
        job.put("jobId", jobId);
        job.put("campaignId", campaignId);
        job.put("country", campaign.getCountry());
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

        // Push job qua topic
        messagingTemplate.convertAndSend("/topic/sms-job-topic", job);

        // Push update cho FE
        Map<String, Object> updatePayload = new HashMap<>();
        updatePayload.put("localMsgId", msg.getLocalMsgId());
        updatePayload.put("status", msg.getStatus());
        updatePayload.put("campaignId", campaignId);
        updatePayload.put("smsType", campaign.getType());
        messagingTemplate.convertAndSend("/topic/fe-updates", updatePayload);
        messagingTemplate.convertAndSend("/topic/chat/" + phoneNumber, msg);
    }

    /**
     * GSM gửi inbound SMS
     */
    @MessageMapping("/sms-inbound")
    public void handleInbound(@Payload Map<String, Object> payload) {
        String phoneNumber = (String) payload.get("phoneNumber");
        String content = (String) payload.get("content");
        String campaignId = (String) payload.get("campaignId");
        String sessionId = (String) payload.get("sessionId");
        String simId = (String) payload.get("simId");

        // Validate input
        if (phoneNumber == null || phoneNumber.isBlank() ||
                content == null || content.isBlank() ||
                campaignId == null || campaignId.isBlank() ||
                sessionId == null || sessionId.isBlank() ||
                simId == null || simId.isBlank()) {
            throw new IllegalArgumentException("Missing required fields in inbound payload");
        }

        // Tìm campaign
        SmsCampaign campaign = campaignRepo.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found: " + campaignId));

        // Kiểm tra campaign status
        if ("EXPIRED".equals(campaign.getStatus()) || "COMPLETED".equals(campaign.getStatus())) {
            return; // Không xử lý inbound nếu campaign hết hạn
        }

        // Tìm session
        SmsSession session = sessionRepo.findById(sessionId)
                .filter(s -> s.isActive() && simId.equals(s.getSimId()))
                .orElseGet(() -> {
                    SmsSession newSession = SmsSession.builder()
                            .campaignId(campaignId)
                            .phoneNumber(phoneNumber)
                            .simId(simId)
                            .startTime(LocalDateTime.now())
                            .lastActivityAt(LocalDateTime.now())
                            .endTime(campaign.getEndTime())
                            .active(true)
                            .build();
                    return sessionRepo.save(newSession);
                });

        // Update last activity
        session.setLastActivityAt(LocalDateTime.now());
        session.setActive(true);
        sessionRepo.save(session);

        // Tạo inbound message
        SmsMessageWsk inbound = SmsMessageWsk.builder()
                .phoneNumber(phoneNumber)
                .content(content)
                .direction("INBOUND")
                .status("DELIVERED")
                .campaignId(campaignId)
                .country(campaign.getCountry())
                .localMsgId(UUID.randomUUID().toString())
                .chatSessionId(session.getId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .deliveredAt(LocalDateTime.now())
                .build();
        messageRepo.save(inbound);

        // Cộng revenue cho SIM nếu TWO_WAY
        if ("TWO_WAY".equalsIgnoreCase(campaign.getType())) {
            simRepository.findById(simId).ifPresent(sim -> {
                double oldRevenue = sim.getRevenue() != null ? sim.getRevenue() : 0.0;
                PricingConfig pricing = pricingRepo
                        .findActivePricing("TWO_WAY", LocalDateTime.now())
                        .orElse(PricingConfig.builder()
                                .pricePerMinute(1.0)
                                .build());
                long minutes = Duration.between(session.getStartTime(), LocalDateTime.now()).toMinutes();
                sim.setRevenue(oldRevenue + minutes * pricing.getPricePerMinute());
                sim.setLastUpdated(new Date());
                simRepository.save(sim);
            });
        }

        // Push update cho FE
        Map<String, Object> updatePayload = new HashMap<>();
        updatePayload.put("localMsgId", inbound.getLocalMsgId());
        updatePayload.put("status", inbound.getStatus());
        updatePayload.put("campaignId", campaignId);
        updatePayload.put("smsType", campaign.getType());
        messagingTemplate.convertAndSend("/topic/fe-updates", updatePayload);
        messagingTemplate.convertAndSend("/topic/chat/" + phoneNumber, inbound);
    }

    /**
     * Tính thời lượng campaign (phút)
     */
    private long calculateDuration(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return 0L;
        }
        return Duration.between(startTime, endTime).toMinutes();
    }

    /**
     * Tạo retry job
     */
    private Map<String, Object> createRetryJob(SmsMessageWsk msg, SmsCampaign campaign) {
        // Lấy simId từ SmsSession thay vì SmsMessageWsk
        SmsSession session = sessionRepo.findById(msg.getChatSessionId())
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + msg.getChatSessionId()));
        Sim sim = simRepository.findById(session.getSimId())
                .orElseThrow(() -> new IllegalArgumentException("SIM not found: " + session.getSimId()));
        Map<String, Object> job = new HashMap<>();
        job.put("action", "SEND_GSM_SMS");
        job.put("jobId", UUID.randomUUID().toString());
        job.put("campaignId", msg.getCampaignId());
        job.put("country", msg.getCountry());
        job.put("phoneNumber", msg.getPhoneNumber());
        job.put("content", msg.getContent());
        job.put("simId", sim.getId());
        job.put("simPhoneNumber", sim.getPhoneNumber());
        job.put("deviceName", sim.getDeviceName());
        job.put("comName", sim.getComName());
        job.put("localMsgId", msg.getLocalMsgId());
        job.put("sessionId", msg.getChatSessionId());
        job.put("campaignStartTime", campaign.getStartTime() != null ? campaign.getStartTime().toString() : null);
        job.put("campaignEndTime", campaign.getEndTime() != null ? campaign.getEndTime().toString() : null);
        job.put("smsType", campaign.getType() != null ? campaign.getType() : "ONE_WAY");
        job.put("timeDuration", calculateDuration(campaign.getStartTime(), campaign.getEndTime()));
        return job;
    }
}