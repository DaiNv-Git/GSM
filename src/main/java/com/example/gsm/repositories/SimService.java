package com.example.gsm.repositories;

import com.example.gsm.dao.ResponseCommon;
import com.example.gsm.dao.SimRequest;
import com.example.gsm.entity.Sim;

import java.util.List;

public interface SimService {
    ResponseCommon<List<Sim>> getSim(SimRequest req);
}
