package com.example.gsm.controller;

import com.example.gsm.dao.*;
import com.example.gsm.repositories.DashboardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.example.gsm.comon.Constants.CORE_ERROR_CODE;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    @PostMapping("/overview")
    public ResponseCommon<DashboardResponse> getDashboard(@Valid @RequestBody DashboardRequest request) {
        try {
            return dashboardService.getDashboard(request);
        }catch (Exception ex){
            return new ResponseCommon<>(CORE_ERROR_CODE, ex.getMessage(), null);
        }
    }

    @PostMapping("/type-totals")
    public ResponseCommon<TypeTotalsResponse> getTypeTotals(@Valid @RequestBody TypeTotalsRequest request) {
        try {
            return dashboardService.getTypeTotals(request);
        }catch (Exception ex){
            return new ResponseCommon<>(CORE_ERROR_CODE, ex.getMessage(), null);
        }
    }
}
