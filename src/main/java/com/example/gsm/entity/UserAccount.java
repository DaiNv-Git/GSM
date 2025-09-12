package com.example.gsm.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

// domain/UserAccount.java
@Document(collection = "users") // <— tên collection
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAccount {
    @Id
    private String id;
    private Long accountId;
    private String firstName;
    private String lastName;
    private String localeCode;
    private Double balanceAmount;
    private String apiKey;
    private boolean isDev, isAdmin, isPartner, isAgent, isActive;
    private Integer specialRole;
    private String platform;
    private List<String> permission;
    private Instant createdAt, updatedAt;
    private String referralCode; private Integer referralId;
    private Double referralBalance; private Integer referralRate;
    private String webhook, orderWebhook;

    @Getter @Setter
    public static class WebInfo {
        private String username;
        private String password;
    }
    private WebInfo webInfo;
}
