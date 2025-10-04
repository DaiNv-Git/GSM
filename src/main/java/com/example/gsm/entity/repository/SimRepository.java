package com.example.gsm.entity.repository;

import com.example.gsm.entity.Sim;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface SimRepository extends MongoRepository<Sim, String> {
    List<Sim> findByCountryCodeAndStatusIgnoreCaseOrderByRevenueDesc(String countryCode, String status);
    Optional<Sim> findByPhoneNumber(String phoneNumber);
    List<Sim> findAllByCountryCode(String country);

}
