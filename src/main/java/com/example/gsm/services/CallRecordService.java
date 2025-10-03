package com.example.gsm.services;

import com.example.gsm.entity.CallRecord;

import java.time.Instant;
import java.util.List;

public interface CallRecordService {
    List<CallRecord> searchByCustomerAndCreatedTime(Long customerId, Instant start, Instant end);
}
