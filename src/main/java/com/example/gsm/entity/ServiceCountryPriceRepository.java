package com.example.gsm.entity;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ServiceCountryPriceRepository extends MongoRepository<ServiceCountryPrice, String> {
    Optional<ServiceCountryPrice> findByServiceCodeAndCountryCode(String serviceCode, String countryCode);
}
