package com.example.gsm.controller;

import com.example.gsm.entity.ServiceEntity;
import com.example.gsm.services.ServiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceService serviceService;

    @GetMapping
    public List<ServiceEntity> getAll() {
        return serviceService.getAllServices();
    }
    
    @GetMapping("/search")
    public List<ServiceEntity> search(@RequestParam String name) {
        return serviceService.searchByName(name);
    }
}
