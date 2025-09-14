    package com.example.gsm.entity;
    
    import lombok.*;
    import org.springframework.data.annotation.Id;
    import org.springframework.data.mongodb.core.mapping.Document;
    
    import java.time.Instant;
    import java.util.List;
    
    @Document(collection = "users")
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
        private Integer discountRate;
        private Integer bonusChargeRate;
        private String apiKey;
        private String webhook;      
        private String orderWebhook;
    
        private boolean isDev;
        private boolean isAdmin;
        private boolean isPartner;
        private boolean isAgent;
        private boolean isActive;
    
        private Integer specialRole;
        private String platform;
    
        private List<String> permission; // []
    
        private Instant createdAt;
        private Instant updatedAt;
    
        private String referralCode;
        private Integer referralId;
        private Double referralBalance;
        private Integer referralRate;
        
        private String TRC20Address;
        
        private List<CryptoAddress> cryptoAddressList;
    
        /** Danh sách dịch vụ đã chọn */
        private List<String> selectedServices;
    
        /** Discount theo dịch vụ */
        private ServiceDiscount serviceDiscount;
    
        /** Sự kiện đặc biệt */
        private SpecialEvents specialEvents;
    
        /** Thông tin Telegram */
        private TelegramInfo telegramInfo;
    
        /** Thông tin Web */
        private WebInfo webInfo;
    
        // === Nested Classes ===
    
        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class CryptoAddress {
            private String value;
            private String type;
        }
    
        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ServiceDiscount {
            private Integer otpService;
            private Integer rentService;
        }
    
        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class SpecialEvents {
            private boolean isChooseLanguage;
        }
    
        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class TelegramInfo {
            private String username;
        }
    
        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class WebInfo {
            private String username;
            private String password;
        }
    }
