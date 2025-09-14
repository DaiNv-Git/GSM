package com.example.gsm.dao;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AgentResponse {
    private String id;
    private String accountId;
    private String firstName;
    private String lastName;
    private String balanceAmount;
}
