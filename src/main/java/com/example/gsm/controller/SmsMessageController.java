package com.example.gsm.controller;

import com.example.gsm.dao.response.SmsInfoResponse;
import com.example.gsm.dao.response.SmsOrderDTO;
import com.example.gsm.entity.SmsMessage;
import com.example.gsm.entity.repository.SmsMessageRepository;
import com.example.gsm.services.SmsOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sms")
@RequiredArgsConstructor
public class SmsMessageController {

    private final SmsMessageRepository smsMessageRepository;
    private final SmsOrderService smsOrderService;

    
    @GetMapping("/by-order/{orderId}")
    public List<SmsInfoResponse> getByOrderId(@PathVariable String orderId) {
        List<SmsMessage> messages = smsMessageRepository.findByOrderId(orderId);

        return messages.stream()
                .collect(Collectors.groupingBy(SmsMessage::getSimPhone))
                .values().stream()
                .map(list -> list.stream()
                        .max(Comparator.comparing(SmsMessage::getTimestamp))
                        .orElse(null)
                )
                .filter(m -> m != null)
                .map(msg -> {
                    String otp = extractOtp(msg.getContent());
                    return new SmsInfoResponse(
                            msg.getSimPhone(),
                            msg.getServiceCode(),
                            otp,
                            msg.getDurationMinutes()
                    );
                })
                .collect(Collectors.toList());
    }
    @Operation(
            summary = "Tìm kiếm SMS OTP theo customerId và ngày",
            description = "API join giữa `orders` và `sms_messages`. "
                    + "Chỉ lấy order type = `buy.otp.service`. "
                    + "Cho phép lọc theo customerId và khoảng ngày (from/to). "
                    + "Kết quả có phân trang."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Danh sách SMS OTP",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = SmsOrderDTO.class))
    )
    @GetMapping("/searchOtp")
    public Page<SmsOrderDTO> search(
            @Parameter(description = "ID của khách hàng (customerId )", example = "12345")
            @RequestParam Long customerId,

            @Parameter(description = "Ngày bắt đầu (yyyy-MM-dd)", example = "2025-09-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @Parameter(description = "Ngày kết thúc (yyyy-MM-dd). Bao gồm cả ngày này", example = "2025-09-02")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam String type,

            @Parameter(description = "Số trang (bắt đầu từ 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Số bản ghi mỗi trang", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        return smsOrderService.search(customerId, fromInstant, toInstant, type,PageRequest.of(page, size));
    }


    private String extractOtp(String content) {
        if (content == null) return null;
        Matcher m = Pattern.compile("\\b\\d{4,8}\\b").matcher(content);
        return m.find() ? m.group() : null;
    }
}
