package com.example.gsm.entity.repository;

import com.example.gsm.entity.ServiceEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceRepository extends MongoRepository<ServiceEntity, String> {
    List<ServiceEntity> findByTextRegexIgnoreCase(String keyword);
    Optional<ServiceEntity> findByCode(String code);
    List<ServiceEntity> findAllByCodeIn(List<String> codes);
    List<ServiceEntity> findAllByCountryCode(String countryCode);


}