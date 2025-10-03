package com.example.gsm.services;

import com.example.gsm.entity.PricingConfig;

import java.util.List;
import java.util.Optional;

public interface PricingConfigService {
    List<PricingConfig> getAll();
    Optional<PricingConfig> getById(String id);
    PricingConfig create(PricingConfig config);
    PricingConfig update(String id, PricingConfig updated) ;
    void delete(String id);
}
