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

import static com.example.gsm.comon.Constants.CORE_ERROR_CODE;
import static com.example.gsm.comon.Constants.SUCCESS_CODE;

@RestController
@RequestMapping("/api/rent")
@RequiredArgsConstructor
public class RentController {

    @Autowired
    RentService rentService;

    @PostMapping("/overview")
    public ResponseCommon<RentResponse> getRentManager(@RequestBody RentRequest req) {
        try{
            return rentService.getRent(req);
        }catch (Exception ex){
            return new ResponseCommon<>(CORE_ERROR_CODE, ex.getMessage(), null);
        }
    }

    @PostMapping("services/prices")
    public ResponseCommon<List<RentServicesResponse>> getRentPrices(@RequestBody RentRequest req) {
        try{
            return rentService.getRentPrices(req);
        }catch (Exception ex){
            return new ResponseCommon<>(CORE_ERROR_CODE, ex.getMessage(), null);
        }
    }

    // API 2: Cập nhật giá
    @PostMapping("services/prices/update")
    public ResponseCommon<?> updateRentPrices(
            @RequestParam String serviceId,
            @RequestBody Map<String, Integer> newPrices) {
        try{
            rentService.updateRentPrices(serviceId, newPrices);
            return new ResponseCommon<>(SUCCESS_CODE, "Success",  null);
        }catch (Exception ex){
            return new ResponseCommon<>(CORE_ERROR_CODE, ex.getMessage(), null);
        }
    }
}
