package com.example.gsm.entity.repository;

import com.example.gsm.entity.Country;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CountryRepository extends MongoRepository<Country, String> {
}
