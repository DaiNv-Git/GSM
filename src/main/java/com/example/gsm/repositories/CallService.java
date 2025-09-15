package com.example.gsm.repositories;

import com.example.gsm.dao.*;

public interface CallService {
    ResponseCommon<CallResponse> getCall(CallRequest req);
}
