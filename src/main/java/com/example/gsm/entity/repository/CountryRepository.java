package com.example.gsm.entity.repository;

import com.example.gsm.entity.Country;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface CountryRepository extends MongoRepository<Country, String> {
    List<Country> findAllByCountryCodeIn(List<String> countryCodes);
    List<Country> findAllByCountryCodeInAndCountryNameContainingIgnoreCase(List<String> countryCodes, String countryName);
    Optional<Country> findByCountryCode(String countryCode);


}
