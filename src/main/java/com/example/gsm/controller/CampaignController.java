package com.example.gsm.controller;

import com.example.gsm.entity.SmsSession;
import com.example.gsm.entity.repository.SmsSessionRepository;
import com.example.gsm.services.CampaignService;
import com.example.gsm.services.impl.SmsSessionServiceImpl;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
public class CampaignController {
    private final CampaignService campaignService;
    private final SmsSessionRepository sessionRepo;

    @PostMapping(
            value = "/upload",
            consumes = { MediaType.MULTIPART_FORM_DATA_VALUE }
    )
    public ResponseEntity<?> uploadCampaign(
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam("type") String type, // ONE_WAY | TWO_WAY
            @RequestParam("content") String content, // 👈 nội dung tin nhắn nhập từ FE
            @RequestParam(value="countryCode", required=false, defaultValue="VN") String countryCode,
            @RequestParam(value="autoReply", required=false) String autoReply,
            @RequestParam(value="endTime", required=false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) throws IOException {

        // Check kích thước file
        long maxSize = 10 * 1024 * 1024; // 10MB
        if (file.getSize() > maxSize) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("error", "File quá lớn. Kích thước tối đa 10MB"));
        }

        // check đuôi file
        String filename = file.getOriginalFilename();
        if (!(filename.endsWith(".xls") || filename.endsWith(".xlsx"))) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("error", "File không phải Excel"));
        }

        // Check MIME type bằng Tika
        Tika tika = new Tika();
        String mimeType = tika.detect(file.getInputStream());
        if (!(mimeType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") ||
                mimeType.equals("application/vnd.ms-excel") ||
                mimeType.equals("application/x-tika-ooxml"))) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("error", "File không hợp lệ, không phải Excel"));
        }

        String campaignId = campaignService.createCampaignFromExcel(file, name, type, content, autoReply, endTime, countryCode);
        return ResponseEntity.ok(Map.of("campaignId", campaignId));
    }

    @GetMapping("/get-all")
    public ResponseEntity<?> findAllCampaigns() {
        // Gọi service
        var campaigns = campaignService.findAll();
        return ResponseEntity.ok(campaigns);
    }

    @GetMapping("/find/{id}")
    public ResponseEntity<?> findCampaignById(@PathVariable String id) {
        var campaign = campaignService.findById(id);
        if (campaign == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Campaign không tồn tại"));
        }
        return ResponseEntity.ok(campaign);
    }

    @GetMapping("/get-all-sessions")
    public List<SmsSession> getSessionsByCampaign(@RequestParam String id) {
        return sessionRepo.findByCampaignId(id);
    }
}
