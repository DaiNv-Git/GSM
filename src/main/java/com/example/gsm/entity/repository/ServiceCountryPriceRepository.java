package com.example.gsm.entity.repository;

import com.example.gsm.entity.ServiceCountryPrice;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceCountryPriceRepository extends MongoRepository<ServiceCountryPrice, String> {
    Optional<ServiceCountryPrice> findByServiceCodeAndCountryCode(String serviceCode, String countryCode);
    List<ServiceCountryPrice> findByServiceCode(String serviceCode);
}
