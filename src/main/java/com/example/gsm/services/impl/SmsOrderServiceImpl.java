package com.example.gsm.services.impl;

import com.example.gsm.dao.response.SmsOrderDTO;
import com.example.gsm.entity.repository.SmsOrderCustomRepository;
import com.example.gsm.services.SmsOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SmsOrderServiceImpl implements SmsOrderService {
    private final SmsOrderCustomRepository repository;

    @Override
    public Page<SmsOrderDTO> search(Long customerId, Instant from, Instant to, Pageable pageable) {
        return repository.searchByCustomerAndDate(customerId, from, to, pageable);
    }
}
