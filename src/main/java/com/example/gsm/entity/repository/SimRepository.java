package com.example.gsm.entity.repository;

import com.example.gsm.entity.Sim;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SimRepository extends MongoRepository<Sim, String> {
    List<Sim> findByCountryCode(String CountryCode);
}
