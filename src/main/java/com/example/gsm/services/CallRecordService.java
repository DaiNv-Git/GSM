package com.example.gsm.services;

import com.example.gsm.entity.CallRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

public interface CallRecordService {
    Page<CallRecord> searchByCustomerAndCreatedTime(Long customerId, Instant start, Instant end, Pageable pageable);
}
