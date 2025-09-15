package com.example.gsm.repositories;

import com.example.gsm.dao.RentRequest;
import com.example.gsm.dao.ResponseCommon;
import com.example.gsm.dao.SmsResponse;

public interface SmsService {
    ResponseCommon<SmsResponse> getSms(RentRequest req);
}
