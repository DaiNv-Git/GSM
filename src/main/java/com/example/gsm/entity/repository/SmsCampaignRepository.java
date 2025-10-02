package com.example.gsm.entity.repository;

import com.example.gsm.entity.SmsCampaign;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SmsCampaignRepository extends MongoRepository<SmsCampaign, String> {

}
