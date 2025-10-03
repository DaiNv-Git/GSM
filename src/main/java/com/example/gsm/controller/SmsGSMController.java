
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

    @MessageMapping("/sms-response")
    public void handleResponse(Map<String, Object> response) {
        String localMsgId = (String) response.get("localMsgId");
        String status = (String) response.get("status");// WAIT | PENDING | SENT | DELIVERED | FAILED
        String errorMsg = (String) response.getOrDefault("errorMsg", null);
        String simId = (String) response.get("simId");

        messageRepo.findByLocalMsgId(localMsgId).ifPresent(msg -> {
            msg.setStatus(status);
            msg.setUpdatedAt(LocalDateTime.now());

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

                    // Xác định loại SMS từ message (ONE_WAY / TWO_WAY)
                    String smsType = "OUTBOUND".equalsIgnoreCase(msg.getDirection()) ? "ONE_WAY" : "TWO_WAY";

                    // Lấy config giá theo loại
                    PricingConfig pricing = pricingRepo
                            .findActivePricing(smsType, LocalDateTime.now())
                            .orElse(PricingConfig.builder()
                                    .pricePerSms(1.0)
                                    .pricePerMinute(1.0)
                                    .build());

                    if ("ONE_WAY".equalsIgnoreCase(smsType)) {
                        // Logic: 1 chiều tính theo số tin outbound
                        sim.setRevenue(oldRevenue + pricing.getPricePerSms());
                    } else if ("TWO_WAY".equalsIgnoreCase(smsType) && msg.getChatSessionId() != null) {
                        // Logic: 2 chiều tính theo thời gian session
                        sessionRepo.findById(msg.getChatSessionId()).ifPresent(session -> {
                            long minutes = Duration.between(session.getStartTime(), LocalDateTime.now()).toMinutes();
                            sim.setRevenue(oldRevenue + minutes * pricing.getPricePerMinute());
                        });
                    }

                    sim.setLastUpdated(new Date());
                    simRepository.save(sim);
                });
            }

            // Push realtime cho UI dashboard
            messagingTemplate.convertAndSend("/topic/fe-updates", response);

            // Push realtime cho UI chat (theo số điện thoại)
            messagingTemplate.convertAndSend("/topic/chat/" + msg.getPhoneNumber(), msg);
        });
    }

    // Khi gửi SMS outbound : FE GỬI XUỐNG
    @MessageMapping("/send-sms")
    public void sendSms(Map<String, Object> payload) {
        String phone = (String) payload.get("phoneNumber");
        String content = (String) payload.get("content");
        String campaignId = (String) payload.get("campaignId");
        String simId = (String) payload.get("simId");

        // Tìm session active theo phone
        SmsSession session = sessionRepo.findByPhoneNumberAndIsActiveTrue(phone)
                .orElseGet(() -> {
                    SmsSession newSession = SmsSession.builder()
                            .campaignId(campaignId)
                            .phoneNumber(phone)
                            .startTime(LocalDateTime.now())
                            .lastActivityAt(LocalDateTime.now())
                            .isActive(true)
                            .build();
                    return sessionRepo.save(newSession);
                });

        session.setLastActivityAt(LocalDateTime.now());
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

        Map<String,Object> job = Map.of(
                "action", "SEND_SINGLE_SMS",
                "localMsgId", msg.getLocalMsgId(),
                "simId", simId,
                "phone", phone,
                "content", content
        );
        messagingTemplate.convertAndSend("/topic/sms-job-topic", job);
        messagingTemplate.convertAndSend("/topic/chat/" + phone, msg);
    }

    // Khi nhận SMS inbound : GSM CLIENT GỬI
    @MessageMapping("/sms-inbound")
    public void handleInbound(Map<String,Object> payload) {
        String phone = (String) payload.get("phoneNumber");
        String content = (String) payload.get("content");

        SmsSession session = sessionRepo.findByPhoneNumberAndIsActiveTrue(phone)
                .orElseGet(() -> {
                    SmsSession newSession = SmsSession.builder()
                            .phoneNumber(phone)
                            .startTime(LocalDateTime.now())
                            .lastActivityAt(LocalDateTime.now())
                            .isActive(true)
                            .build();
                    return sessionRepo.save(newSession);
                });

        session.setLastActivityAt(LocalDateTime.now());
        sessionRepo.save(session);

        SmsMessageWsk inbound = SmsMessageWsk.builder()
                .phoneNumber(phone)
                .content(content)
                .direction("INBOUND")
                .status("DELIVERED")
                .createdAt(LocalDateTime.now())
                .chatSessionId(session.getId())
                .build();

        messageRepo.save(inbound);
        messagingTemplate.convertAndSend("/topic/chat/" + phone, inbound);
    }
}
