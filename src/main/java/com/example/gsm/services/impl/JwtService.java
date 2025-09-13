package com.example.gsm.services.impl;

import org.springframework.stereotype.Service;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import java.security.Key;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secretBase64;

    private Key key() {
        // Giải base64 thành bytes và tạo HMAC key
        byte[] keyBytes = io.jsonwebtoken.io.Decoders.BASE64.decode(secretBase64);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Sinh JWT token với username, roles và thời gian hết hạn.
     */
    public String generateToken(String username, List<String> roles, long expirationMs) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .setSubject(username)
                .claim("roles", roles)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    /** Lấy username từ token */
    public String extractUsername(String token) {
        return parse(token).getBody().getSubject();
    }

    /** Lấy roles từ token */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Object val = parse(token).getBody().get("roles");
        if (val instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    /** Lấy thời gian hết hạn */
    public Date extractExpiration(String token) {
        return parse(token).getBody().getExpiration();
    }

    /** Kiểm tra token hợp lệ */
    public boolean isValid(String token, String username) {
        try {
            var jws = parse(token);
            return username.equals(jws.getBody().getSubject())
                    && jws.getBody().getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Jws<Claims> parse(String token) {
        return Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(token);
    }
}
