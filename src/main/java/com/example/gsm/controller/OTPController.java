package com.example.gsm.controller;


import com.example.gsm.dao.*;
import com.example.gsm.entity.ServiceEntity;
import com.example.gsm.entity.repository.ServiceRepository;
import com.example.gsm.repositories.OTPService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

import static com.example.gsm.comon.Constants.SUCCESS_CODE;
import static com.example.gsm.comon.Constants.SUCCESS_MESSAGE;


@RestController
@RequestMapping("/api/otp")
@RequiredArgsConstructor
public class OTPController {

    @Autowired
    OTPService otpService;

    private final ServiceRepository repository;

    @PostMapping("/overview")
    public ResponseCommon<OtpResponse> getOTPManager(@RequestBody OtpRequest req) {
        return otpService.getOverview(req);
    }

    @PostMapping("/details")
    public ResponseCommon<OtpDetailsPagedResponse> getOTPDetails(@RequestBody OtpDetailsRequest req) {
        return otpService.getOtpDetails(req);
    }

    @GetMapping("/services/get-all")
    public ResponseCommon<List<ServiceEntity>> getAll() {
        return new ResponseCommon<>(SUCCESS_CODE, SUCCESS_MESSAGE, repository.findAll());
    }

    @GetMapping("/services/{id}")
    public ResponseCommon<?> getById(@PathVariable String id) {
        return new ResponseCommon<>(SUCCESS_CODE, SUCCESS_MESSAGE, repository.findById(id));
    }

    @PostMapping("/services/create")
    public ResponseCommon<ServiceEntity> create(@RequestBody OtpServiceRequest req) {
        ServiceEntity service = ServiceEntity.builder()
                .code(req.getCode())
                .text(req.getText())
                .price(req.getPrice())
                .image(req.getImage())
                .invertLogo(req.isInvertLogo())
                .isActive(req.isActive())
                .isPrivate(req.isPrivate())
                .pricePerDay(req.getPricePerDay())
                .countryCode(req.getCountryCode())
                .messageLimit(req.getMessageLimit())
                .saleOffValue(req.getSaleOffValue())
                .supportFeatures(req.getSupportFeatures())
                .rentDurationPrices(req.getRentDurationPrices())
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();

        return new ResponseCommon<>(SUCCESS_CODE, SUCCESS_MESSAGE, repository.save(service));
    }

    @PutMapping("/services/update/{id}")
    public ResponseCommon<ServiceEntity> update(@PathVariable String id, @RequestBody OtpServiceRequest req) {
        ServiceEntity service = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found"));

        service.setText(req.getText());
        service.setPrice(req.getPrice());
        service.setImage(req.getImage());
        service.setInvertLogo(req.isInvertLogo());
        service.setActive(req.isActive());
        service.setPrivate(req.isPrivate());
        service.setPricePerDay(req.getPricePerDay());
        service.setCountryCode(req.getCountryCode());
        service.setMessageLimit(req.getMessageLimit());
        service.setSaleOffValue(req.getSaleOffValue());
        service.setSupportFeatures(req.getSupportFeatures());
        service.setRentDurationPrices(req.getRentDurationPrices());
        service.setUpdatedAt(new Date());

        return new ResponseCommon<>(SUCCESS_CODE, SUCCESS_MESSAGE, repository.save(service));
    }

    @DeleteMapping("/services/delete/{id}")
    public ResponseCommon<?> delete(@PathVariable String id) {
        repository.deleteById(id);
        return new ResponseCommon<>(SUCCESS_CODE, SUCCESS_MESSAGE, null);
    }

    @GetMapping("/services/search-details")
    public ResponseCommon<?> searchByName(@RequestParam String name) {
        return otpService.findServicesByAppName(name);
    }

    @GetMapping("/services/advanced-filter")
    @Operation(summary = "Lọc nâng cao dịch vụ", description = "Lọc dịch vụ theo mã, quốc gia, trạng thái, tính năng hỗ trợ, giá và thời gian tạo.")
    public ResponseCommon<List<ServiceEntity>> advancedFilter(
            @Parameter(description = "Mã dịch vụ (hỗ trợ regex, không phân biệt hoa/thường)", example = "YAHOO")
            @RequestParam(required = false) String code,
            @Parameter(description = "Mã quốc gia (JPN, USA, v.v.)", example = "JPN")
            @RequestParam(required = false) String countryCode,
            @Parameter(description = "Trạng thái hoạt động", example = "true")
            @RequestParam(required = false) Boolean isActive,
            @Parameter(description = "Dịch vụ riêng tư", example = "false")
            @RequestParam(required = false) Boolean isPrivate,
            @Parameter(description = "Hỗ trợ SMS", example = "true")
            @RequestParam(required = false) Boolean smsSupport,
            @Parameter(description = "Hỗ trợ gọi điện", example = "false")
            @RequestParam(required = false) Boolean callSupport,
            @Parameter(description = "Giá tối thiểu", example = "5")
            @RequestParam(required = false) Integer minPrice,
            @Parameter(description = "Giá tối đa", example = "10")
            @RequestParam(required = false) Integer maxPrice,
            @Parameter(description = "Thời gian tạo (12H, 24H, 48H, 72H)", example = "24H")
            @RequestParam(required = false) String timePeriod
    ) {
        return otpService.advancedFilter(code, countryCode, isActive, isPrivate, smsSupport, callSupport, minPrice, maxPrice, timePeriod);
    }
}
