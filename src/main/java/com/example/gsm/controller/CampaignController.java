package com.example.gsm.controller;

import com.example.gsm.dao.request.PhoneUploadRequest;
import com.example.gsm.dao.response.UploadResponseDto;
import com.example.gsm.entity.SmsCampaign;
import com.example.gsm.entity.SmsSession;
import com.example.gsm.entity.repository.SmsSessionRepository;
import com.example.gsm.services.CampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/all")
    public ResponseEntity<?> findAllCampaigns() {
        return ResponseEntity.ok(campaignService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findCampaignById(@PathVariable String id) {
        var campaign = campaignService.findById(id);
        if (campaign == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Campaign không tồn tại"));
        }
        return ResponseEntity.ok(campaign);
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadNumbersToCampaign(@RequestBody PhoneUploadRequest request) throws IOException {
        if (request.getPhoneNumbers() == null || request.getPhoneNumbers().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Danh sách số điện thoại trống"));
        }

        List<UploadResponseDto> result = campaignService.addNumbers(
                request.getPhoneNumbers(),
                request.getCampaignId(),
                request.getContent()
        );

        return ResponseEntity.ok(result);
    }


    // ✅ Lấy session theo campaignId
    @GetMapping("/sessions")
    public List<SmsSession> getSessionsByCampaign(@RequestParam String campaignId) {
        return sessionRepo.findByCampaignId(campaignId);
    }
}
