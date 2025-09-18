package com.example.gsm.controller;

import com.example.gsm.dao.*;
import com.example.gsm.entity.ServiceEntity;
import com.example.gsm.entity.repository.ServiceRepository;
import com.example.gsm.repositories.OTPService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

import static com.example.gsm.comon.Constants.CORE_ERROR_CODE;
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
        try {
            return otpService.getOverview(req);
        } catch (Exception ex) {
            return new ResponseCommon<>(CORE_ERROR_CODE, ex.getMessage(), null);
        }
    }

    @PostMapping("/details")
    public ResponseCommon<OtpDetailsPagedResponse> getOTPDetails(@RequestBody OtpDetailsRequest req) {
        try {
            return otpService.getOtpDetails(req);
        } catch (Exception ex) {
            return new ResponseCommon<>(CORE_ERROR_CODE, ex.getMessage(), null);
        }
    }

    @GetMapping("/services/get-all")
    public ResponseCommon<List<ServiceEntity>> getAll() {
        try {
            return new ResponseCommon<>(SUCCESS_CODE, SUCCESS_MESSAGE, repository.findAll());
        } catch (Exception ex) {
            return new ResponseCommon<>(CORE_ERROR_CODE, ex.getMessage(), null);
        }
    }

    @GetMapping("/services/{id}")
    public ResponseCommon<?> getById(@PathVariable String id) {
        try {
            return new ResponseCommon<>(SUCCESS_CODE, SUCCESS_MESSAGE, repository.findById(id));
        } catch (Exception ex) {
            return new ResponseCommon<>(CORE_ERROR_CODE, ex.getMessage(), null);
        }
    }

    @PostMapping("/services/create")
    public ResponseCommon<ServiceEntity> create(@RequestBody OtpServiceRequest req) {
        try {
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
        } catch (Exception ex) {
            return new ResponseCommon<>(CORE_ERROR_CODE, ex.getMessage(), null);
        }
    }

    @PutMapping("/services/update/{id}")
    public ResponseCommon<ServiceEntity> update(@PathVariable String id, @RequestBody OtpServiceRequest req) {
        try {
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
        } catch (Exception ex) {
            return new ResponseCommon<>(CORE_ERROR_CODE, ex.getMessage(), null);
        }
    }

    @DeleteMapping("/services/delete/{id}")
    public ResponseCommon<?> delete(@PathVariable String id) {
        try {
            repository.deleteById(id);
            return new ResponseCommon<>(SUCCESS_CODE, SUCCESS_MESSAGE, null);
        } catch (Exception ex) {
            return new ResponseCommon<>(CORE_ERROR_CODE, ex.getMessage(), null);
        }
    }

    @GetMapping("/services/search-details")
    public ResponseCommon<?> searchByName(@RequestParam String name) {
        try {
            return otpService.findServicesByAppName(name);
        } catch (Exception ex) {
            return new ResponseCommon<>(CORE_ERROR_CODE, ex.getMessage(), null);
        }
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
        try {
            return otpService.advancedFilter(code, countryCode, isActive, isPrivate, smsSupport, callSupport, minPrice, maxPrice, timePeriod);
        } catch (Exception ex) {
            return new ResponseCommon<>(CORE_ERROR_CODE, ex.getMessage(), null);
        }
    }
}
