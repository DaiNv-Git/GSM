package com.example.gsm.entity.repository;

import com.example.gsm.entity.CallRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
@Repository
public interface CallRecordRepository  extends MongoRepository<CallRecord, String> {
    Page<CallRecord> findByCustomerIdAndCreatedAtBetween(
            Long customerId,
            Instant start,
            Instant end,
            Pageable pageable
    );

}
