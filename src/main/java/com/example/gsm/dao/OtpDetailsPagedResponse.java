package com.example.gsm.dao;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OtpDetailsPagedResponse {
    private OtpResponse.RevenueMetrics revenueMetrics;
    private PagedResponse<OtpDetailsResponse> page;
}
