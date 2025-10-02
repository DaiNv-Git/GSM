package com.example.gsm.entity.repository;

import com.example.gsm.entity.SmsReply;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SmsReplyRepository extends MongoRepository<SmsReply, String> {}
