package com.example.gsm.services;

import com.example.gsm.dao.StatisticsRequest;
import com.example.gsm.dao.StatisticsSimpleResponse;

public interface StatisticsService {
    StatisticsSimpleResponse getStatistics(StatisticsRequest req);
}
