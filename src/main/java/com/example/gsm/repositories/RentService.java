package com.example.gsm.repositories;

import com.example.gsm.dao.RentRequest;
import com.example.gsm.dao.RentResponse;
import com.example.gsm.dao.RentServicesResponse;
import com.example.gsm.dao.ResponseCommon;

import java.util.List;
import java.util.Map;

public interface RentService {
    ResponseCommon<RentResponse> getRent(RentRequest req);
    ResponseCommon<List<RentServicesResponse>> getRentPrices(RentRequest req);
    void updateRentPrices(String serviceId, Map<String, Integer> newPrices);
}
