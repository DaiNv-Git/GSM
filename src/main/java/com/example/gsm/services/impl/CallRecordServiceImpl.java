package com.example.gsm.services.impl;

import com.example.gsm.entity.CallRecord;
import com.example.gsm.entity.repository.CallRecordRepository;
import com.example.gsm.services.CallRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
@Service
@RequiredArgsConstructor
public class CallRecordServiceImpl implements CallRecordService {
    private final CallRecordRepository repository;

    @Override
    public List<CallRecord> searchByCustomerAndCreatedTime(Long customerId, Instant start, Instant end) {
        return repository.findByCustomerIdAndCreatedAtBetween(customerId, start, end);
    }
}
