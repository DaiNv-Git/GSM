package com.example.gsm.controller;

import com.example.gsm.dao.RentRequest;
import com.example.gsm.dao.ResponseCommon;
import com.example.gsm.dao.SmsResponse;
import com.example.gsm.repositories.SmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.example.gsm.comon.Constants.CORE_ERROR_CODE;

@RestController
@RequestMapping("/api/sms")
@RequiredArgsConstructor
public class SmsController {
    @Autowired
    SmsService rentService;

    @PostMapping("/overview")
    public ResponseCommon<SmsResponse> getRentManager(@RequestBody RentRequest req) {
        try{
            return rentService.getSms(req);
        }catch (Exception ex){
            return new ResponseCommon<>(CORE_ERROR_CODE, ex.getMessage(), null);
        }
    }

}
