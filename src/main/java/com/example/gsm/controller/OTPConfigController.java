package com.example.gsm.controller;

import com.example.gsm.entity.ApplicationConfig;
import com.example.gsm.services.ApplicationConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/otp-configs")
@RequiredArgsConstructor
public class OTPConfigController {
    private final ApplicationConfigService service;

    @GetMapping
    public ResponseEntity<List<ApplicationConfig>> getAll() {
        List<ApplicationConfig> configs = service.getAll();
        return ResponseEntity.ok(configs);
    }

    @GetMapping("/{name}")
    public ResponseEntity<ApplicationConfig> getByName(@PathVariable String name) {
        ApplicationConfig config = service.getByName(name);
        return ResponseEntity.ok(config);
    }

    @PostMapping
    public ResponseEntity<ApplicationConfig> create(@RequestBody ApplicationConfig config) {
        ApplicationConfig created = service.create(config);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApplicationConfig> update(@PathVariable String id,
                                                    @RequestBody ApplicationConfig config) {
        ApplicationConfig updated = service.update(id, config);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
