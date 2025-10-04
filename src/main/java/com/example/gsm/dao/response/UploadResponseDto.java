package com.example.gsm.dao.response;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UploadResponseDto {
    private String campaignId;
    private String phoneNumber;
    private String content;
}
