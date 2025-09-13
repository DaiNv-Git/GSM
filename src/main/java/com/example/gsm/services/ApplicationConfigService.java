package com.example.gsm.services;
import com.example.gsm.entity.ApplicationConfig;
import java.util.List;

public interface ApplicationConfigService {
    List<ApplicationConfig> getAll();
    ApplicationConfig getByName(String appName);
    ApplicationConfig create(ApplicationConfig config);
    ApplicationConfig update(String id, ApplicationConfig updated);
    void delete(String id);
}
