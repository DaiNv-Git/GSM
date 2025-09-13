package com.example.gsm.services;

import com.example.gsm.entity.ServiceEntity;
import org.springframework.stereotype.Service;

import java.util.List;
public interface ServiceService {
    List<ServiceEntity> getAllServices();
    List<ServiceEntity> searchByName(String name);
}
