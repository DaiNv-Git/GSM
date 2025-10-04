package com.example.gsm.controller;

import com.example.gsm.entity.CallRecord;
import com.example.gsm.services.CallRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.List;

@RestController
@RequestMapping("/api/call-records")
@RequiredArgsConstructor
public class CallRecordController {

    private final CallRecordService callRecordService;

    /**
     * Search CallRecord theo customerId và khoảng createdAt
     * Format ngày: yyyy-MM-dd
     * Nếu không truyền endDate -> mặc định = startDate
     */
    @GetMapping("/search-created")
    public Page<CallRecord> searchByCreatedAt(
            @RequestParam Long customerId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        if (endDate == null) {
            endDate = startDate;
        }

        Instant start = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).minusNanos(1).toInstant();

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        return callRecordService.searchByCustomerAndCreatedTime(customerId, start, end, pageable);
    }

}
