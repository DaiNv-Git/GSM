package com.example.gsm.entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "users")
public class User {
    @Id
    private String id;
    private String accountId;
    private String firstName;
    private String lastName;
    private String localeCode;
    private Double balanceAmount;
    private String apiKey;

    @Field("isAdmin")
    private boolean isAdmin;

    @Field("isDev")
    private boolean isDev;

    @Field("isPartner")
    private boolean isPartner;

    @Field("isAgent")
    private boolean isAgent;

    @Field("isActive")
    private boolean isActive;

    private String TRC20Address;
    private List<String> permission;

    private Instant createdAt;
    private Instant updatedAt;
    private Integer __v;

    private String webhook;
    private Double bonusChargeRate;
    private Double discountRate;
    private String referralId;
    private String languageCode;

    private SpecialEvents specialEvents;
    private Double referralBalance;

    private String platform;
    private ServiceDiscount serviceDiscount;
    private TelegramInfo telegramInfo;
    private List<String> selectedServices;
    private Double referralRate;
    private String referralCode;
    private WebInfo webInfo;
    private List<CryptoAddress> cryptoAddressList;
    private List<String> selectedProvider;
}

// ========== Embedded classes ==========

@Data
@AllArgsConstructor
@NoArgsConstructor
class SpecialEvents {
    private boolean isChooseLanguage;
    private String partnerId;
    private boolean isAgent;
    private String platform;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class ServiceDiscount {
    private Double otpService;
    private Double rentService;
    private Double specialRole;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class TelegramInfo {
    private String username;
    private String orderWebhook;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class WebInfo {
    private String username;
    private String password;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class CryptoAddress {
    private String value;
    private String type;
}