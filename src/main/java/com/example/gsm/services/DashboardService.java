package com.example.gsm.services;

import com.example.gsm.dao.request.DashboardRequest;
import com.example.gsm.dao.response.DashboardResponse;
import com.example.gsm.dao.request.TypeTotalsRequest;
import com.example.gsm.dao.response.TypeTotalsResponse;

public interface DashboardService {
    DashboardResponse getDashboard(DashboardRequest req);
    TypeTotalsResponse getTypeTotals(TypeTotalsRequest req);

}
