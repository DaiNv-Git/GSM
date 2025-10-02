package com.example.gsm.services.impl;

import com.example.gsm.entity.Sim;
import com.example.gsm.entity.SmsMessageWsk;
import com.example.gsm.entity.repository.OrderRepository;
import com.example.gsm.entity.repository.SimRepository;
import com.example.gsm.entity.repository.SmsCampaignRepository;
import com.example.gsm.entity.repository.SmsMessageWskRepository;
import com.example.gsm.services.ClaimService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SmsSenderServiceImpl {

    private final ClaimService claimService;
    private final SmsMessageWskRepository messageRepo;
    private final SmsCampaignRepository campaignRepo;
    private final SimpMessagingTemplate messagingTemplate;
    private final SimRepository simRepository;
    private final OrderRepository orderRepository;
    private final int batchSize = 100;
    private final String workerId = UUID.randomUUID().toString();

    public void processBatch() {
        List<SmsMessageWsk> batch = claimService.claimBatch(batchSize, workerId);
        if (batch.isEmpty()) return;

        // Group batch theo country
        Map<String, List<SmsMessageWsk>> groupedByCountry =
                batch.stream().collect(Collectors.groupingBy(SmsMessageWsk::getCountry));

        groupedByCountry.forEach((country, messages) -> {
            if (messages.isEmpty()) return;

            String jobId = UUID.randomUUID().toString();
            String campaignId = messages.get(0).getCampaignId();
            String content = messages.get(0).getContent();

            List<String> phones = messages.stream()
                    .map(SmsMessageWsk::getPhoneNumber)
                    .collect(Collectors.toList());

            // Chỉ chọn 1 SIM có doanh thu cao nhất
            Optional<Sim> topSim = selectTopSim(country);

            if (topSim.isEmpty()) {
                // Không có SIM nào khả dụng → mark failed hoặc retry
                messages.forEach(msg -> {
                    msg.setStatus("FAILED_NO_SIM");
                });
                messageRepo.saveAll(messages);
                return;
            }

            Map<String, Object> job = Map.of(
                    "action", "SEND_BATCH_SMS",
                    "jobId", jobId,
                    "campaignId", campaignId,
                    "country", country,
                    "simId", topSim.get().getId(),
                    "phones", phones,
                    "content", content
            );

            // Push job lên topic
            messagingTemplate.convertAndSend("/topic/sms-job-topic", job);

            // Update DB
            messages.forEach(msg -> {
                msg.setStatus("PENDING");
                msg.setSentAt(LocalDateTime.now());
            });
            messageRepo.saveAll(messages);
        });
    }

    private Optional<Sim> selectTopSim(String countryCode) {
        return simRepository
                .findByCountryCodeAndStatusIgnoreCaseOrderByRevenueDesc(countryCode, "ACTIVE")
                .stream()
                .filter(sim -> sim.getPhoneNumber() != null && !sim.getPhoneNumber().isBlank())
                .filter(sim -> orderRepository.countByPhoneAndServiceCodes(sim.getPhoneNumber(), List.of("SMS")) <= 0)
                .findFirst(); // chỉ lấy 1 SIM doanh thu cao nhất
    }
}