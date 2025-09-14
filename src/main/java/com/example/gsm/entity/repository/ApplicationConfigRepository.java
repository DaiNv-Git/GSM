package com.example.gsm.entity.repository;

import com.example.gsm.entity.ApplicationConfig;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ApplicationConfigRepository extends MongoRepository<ApplicationConfig, String> {
    ApplicationConfig findByApplicationName(String applicationName);

    List<ApplicationConfig> findAllByOrderByCreatedAtDesc();

}
