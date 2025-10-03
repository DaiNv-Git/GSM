package com.example.gsm.controller;

import com.example.gsm.entity.CallRecord;
import com.example.gsm.services.CallRecordService;
import lombok.RequiredArgsConstructor;
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
    public List<CallRecord> searchByCreatedAt(
            @RequestParam Long customerId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate
    ) {
        if (endDate == null) {
            endDate = startDate;
        }

        // ✅ Convert yyyy-MM-dd -> Instant UTC
        Instant start = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).minusNanos(1).toInstant();

        return callRecordService.searchByCustomerAndCreatedTime(customerId, start, end);
    }
}
