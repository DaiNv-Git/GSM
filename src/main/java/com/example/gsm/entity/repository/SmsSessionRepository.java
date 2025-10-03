package com.example.gsm.entity.repository;

import com.example.gsm.entity.SmsSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
@Repository
public interface SmsSessionRepository extends MongoRepository<SmsSession, String> {
    Optional<SmsSession> findByCampaignIdAndPhoneNumberAndIsActiveTrue(String campaignId, String phoneNumber);

    @Query("{ 'phoneNumber': ?0, 'isActive': true }")
    Optional<SmsSession> findActiveByPhone(String phoneNumber);

    Optional<SmsSession> findByPhoneNumberAndIsActiveTrue(String phoneNumber);
    List<SmsSession> findByIsActiveTrue();

    List<SmsSession> findByStatusAndExpiredAtBefore(String status, LocalDateTime time);
    List<SmsSession> findByIsActiveTrueAndEndTimeBefore(LocalDateTime now);
    List<SmsSession> findByIsActiveTrueAndLastActivityAtBefore(LocalDateTime threshold);
    SmsSession findActiveBySimId(String simId);

}
