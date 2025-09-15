package com.example.gsm.controller;

import com.example.gsm.dao.*;
import com.example.gsm.repositories.SmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/call")
@RequiredArgsConstructor
public class CallController {
    @Autowired
    SmsService rentService;

    @PostMapping("/overview")
    public ResponseCommon<CallResponse> getRentManager(@RequestBody CallRequest req) {
        return rentService.get(req);
    }
}
