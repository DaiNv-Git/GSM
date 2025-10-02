package com.example.gsm.entity.repository;

import com.example.gsm.entity.PricingConfig;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PricingConfigRepository extends MongoRepository<PricingConfig, String> {
    @Query("{ 'smsType': ?0, 'validFrom': { $lte: ?1 }, 'validTo': { $gte: ?1 } }")
    Optional<PricingConfig> findActivePricing(String smsType, LocalDateTime now);
    Optional<PricingConfig> findFirstBySmsTypeAndValidFromBeforeAndValidToAfter(String smsType, LocalDateTime now1, LocalDateTime now2 );
}