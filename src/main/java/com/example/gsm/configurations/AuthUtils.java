package com.example.gsm.configurations;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

public class AuthUtils {

    private AuthUtils() {}

    /**
     * Kiểm tra authentication null hay không
     * @param authentication Authentication
     * @return ResponseEntity nếu Unauthorized, null nếu hợp lệ
     */
    public static ResponseEntity<?> checkUnauthorized(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return null;
    }
}

