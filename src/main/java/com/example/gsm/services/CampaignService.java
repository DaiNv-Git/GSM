package com.example.gsm.services;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

public interface CampaignService {
    String createCampaignFromExcel(MultipartFile file, String name, String type, String content,String autoReply, LocalDateTime endTime,String country) throws IOException;
}
