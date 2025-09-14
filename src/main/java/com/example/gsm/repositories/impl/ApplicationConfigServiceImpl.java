package com.example.gsm.repositories.impl;

import com.example.gsm.entity.ApplicationConfig;

import com.example.gsm.entity.repository.ApplicationConfigRepository;
import com.example.gsm.exceptions.BadRequestException;
import com.example.gsm.exceptions.ErrorCode;
import com.example.gsm.repositories.ApplicationConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationConfigServiceImpl implements ApplicationConfigService {

    private final ApplicationConfigRepository repository;

    @Override
    public List<ApplicationConfig> getAll() {
        try {
            log.info("Fetching all ApplicationConfig records ordered by createdAt...");
            List<ApplicationConfig> configs = repository.findAllByOrderByCreatedAtDesc();
            log.info("Fetched {} records", configs.size());
            return configs;
        } catch (DataAccessException ex) {
            log.error("Error fetching all configs", ex);
            throw new RuntimeException("Unable to fetch configs", ex);
        }
    }

    @Override
    public ApplicationConfig getByName(String appName) {
        try {
            log.info("Fetching ApplicationConfig by name: {}", appName);
            ApplicationConfig config = repository.findByApplicationName(appName);
            if (config == null) {
                log.warn("ApplicationConfig not found for name: {}", appName);
                throw new BadRequestException(ErrorCode.BAD_EXCEPTION, "ApplicationConfig not found: " + appName);
            }
            log.info("Found config: {}", config);
            return config;
        } catch (DataAccessException ex) {
            log.error("Error fetching config by name {}", appName, ex);
            throw new RuntimeException("Unable to fetch config: " + appName, ex);
        }
    }

    @Override
    public ApplicationConfig create(ApplicationConfig config) {
        try {
            log.info("Creating ApplicationConfig: {}", config);
            ApplicationConfig saved = repository.save(config);
            log.info("Created ApplicationConfig with id {}", saved.getId());
            return saved;
        } catch (DataAccessException ex) {
            log.error("Error creating config {}", config, ex);
            throw new RuntimeException("Unable to create config", ex);
        }
    }

    @Override
    public ApplicationConfig update(String id, ApplicationConfig updated) {
        try {
            log.info("Updating ApplicationConfig with id {}", id);
            if (!repository.existsById(id)) {
                log.warn("Cannot update: config id {} not found", id);
                throw new BadRequestException(ErrorCode.BAD_EXCEPTION, "ApplicationConfig not found with id: " + id);
            }
            updated.setId(id);
            ApplicationConfig saved = repository.save(updated);
            log.info("Updated ApplicationConfig id {}", id);
            return saved;
        } catch (DataAccessException ex) {
            log.error("Error updating config id {}", id, ex);
            throw new RuntimeException("Unable to update config with id " + id, ex);
        }
    }

    @Override
    public void delete(String id) {
        try {
            log.info("Deleting ApplicationConfig with id {}", id);
            if (!repository.existsById(id)) {
                log.warn("Cannot delete: config id {} not found", id);
                throw new BadRequestException(ErrorCode.BAD_EXCEPTION, "ApplicationConfig not found with id: " + id);

            }
            repository.deleteById(id);
            log.info("Deleted ApplicationConfig id {}", id);
        } catch (DataAccessException ex) {
            log.error("Error deleting config id {}", id, ex);
            throw new RuntimeException("Unable to delete config with id " + id, ex);
        }
    }
}
