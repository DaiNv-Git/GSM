package com.example.gsm.entity.repository;

import com.example.gsm.entity.SimHistory;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

public interface SimHistoryRepository extends MongoRepository<SimHistory, String> {
    boolean existsByPhoneNumberAndStatus(String phoneNumber, String status);
    boolean existsByPhoneNumberAndExpiredAtAfter(String phoneNumber,  Date expiredAt);



    Optional<SimHistory> findByPhoneNumberAndRequestTime(String phoneNumber, Long requestTime);
}
