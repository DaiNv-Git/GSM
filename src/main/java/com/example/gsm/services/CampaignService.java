package com.example.gsm.services;

import com.example.gsm.entity.SmsCampaign;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;


public interface CampaignService {

     List<SmsCampaign> findAll();

     SmsCampaign findById(String id);

    String createCampaignFromExcel(MultipartFile file, String name, String type, String content,String autoReply, LocalDateTime endTime,String country) throws IOException;
}
