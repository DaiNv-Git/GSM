package com.example.gsm.controller;

import com.example.gsm.dao.RentRequest;
import com.example.gsm.dao.RentResponse;
import com.example.gsm.dao.RentServicesResponse;
import com.example.gsm.dao.ResponseCommon;
import com.example.gsm.repositories.RentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.example.gsm.comon.Constants.SUCCESS_CODE;

@RestController
@RequestMapping("/api/proxy")
@RequiredArgsConstructor
public class ProxyController {

    @Autowired
    RentService rentService;

    @PostMapping("/overview")
    public ResponseCommon<RentResponse> getRentManager(@RequestBody RentRequest req) {
        return rentService.getRent(req);
    }
}
