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

    @Query("{ 'phoneNumber': ?0, 'isActive': true }")
    Optional<SmsSession> findActiveByPhone(String phoneNumber);

    Optional<SmsSession> findByPhoneNumberAndActiveTrue(String phoneNumber);
    List<SmsSession> findByStatusAndExpiredAtBefore(String status, LocalDateTime time);
    List<SmsSession> findByActiveTrueAndEndTimeBefore(LocalDateTime now);
    SmsSession findActiveBySimId(String simId);

}
