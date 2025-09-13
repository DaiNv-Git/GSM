package com.example.gsm.controller;
import com.example.gsm.dao.StatisticsRequest;
import com.example.gsm.dao.StatisticsSimpleResponse;
import com.example.gsm.services.StatisticsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {
    private final StatisticsService statisticsService;

    @PostMapping
    public StatisticsSimpleResponse getStatistics(@Valid @RequestBody StatisticsRequest req) {
        return statisticsService.getStatistics(req);
    }
}
