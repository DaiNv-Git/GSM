package com.example.gsm.services;

import com.example.gsm.entity.SmsCampaign;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;


public interface CampaignService {
    List<SmsCampaign> findAll();
    SmsCampaign findById(String id);
    String create(SmsCampaign campaign);
    SmsCampaign update(String id, SmsCampaign campaign);
    boolean delete(String id);

    int addNumbers(List<String> phoneNumbers, String campaignId, String content) throws IOException;

}
