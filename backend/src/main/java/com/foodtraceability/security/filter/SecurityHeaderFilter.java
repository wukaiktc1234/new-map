package com.foodtraceability.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class SecurityHeaderFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(SecurityHeaderFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String path = request.getRequestURI();
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("X-XSS-Protection", "1; mode=block");
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            response.setHeader("X-Frame-Options", "DENY");
            response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
            response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
            if (!path.startsWith("/static/") && !path.startsWith("/uploads/") && !path.equals("/favicon.ico")) {
                response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, proxy-revalidate");
            }
        } catch (Exception e) {
            logger.warn("安全头部设置异常（不影响请求继续处理）: {}", e.getMessage());
        }
        filterChain.doFilter(request, response);
    }
}
