package com.example.gsm.controller;

import com.example.gsm.entity.PricingConfig;
import com.example.gsm.services.PricingConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pricing")
@RequiredArgsConstructor
public class PricingConfigController {

    private final PricingConfigService service;

    @GetMapping("/get-all")
    public ResponseEntity<List<PricingConfig>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/find/{id}")
    public ResponseEntity<PricingConfig> getById(@PathVariable String id) {
        return service.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/create")
    public ResponseEntity<PricingConfig> create(@RequestBody PricingConfig config) {
        return ResponseEntity.ok(service.create(config));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<PricingConfig> update(@PathVariable String id,
                                                @RequestBody PricingConfig updated) {
        return ResponseEntity.ok(service.update(id, updated));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
