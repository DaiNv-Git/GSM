package com.example.gsm.services;

import com.example.gsm.dao.response.SmsOrderDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

public interface SmsOrderService {
    Page<SmsOrderDTO> search(Long customerId, Instant from, Instant to, Pageable pageable) ;
}
