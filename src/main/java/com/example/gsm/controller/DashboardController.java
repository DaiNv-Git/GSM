package com.example.gsm.controller;

import com.example.gsm.dao.DashboardRequest;
import com.example.gsm.dao.DashboardResponse;
import com.example.gsm.dao.TypeTotalsRequest;
import com.example.gsm.dao.TypeTotalsResponse;
import com.example.gsm.services.DashboardService;
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
    public DashboardResponse getDashboard(@Valid @RequestBody DashboardRequest request) {
        return dashboardService.getDashboard(request);
    }

    @PostMapping("/type-totals")
    public TypeTotalsResponse getTypeTotals(@Valid @RequestBody TypeTotalsRequest request) {
        return dashboardService.getTypeTotals(request);
    }
}
