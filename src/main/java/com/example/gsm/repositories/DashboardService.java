package com.example.gsm.repositories;

import com.example.gsm.dao.*;

public interface DashboardService {
    ResponseCommon<DashboardResponse> getDashboard(DashboardRequest req);
    ResponseCommon<TypeTotalsResponse> getTypeTotals(TypeTotalsRequest req);

}
