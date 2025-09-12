package com.example.gsm.controller;

import com.example.gsm.dao.*;
import com.example.gsm.repositories.DashboardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    @PostMapping("/overview")
    public ResponseCommon<DashboardResponse> getDashboard(@Valid @RequestBody DashboardRequest request) {
        return dashboardService.getDashboard(request);
    }

    @PostMapping("/type-totals")
    public ResponseCommon<TypeTotalsResponse> getTypeTotals(@Valid @RequestBody TypeTotalsRequest request) {
        return dashboardService.getTypeTotals(request);
    }
}
