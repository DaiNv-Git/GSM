package com.example.gsm.services.impl;

import com.example.gsm.entity.CallRecord;
import com.example.gsm.entity.repository.CallRecordRepository;
import com.example.gsm.services.CallRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
@Service
@RequiredArgsConstructor
public class CallRecordServiceImpl implements CallRecordService {
    private final CallRecordRepository repository;

    @Override
    public Page<CallRecord> searchByCustomerAndCreatedTime(Long customerId, Instant start, Instant end, Pageable pageable) {
        return repository.findByCustomerIdAndCreatedAtBetween(customerId, start, end, pageable);
    }

}
