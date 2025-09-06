package com.example.gsm.controller;

import com.example.gsm.dao.DashboardFilter;
import com.example.gsm.dao.DashboardResponse;
import com.example.gsm.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard API", description = "API cung cấp dữ liệu thống kê tổng quan (OTP, Rent, Sms, Call, Proxy) theo ngày/tháng/năm")
public class DashboardController {
    @Autowired
    private  DashboardService dashboardService;

    @GetMapping
    @Operation(
            summary = "Lấy dữ liệu dashboard",
            description = """
                    Trả về dữ liệu thống kê theo bộ lọc:
                    - DAY: toàn bộ ngày trong tháng (1-28/29/30/31)
                    - MONTH: 4 tuần trong tháng
                    - YEAR: 12 tháng trong năm

                    Tính toán summary, services, revenue, chart và growthRate (so sánh cùng kỳ trước).
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Dashboard data",
                            content = @Content(schema = @Schema(implementation = DashboardResponse.class))
                    )
            }
    )
    public DashboardResponse getDashboard(
            @Parameter(
                    name = "filter",
                    description = "Loại thống kê: DAY | MONTH | YEAR",
                    required = true,
                    in = ParameterIn.QUERY,
                    schema = @Schema(implementation = DashboardFilter.class)
            )
            @RequestParam DashboardFilter filter,

            @Parameter(
                    name = "date",
                    description = "Ngày tham chiếu (yyyy-MM-dd). " +
                            "Ví dụ: 2025-09-06 → nếu filter=DAY thì lấy tháng 9, " +
                            "nếu filter=MONTH thì lấy 4 tuần trong tháng 9, " +
                            "nếu filter=YEAR thì lấy cả năm 2025.",
                    required = true,
                    in = ParameterIn.QUERY,
                    example = "2025-09-06"
            )
            @RequestParam String date
    ) {
        LocalDate localDate = LocalDate.parse(date);
        return dashboardService.getDashboard(filter.name(), localDate);
    }
}
