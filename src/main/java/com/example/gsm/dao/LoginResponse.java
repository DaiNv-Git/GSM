package com.example.gsm.dao;

import lombok.*;

import java.util.List;

@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class LoginResponse {
    private String accessToken;
    private long   accessTokenExpiresAt;  
    private String refreshToken;
    private long   refreshTokenExpiresAt;

    private String id;
    private String username;
    private String firstName;
    private String lastName;
    private List<String> roles;
    private Boolean isDev;
    private Boolean isAdmin;
    private Boolean isPartner;
    private Boolean isAgent;
}
