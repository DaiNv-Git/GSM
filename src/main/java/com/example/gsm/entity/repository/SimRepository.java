package com.example.gsm.entity.repository;

import com.example.gsm.entity.Sim;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SimRepository extends MongoRepository<Sim, String> {

}
