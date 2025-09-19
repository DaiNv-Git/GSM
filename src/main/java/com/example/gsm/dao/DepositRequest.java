package com.example.gsm.dao;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DepositRequest {

    @NotNull
    private Long accountId;

    @NotNull
    @Min(1)
    private Double amount;
}
