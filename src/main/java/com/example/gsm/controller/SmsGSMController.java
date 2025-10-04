
package com.example.gsm.controller;

import com.example.gsm.entity.*;
import com.example.gsm.entity.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

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
    /** GSM client gửi kết quả gửi tin nhắn
     * WAIT → khi BE mới tạo message, chưa gửi.
     *
     * PENDING → GSM Client đã nhận job nhưng chưa gửi.
     *
     * SENT → GSM đã gửi thành công ra mạng (chưa chắc người nhận nhận được).
     *
     * DELIVERED → mạng viễn thông báo nhận (DLR).
     *
     * FAILED → gửi lỗi (SIM lỗi, nội dung lỗi, số không hợp lệ, modem mất kết nối...).
     * */


    /**
     * GSM phản hồi trạng thái tin nhắn (ACK)
     */

    @MessageMapping("/sms-response")
    public void handleResponse(Map<String, Object> response) {
        String localMsgId = (String) response.get("localMsgId");
        String status = (String) response.get("status"); // WAIT | PENDING | SENT | DELIVERED | FAILED
        String errorMsg = (String) response.getOrDefault("errorMsg", null);
        String simId = (String) response.get("simId");
        String campaignId = (String) response.get("campaignId");
        String sessionId = (String) response.get("sessionId");

        // Validate bắt buộc
//        if (localMsgId == null || localMsgId.isBlank() ||
//                status == null || status.isBlank() ||
//                simId == null || simId.isBlank() ||
//                campaignId == null || campaignId.isBlank() ||
//                sessionId == null || sessionId.isBlank()) {
//            throw new IllegalArgumentException("Missing required fields in GSM response");
//        }

        messageRepo.findByLocalMsgId(localMsgId).ifPresent(msg -> {
            msg.setStatus(status);
            msg.setUpdatedAt(LocalDateTime.now());
            msg.setCampaignId(campaignId);   // ép luôn campaignId
            msg.setChatSessionId(sessionId); // ép luôn sessionId

            if ("FAILED".equalsIgnoreCase(status)) {
                msg.setErrorMsg(errorMsg);
            }
            if ("DELIVERED".equalsIgnoreCase(status)) {
                msg.setDeliveredAt(LocalDateTime.now());
            }
            messageRepo.save(msg);

            // Nếu thành công thì cộng revenue cho SIM
            if ("SENT".equalsIgnoreCase(status) || "DELIVERED".equalsIgnoreCase(status)) {
                simRepository.findById(simId).ifPresent(sim -> {
                    double oldRevenue = sim.getRevenue() != null ? sim.getRevenue() : 0.0;

                    // Loại SMS
                    String smsType = "OUTBOUND".equalsIgnoreCase(msg.getDirection()) ? "ONE_WAY" : "TWO_WAY";

                    // Giá theo config
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

            // Push realtime FE kèm đủ thông tin
            messagingTemplate.convertAndSend("/topic/fe-updates", msg);
            messagingTemplate.convertAndSend("/topic/chat/" + msg.getPhoneNumber(), msg);
        });
    }


    /**
     * FE gửi SMS outbound (2 chiều chat)
     */
    @MessageMapping("/send-sms")
    public void sendSms(Map<String, Object> payload) {
        String phone = (String) payload.get("phoneNumber");
        String content = (String) payload.get("content");
        String campaignId = (String) payload.get("campaignId");
        String sessionId = (String) payload.get("sessionId");
        String simId = (String) payload.get("simId");

        SmsCampaign campaign = campaignRepo.findById(campaignId).orElse(null);

        // Tìm session theo phone
        SmsSession session = sessionRepo.findByCampaignIdAndPhoneNumber(campaignId, phone)
                .orElseGet(() -> {
                    SmsSession newSession = SmsSession.builder()
                            .campaignId(campaignId)
                            .phoneNumber(phone)
                            .startTime(LocalDateTime.now())
                            .lastActivityAt(LocalDateTime.now())
                            .endTime(campaign != null ? campaign.getEndTime() : null)
                            .active(true)
                            .build();
                    return sessionRepo.save(newSession);
                });

        // Update activity nếu session đã tồn tại
        session.setLastActivityAt(LocalDateTime.now());
        session.setActive(true);
        sessionRepo.save(session);

        SmsMessageWsk msg = SmsMessageWsk.builder()
                .phoneNumber(phone)
                .content(content)
                .direction("OUTBOUND")
                .status("WAIT")
                .campaignId(campaignId)
                .localMsgId(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now())
                .chatSessionId(session.getId())
                .build();

        messageRepo.save(msg);

        // Push job xuống GSM
        Map<String, Object> job = Map.of(
                "action", "SEND_GSM_SMS",
                "localMsgId", msg.getLocalMsgId(),
                "simId", simId,
                "phoneNumber", phone,
                "content", content,
                "campaignId", campaignId,
                "sessionId", sessionId
        );
        messagingTemplate.convertAndSend("/topic/sms-job-topic", job);
        messagingTemplate.convertAndSend("/topic/chat/" + phone, msg);
    }

    /**
     * GSM gửi inbound SMS
     */
    @MessageMapping("/sms-inbound")
    public void handleInbound(Map<String, Object> payload) {
        String phone = (String) payload.get("phoneNumber");
        String content = (String) payload.get("content");
        String campaignId = (String) payload.get("campaignId");
        String sessionId = (String) payload.get("sessionId");

        SmsCampaign campaign = campaignRepo.findById(campaignId).orElse(null);

        //tìm session theo sessionId
        SmsSession session = sessionRepo.findById(sessionId)
                .orElseGet(() -> {
                    SmsSession newSession = SmsSession.builder()
                            .campaignId(campaignId)
                            .phoneNumber(phone)
                            .startTime(LocalDateTime.now())
                            .lastActivityAt(LocalDateTime.now())
                            .endTime(campaign != null ? campaign.getEndTime() : null)
                            .active(true)
                            .build();
                    return sessionRepo.save(newSession);
                });

        // Update activity nếu session đã tồn tại
        session.setLastActivityAt(LocalDateTime.now());
        session.setActive(true);
        sessionRepo.save(session);

        SmsMessageWsk inbound = SmsMessageWsk.builder()
                .phoneNumber(phone)
                .content(content)
                .direction("INBOUND")
                .status("DELIVERED")
                .createdAt(LocalDateTime.now())
                .chatSessionId(session.getId())
                .campaignId(campaignId)
                .build();

        messageRepo.save(inbound);

        // Push realtime FE chat
        messagingTemplate.convertAndSend("/topic/chat/" + phone, inbound);
    }
}
