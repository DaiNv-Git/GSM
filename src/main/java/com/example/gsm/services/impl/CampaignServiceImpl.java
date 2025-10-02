package com.example.gsm.services.impl;

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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CampaignServiceImpl implements CampaignService {
    private final SmsCampaignRepository campaignRepo;
    private final SmsMessageWskRepository messageRepo;

    @Override
    public String createCampaignFromExcel(
            MultipartFile file, String name, String type, String content,
            String autoReply, LocalDateTime endTime,String country) throws IOException {

        SmsCampaign campaign = SmsCampaign.builder()
                .name(name)
                .type(type)
                .createdAt(LocalDateTime.now())
                .startTime(LocalDateTime.now())
                .endTime(endTime)
                .status("NEW")
                .autoReplyTemplate(autoReply)
                .country(country)
                .build();
        campaign = campaignRepo.save(campaign);

        // parse excel chỉ lấy số điện thoại, gắn content từ input
        List<SmsMessageWsk> msgs = parseExcelFile(file, campaign.getId(), content);
        messageRepo.saveAll(msgs);

        campaign.setTotalMessages(msgs.size());
        campaignRepo.save(campaign);
        return campaign.getId();
    }

    private List<SmsMessageWsk> parseExcelFile(MultipartFile file, String campaignId, String content) throws IOException {
        List<SmsMessageWsk> out = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Cell phoneCell = row.getCell(0); // chỉ lấy cột 0 là SĐT
                if (phoneCell == null) continue;
                String phone = phoneCell.toString().trim();
                if (phone.isEmpty()) continue;

                SmsMessageWsk m = SmsMessageWsk.builder()
                        .campaignId(campaignId)
                        .phoneNumber(phone)
                        .content(content) // 👈 gán chung content từ FE
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


