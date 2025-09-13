package com.example.gsm.dao;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterUserRequest {
    @NotBlank
    private String username;     
    @NotBlank
    private String password;     

    private String firstName;
    private String lastName;
}
