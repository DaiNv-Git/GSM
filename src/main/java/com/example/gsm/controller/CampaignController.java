package com.example.gsm.controller;

import com.example.gsm.entity.SmsCampaign;
import com.example.gsm.entity.SmsSession;
import com.example.gsm.entity.repository.SmsSessionRepository;
import com.example.gsm.services.CampaignService;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
public class CampaignController {
    private final CampaignService campaignService;
    private final SmsSessionRepository sessionRepo;

    // ✅ CRUD

    @PostMapping("/create")
    public ResponseEntity<?> createCampaign(@RequestBody SmsCampaign campaign) {
        String id = campaignService.create(campaign);
        return ResponseEntity.ok(Map.of("campaignId", id));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateCampaign(@PathVariable String id, @RequestBody SmsCampaign campaign) {
        SmsCampaign updated = campaignService.update(id, campaign);
        if (updated == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Campaign không tồn tại"));
        }
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteCampaign(@PathVariable String id) {
        boolean deleted = campaignService.delete(id);
        if (!deleted) {
            return ResponseEntity.status(404).body(Map.of("error", "Campaign không tồn tại"));
        }
        return ResponseEntity.ok(Map.of("message", "Xóa thành công"));
    }

    @GetMapping("/get-all")
    public ResponseEntity<?> findAllCampaigns() {
        return ResponseEntity.ok(campaignService.findAll());
    }

    @GetMapping("/find/{id}")
    public ResponseEntity<?> findCampaignById(@PathVariable String id) {
        var campaign = campaignService.findById(id);
        if (campaign == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Campaign không tồn tại"));
        }
        return ResponseEntity.ok(campaign);
    }

    // ✅ Upload file Excel -> add số điện thoại vào campaign đã có
    @PostMapping(
            value = "/upload",
            consumes = {MediaType.MULTIPART_FORM_DATA_VALUE}
    )
    public ResponseEntity<?> uploadNumbersToCampaign(
            @RequestParam("file") MultipartFile file,
            @RequestParam("campaignId") String campaignId,
            @RequestParam("content") String content
    ) throws IOException {
        // Check kích thước file
        long maxSize = 10 * 1024 * 1024;  //10MB
        if (file.getSize() > maxSize) {
            return ResponseEntity.badRequest().body(Map.of("error", "File quá lớn. Kích thước tối đa 10MB"));
        }

        String filename = file.getOriginalFilename();
        String mimeType = new Tika().detect(file.getInputStream());

        boolean isExcelMime = mimeType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                || mimeType.equals("application/vnd.ms-excel")
                || mimeType.equals("application/x-tika-ooxml")
                || mimeType.equals("application/zip"); // fallback cho .xlsx bị nhận sai

        boolean isExcelExt = filename.endsWith(".xls") || filename.endsWith(".xlsx");

        if (!(isExcelMime && isExcelExt)) {
            return ResponseEntity.badRequest().body(Map.of("error", "File không hợp lệ, không phải Excel"));
        }

//        int total = campaignService.addNumbersFromExcel(file, campaignId, content);
        List<SmsSession> sessions = campaignService.addNumbersFromExcel(file, campaignId, content);
        return ResponseEntity.ok(Map.of(
                "campaignId", campaignId,
                "totalSessions", sessions.size(),
                "sessions", sessions
        ));
    }

    // ✅ Lấy session theo campaign
    @GetMapping("/get-all-sessions")
    public List<SmsSession> getSessionsByCampaign(@RequestParam String id) {
        return sessionRepo.findByCampaignId(id);
    }
}
