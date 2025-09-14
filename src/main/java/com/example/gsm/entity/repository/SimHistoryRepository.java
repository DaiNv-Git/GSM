package com.example.gsm.entity.repository;

import com.example.gsm.entity.SimHistory;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SimHistoryRepository extends MongoRepository<SimHistory, String> {
    boolean existsByPhoneNumberAndStatus(String phoneNumber, String status);

    Optional<SimHistory> findByPhoneNumberAndRequestTime(String phoneNumber, Long requestTime);
}
