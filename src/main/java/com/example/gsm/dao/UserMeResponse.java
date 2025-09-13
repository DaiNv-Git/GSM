package com.example.gsm.dao;

import lombok.*;

import java.util.List;

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class UserMeResponse {
    private String id;
    private String username;
    private String firstName;
    private String lastName;
    private Double balanceAmount;
    private Boolean isActive;
    private List<String> roles;

    private boolean isDev;
    private boolean isAdmin;
    private boolean isPartner;
    private boolean isAgent;
}
