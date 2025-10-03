package com.example.gsm.services;

import com.example.gsm.entity.SmsMessageWsk;

import java.util.List;

public interface ClaimService {
    List<SmsMessageWsk> claimBatch(int batchSize, String workerId) ;
    List<SmsMessageWsk> claimBatchCountry(int batchSize, String workerId, String countryCode);
}
