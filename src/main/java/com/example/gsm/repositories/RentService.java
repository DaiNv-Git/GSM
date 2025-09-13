package com.example.gsm.repositories;

import com.example.gsm.dao.RentRequest;
import com.example.gsm.dao.RentResponse;
import com.example.gsm.dao.ResponseCommon;

public interface RentService {
    ResponseCommon<RentResponse> getRent(RentRequest req);

}
