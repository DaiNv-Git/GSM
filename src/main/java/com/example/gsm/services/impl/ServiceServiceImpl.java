package com.example.gsm.services.impl;

import com.example.gsm.entity.ServiceEntity;
import com.example.gsm.entity.repository.ServiceRepository;
import com.example.gsm.services.ServiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceServiceImpl implements ServiceService {
    private final ServiceRepository serviceRepository;


    public List<ServiceEntity> getAllServices() {
        return serviceRepository.findAll();
    }

    public List<ServiceEntity> searchByName(String name) {
        String regex = ".*" + name + ".*";
        return serviceRepository.findByTextRegexIgnoreCase(regex);
    }


}
