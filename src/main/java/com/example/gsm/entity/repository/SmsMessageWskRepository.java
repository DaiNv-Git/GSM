package com.example.gsm.entity.repository;

import com.example.gsm.entity.SmsMessageWsk;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.awt.print.Pageable;
import java.util.List;
import java.util.Optional;

@Repository
public interface SmsMessageWskRepository extends MongoRepository<SmsMessageWsk, String> {
    List<SmsMessageWsk> findByCampaignIdAndStatus(String campaignId, String status, Pageable pageable);
    Optional<SmsMessageWsk> findByLocalMsgId(String localMsgId);
    List<SmsMessageWsk> findByPhoneNumberOrderByCreatedAtAsc(String phoneNumber);

    // Nếu muốn group theo session chat
    List<SmsMessageWsk> findByChatSessionIdOrderByCreatedAtAsc(String chatSessionId);
    List<SmsMessageWsk> findByCampaignIdOrderByCreatedAtAsc(String campaignID);
}
