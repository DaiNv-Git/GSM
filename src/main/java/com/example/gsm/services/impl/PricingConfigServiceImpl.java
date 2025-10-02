package com.example.gsm.services.impl;

import com.example.gsm.entity.PricingConfig;
import com.example.gsm.entity.repository.PricingConfigRepository;
import com.example.gsm.services.PricingConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PricingConfigServiceImpl implements PricingConfigService {
    private final PricingConfigRepository repository;

    @Override
    public List<PricingConfig> getAll() {
        return repository.findAll();
    }
    @Override
    public Optional<PricingConfig> getById(String id) {
        return repository.findById(id);
    }
    @Override
    public PricingConfig create(PricingConfig config) {
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        return repository.save(config);
    }
    @Override
    public PricingConfig update(String id, PricingConfig updated) {
        return repository.findById(id).map(existing -> {
            existing.setSmsType(updated.getSmsType());
            existing.setPricePerSms(updated.getPricePerSms());
            existing.setPricePerMinute(updated.getPricePerMinute());
            existing.setValidFrom(updated.getValidFrom());
            existing.setValidTo(updated.getValidTo());
            existing.setUpdatedAt(LocalDateTime.now());
            return repository.save(existing);
        }).orElseThrow(() -> new RuntimeException("PricingConfig not found with id " + id));
    }
    @Override
    public void delete(String id) {
        repository.deleteById(id);
    }
}
