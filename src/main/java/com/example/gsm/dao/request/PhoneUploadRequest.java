package com.example.gsm.dao.request;

import lombok.Data;
import java.util.List;

@Data
public class PhoneUploadRequest {
    private String campaignId;
    private String content;
    private List<String> phoneNumbers;
}
