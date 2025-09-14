package com.example.gsm.repositories;

import com.example.gsm.dao.*;
import com.example.gsm.entity.ServiceEntity;
import org.bson.Document;

import java.util.List;

public interface OTPService {
    ResponseCommon<OtpResponse> getOverview(OtpRequest req);

    ResponseCommon<OtpDetailsPagedResponse> getOtpDetails(OtpDetailsRequest req);

    ResponseCommon<List<Document>> findServicesByAppName(String name);

    ResponseCommon<List<ServiceEntity>> advancedFilter(String code,
                                                       String countryCode,
                                                       Boolean isActive,
                                                       Boolean isPrivate,
                                                       Boolean smsSupport,
                                                       Boolean callSupport,
                                                       Integer minPrice,
                                                       Integer maxPrice,
                                                       String timePeriod);
}
