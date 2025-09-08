package com.example.gsm.repositories;

import com.example.gsm.dao.DashboardRequest;
import com.example.gsm.dao.DashboardResponse;
import com.example.gsm.dao.TypeTotalsRequest;
import com.example.gsm.dao.TypeTotalsResponse;

public interface DashboardService {
    DashboardResponse getDashboard(DashboardRequest req);
    TypeTotalsResponse getTypeTotals(TypeTotalsRequest req);

}
