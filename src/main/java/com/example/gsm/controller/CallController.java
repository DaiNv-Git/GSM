package com.example.gsm.controller;

import com.example.gsm.dao.*;
import com.example.gsm.repositories.CallService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.example.gsm.comon.Constants.CORE_ERROR_CODE;

@RestController
@RequestMapping("/api/call")
@RequiredArgsConstructor
public class CallController {
    @Autowired
    CallService callService;

    @PostMapping("/overview")
    public ResponseCommon<CallResponse> getRentManager(@RequestBody CallRequest req) {
        try {
            return callService.getCall(req);
        }catch (Exception ex){
            return new ResponseCommon<>(CORE_ERROR_CODE, ex.getMessage(), null);
        }
    }
}
