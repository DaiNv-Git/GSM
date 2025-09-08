package com.example.gsm.repositories;

import com.example.gsm.dao.StatisticsRequest;
import com.example.gsm.dao.StatisticsSimpleResponse;

public interface StatisticsService {
    StatisticsSimpleResponse getStatistics(StatisticsRequest req);
}
