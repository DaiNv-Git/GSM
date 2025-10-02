package com.example.gsm.controller;

import com.example.gsm.services.CampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
public class CampaignController {
    private final CampaignService campaignService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadCampaign(
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam("type") String type, // ONE_WAY | TWO_WAY
            @RequestParam("content") String content, // 👈 nội dung tin nhắn nhập từ FE
            @RequestParam(value="countryCode", required=false, defaultValue="VN") String countryCode,
            @RequestParam(value="autoReply", required=false) String autoReply,
            @RequestParam(value="endTime", required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) throws IOException {
        String campaignId = campaignService.createCampaignFromExcel(file, name, type, content, autoReply, endTime,countryCode);
        return ResponseEntity.ok(Map.of("campaignId", campaignId));
    }

}
