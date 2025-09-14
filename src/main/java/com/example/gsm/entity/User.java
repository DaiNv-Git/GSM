package com.example.gsm.entity;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
@Data
@Document(collection = "users")
public class User {
    @Id
    private String id;
    private String accountId;
    private String firstName;
    private String lastName;
    private boolean isAgent;
    private boolean isActive;
    private boolean isAdmin;
    private String localeCode;
    private String balanceAmount;
    // ... thêm các field khác nếu cần
}