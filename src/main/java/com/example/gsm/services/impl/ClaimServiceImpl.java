package com.example.gsm.services.impl;

import com.example.gsm.entity.SmsMessageWsk;
import com.example.gsm.entity.repository.SmsMessageWskRepository;
import com.example.gsm.services.ClaimService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClaimServiceImpl implements ClaimService {

    private final MongoTemplate mongoTemplate;

    private final SmsMessageWskRepository messageRepo;


    @Override
    public List<SmsMessageWsk> claimBatch(int batchSize, String workerId) {
        List<SmsMessageWsk> claimed = new ArrayList<>();
        for (int i = 0; i < batchSize; i++) {
            Query q = new Query(Criteria.where("status").is("WAIT"));
            Update u = new Update()
                    .set("status", "SENDING")
                    .set("lockedBy", workerId)
                    .set("lockedAt", LocalDateTime.now());
            FindAndModifyOptions opts = FindAndModifyOptions.options().returnNew(true);
            SmsMessageWsk m = mongoTemplate.findAndModify(q, u, opts, SmsMessageWsk.class);
            if (m == null) break;
            claimed.add(m);
        }
        return claimed;
    }
}
