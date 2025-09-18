package com.example.gsm.controller;

import com.example.gsm.dao.ResponseCommon;
import com.example.gsm.dao.StatisticsRequest;
import com.example.gsm.dao.StatisticsSimpleResponse;
import com.example.gsm.repositories.StatisticsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import static com.example.gsm.comon.Constants.CORE_ERROR_CODE;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {
    private final StatisticsService statisticsService;

    @PostMapping
    public ResponseCommon<StatisticsSimpleResponse> getStatistics(@Valid @RequestBody StatisticsRequest req) {
        try{
            return statisticsService.getStatistics(req);
        }catch (Exception ex){
            return new ResponseCommon<>(CORE_ERROR_CODE, ex.getMessage(), null);
        }
    }
}
