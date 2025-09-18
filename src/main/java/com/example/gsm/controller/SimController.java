package com.example.gsm.controller;


import com.example.gsm.services.SimService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SimController {

    private final SimService simService;

    @PostMapping("/simlist")
    public ResponseEntity<?> createSimList(@RequestBody String json) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            log.info("=== [SimController] Nhận request /api/simlist === at {}",
                    LocalDateTime.now().format(formatter));

            log.debug("Payload JSON: {} at {}", json, LocalDateTime.now().format(formatter));

            System.out.println("nhận sim json at " + LocalDateTime.now().format(formatter));
            
            simService.processSimJson(json);
    
            return ResponseEntity.ok("Nhận dữ liệu thành công!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Có lỗi khi xử lý dữ liệu: " + e.getMessage());
        }
    }
}
