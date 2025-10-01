package com.example.gsm.services;

import com.example.gsm.dao.request.StatisticsRequest;
import com.example.gsm.dao.response.StatisticsSimpleResponse;

public interface StatisticsService {
    StatisticsSimpleResponse getStatistics(StatisticsRequest req);
}
