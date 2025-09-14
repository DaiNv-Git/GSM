package com.example.gsm.entity.repository;

import com.example.gsm.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface UserRepository extends MongoRepository<User, String> {
    List<User> findByIsAgentTrueAndIsActiveTrue();
}
