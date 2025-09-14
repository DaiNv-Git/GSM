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
@RequestMapping("/api/rent")
@RequiredArgsConstructor
public class RentController {

    @Autowired
    RentService rentService;

    @PostMapping("/overview")
    public ResponseCommon<RentResponse> getRentManager(@RequestBody RentRequest req) {
        return rentService.getRent(req);
    }

    @PostMapping("services/prices")
    public ResponseCommon<List<RentServicesResponse>> getRentPrices(@RequestBody RentRequest req) {
        return rentService.getRentPrices(req);
    }

    // API 2: Cập nhật giá
    @PostMapping("services/prices/update")
    public ResponseCommon<?> updateRentPrices(
            @RequestParam String serviceId,
            @RequestBody Map<String, Integer> newPrices) {
         rentService.updateRentPrices(serviceId, newPrices);
         return new ResponseCommon<>(SUCCESS_CODE, "Success",  null);
    }
}
