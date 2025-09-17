package com.example.gsm.controller;


import com.example.gsm.services.SimService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SimController {

    private final SimService simService;

    @PostMapping("/simlist")
    public ResponseEntity<?> createSimList(@RequestBody String json) {
        try {
            simService.processSimJson(json);

            return ResponseEntity.ok("Nhận dữ liệu thành công!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Có lỗi khi xử lý dữ liệu: " + e.getMessage());
        }
    }
}
