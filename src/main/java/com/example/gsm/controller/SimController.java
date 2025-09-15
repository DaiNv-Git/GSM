package com.example.gsm.controller;

import com.example.gsm.dao.SimListRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class SimController {

    @PostMapping("/simlist")
    public ResponseEntity<?> createSimList(@RequestBody SimListRequest request) {
        System.out.println("Nhận JSON: " + request.getSimData());
        return ResponseEntity.ok().body("Nhận dữ liệu thành công!");
    }
}
