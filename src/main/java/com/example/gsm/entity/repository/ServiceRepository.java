package com.example.gsm.entity.repository;

import com.example.gsm.entity.ServiceEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ServiceRepository extends MongoRepository<ServiceEntity, String> {
    List<ServiceEntity> findByIsActiveTrue();
    List<ServiceEntity> findByCode(String code);
}

