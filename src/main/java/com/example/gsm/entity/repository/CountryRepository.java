package com.example.gsm.entity.repository;

import com.example.gsm.entity.Country;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CountryRepository extends MongoRepository<Country, String> {
    List<Country> findAllByCountryCodeIn(List<String> countryCodes);

}
