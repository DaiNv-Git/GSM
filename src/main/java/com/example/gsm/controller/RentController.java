package com.example.gsm.controller;

import com.example.gsm.dao.RentRequest;
import com.example.gsm.dao.RentResponse;
import com.example.gsm.dao.ResponseCommon;
import com.example.gsm.repositories.RentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rent")
@RequiredArgsConstructor
public class RentController {

    @Autowired
    RentService rentService;

    @PostMapping("/overview")
    public ResponseCommon<RentResponse> getRentManager(@RequestBody RentRequest req) {
        return rentService.getRent(req);
    }
}
