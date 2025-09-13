package com.example.gsm.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OtpDetailsResponse {
    private String serviceName; // stock.serviceCode
    private long operations; // tổng số thao tác / order (count).
    private long successCount; //số order có statusCode == "SUCCESS".
    private long refundCount; //số order refund (isRefund == true hoặc statusCode == "REFUND").
    private long bannedCount; //số account bị banned / flagged (mình giả định isActive == false hoặc statusCode == "BANNED"; bạn chỉnh điều kiện tuỳ thực tế).
    private double successRate;   // successCount / operations * 100 (trả về %).
    private double avgDiscount;   // (cột "AVG" trên UI) — trung bình discountRate (DB lưu 1..100), nên hiển thị % (vd 18.11%).
    private double income;        // tổng cost (sum(cost)), làm tròn 2 chữ số.
}
