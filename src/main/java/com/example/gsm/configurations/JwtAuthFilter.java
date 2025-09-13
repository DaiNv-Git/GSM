package com.example.gsm.configurations;

import com.example.gsm.services.impl.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.stream.Collectors;

/**
 * Filter JWT:
 * - Nếu có Authorization: Bearer <token> thì parse và set SecurityContext
 * - Nếu không có token thì để request đi qua bình thường
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        // Lấy header Authorization
        final String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            // Không có token → cho request qua
            chain.doFilter(request, response);
            return;
        }

        // Lấy token ra
        final String token = header.substring(7);
        String username;
        try {
            username = jwtService.extractUsername(token);
        } catch (Exception e) {
            // Token lỗi → cho qua nhưng không set auth
            chain.doFilter(request, response);
            return;
        }

        // Nếu username khác null và chưa set auth trong context
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Lấy roles từ token
            var roles = jwtService.extractRoles(token).stream()
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                    .collect(Collectors.toSet());

            // Kiểm tra token hợp lệ
            if (jwtService.isValid(token, username)) {
                // Tạo Authentication
                var authToken = new UsernamePasswordAuthenticationToken(username, null, roles);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Set Authentication vào context
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // Cho request qua filter chain
        chain.doFilter(request, response);
    }
}
