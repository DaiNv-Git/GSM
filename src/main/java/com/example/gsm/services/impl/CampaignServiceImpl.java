package com.example.gsm.services.impl;

import com.example.gsm.dao.response.UploadResponseDto;
import com.example.gsm.entity.PricingConfig;
import com.example.gsm.entity.SmsCampaign;
import com.example.gsm.entity.SmsMessageWsk;
import com.example.gsm.entity.repository.SmsCampaignRepository;
import com.example.gsm.entity.repository.SmsMessageWskRepository;
import com.example.gsm.services.CampaignService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
@Service
@RequiredArgsConstructor
public class CampaignServiceImpl implements CampaignService {
    private final SmsCampaignRepository campaignRepo;
    private final SmsMessageWskRepository messageRepo;

    @Override
    public List<SmsCampaign> findAll() {
        return campaignRepo.findAll();
    }

    @Override
    public SmsCampaign findById(String id) {
        return campaignRepo.findById(id).orElse(null);
    }

    @Override
    public String create(SmsCampaign campaign) {
        campaign.setCreatedAt(LocalDateTime.now());
        campaign.setStartTime(LocalDateTime.now());
        campaign.setStatus("NEW");
        campaign = campaignRepo.save(campaign);
        return campaign.getId();
    }

    @Override
    public SmsCampaign update(String id, SmsCampaign campaign) {
        return campaignRepo.findById(id).map(existing -> {
            existing.setName(campaign.getName());
            existing.setType(campaign.getType());
            existing.setEndTime(campaign.getEndTime());
            existing.setAutoReplyTemplate(campaign.getAutoReplyTemplate());
            existing.setCountry(campaign.getCountry());
            existing.setStatus(campaign.getStatus());
            return campaignRepo.save(existing);
        }).orElse(null);
    }

    @Override
    public boolean delete(String id) {
        if (!campaignRepo.existsById(id)) return false;
        campaignRepo.deleteById(id);
        return true;
    }
    @Override
    public List<UploadResponseDto> addNumbers(List<String> phoneNumbers, String campaignId, String content) throws IOException {
        List<SmsMessageWsk> msgs = new ArrayList<>();
        List<UploadResponseDto> responseList = new ArrayList<>();

        for (String phone : phoneNumbers) {
            if (phone == null || phone.trim().isEmpty()) continue;

            SmsMessageWsk m = SmsMessageWsk.builder()
                    .campaignId(campaignId)
                    .phoneNumber(phone.trim())
                    .content(content)
                    .direction("OUTBOUND")
                    .status("WAIT")
                    .createdAt(LocalDateTime.now())
                    .localMsgId(UUID.randomUUID().toString())
                    .retryCount(0)
                    .build();

            msgs.add(m);
            responseList.add(new UploadResponseDto(campaignId, phone.trim(), content));
        }

        messageRepo.saveAll(msgs);

        // update tổng số tin nhắn trong campaign
        SmsCampaign campaign = campaignRepo.findById(campaignId).orElse(null);
        if (campaign != null) {
            int total = (campaign.getTotalMessages() != null ? campaign.getTotalMessages() : 0) + msgs.size();
            campaign.setTotalMessages(total);
            campaignRepo.save(campaign);
        }

        return responseList;
    }

   

    private List<SmsMessageWsk> parseExcelFile(MultipartFile file, String campaignId, String content) throws IOException {
        List<SmsMessageWsk> out = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Cell phoneCell = row.getCell(0);
                if (phoneCell == null) continue;
                String phone = phoneCell.toString().trim();
                if (phone.isEmpty()) continue;

                SmsMessageWsk m = SmsMessageWsk.builder()
                        .campaignId(campaignId)
                        .phoneNumber(phone)
                        .content(content)
                        .direction("OUTBOUND")
                        .status("WAIT")
                        .createdAt(LocalDateTime.now())
                        .localMsgId(UUID.randomUUID().toString())
                        .retryCount(0)
                        .build();
                out.add(m);
            }
        }
        return out;
    }
}


