package com.example.gsm.repositories;

import com.example.gsm.dao.ProxyRequest;
import com.example.gsm.dao.ProxyResponse;
import com.example.gsm.dao.ResponseCommon;

public interface ProxyService {
    ResponseCommon<ProxyResponse> getProxy(ProxyRequest req);
}
