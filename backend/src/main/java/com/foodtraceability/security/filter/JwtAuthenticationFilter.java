package com.foodtraceability.security.filter;

import com.foodtraceability.security.model.SecurityUser;
import com.foodtraceability.security.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);


    public JwtAuthenticationFilter(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    private final JwtUtils jwtUtils;
    
    @Value("${app.security.enabled:true}")
    private boolean securityEnabled;

    private static final List<String> PUBLIC_PATHS = Arrays.asList(
        "/auth/",
        "/v1/auth/",
        "/v1/pos/auth/",
        "/public/",
        "/v1/public/",
        "/v1/ocr/",
        "/v1/kitchen/",
        "/v1/kitchen-order/",
        "/v1/epson-printer/",
        "/onboarding/invitation/validate",
        "/onboarding/invitation/code/",
        "/actuator/health",
        "/swagger-ui",
        "/v3/api-docs",
        "/ws/",
        "/ws",
        "/uploads/"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        for (String publicPath : PUBLIC_PATHS) {
            if (path.startsWith(publicPath) || path.equals(publicPath) || path.contains("/ws/")) {
                logger.debug("Skipping JWT filter for public path: {}", path);
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (!securityEnabled) {
            logger.debug("Security disabled, skipping JWT filter");
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            String requestUri = request.getRequestURI();
            String method = request.getMethod();
            logger.debug("JWT Filter processing request: {} {}", method, requestUri);

            String authHeader = request.getHeader("Authorization");
            logger.debug("Authorization header: {}", authHeader != null ? "present" : "null");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.debug("No valid Authorization header found for request: {}", requestUri);
                filterChain.doFilter(request, response);
                return;
            }

            String token = authHeader.substring(7).trim();
            
            if (token.isEmpty() || token.isBlank() || token.length() < 20) {
                logger.warn("Invalid token format (empty/short): length={}", token.length());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"message\":\"Token格式无效，请重新登录\"}");
                return;
            }
            
            if (!isValidJwtFormat(token)) {
                logger.warn("Malformed JWT token detected, rejecting");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"message\":\"Token格式错误，请重新登录\"}");
                return;
            }
            logger.debug("Token extracted, validating...");

            if (!jwtUtils.validateToken(token)) {
                logger.warn("Token validation failed for request: {}", requestUri);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"message\":\"Token已过期，请重新登录\"}");
                return;
            }

            Optional<SecurityUser> userOptional = jwtUtils.getUserFromToken(token);
            if (userOptional.isEmpty()) {
                logger.warn("Failed to get user from token for request: {}", requestUri);
                filterChain.doFilter(request, response);
                return;
            }

            SecurityUser user = userOptional.get();
            logger.debug("User extracted from token: userId={}, username={}, roles={}, permissions={}", 
                user.getUserId(), user.getUsername(), user.getRoles(), user.getPermissions());

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    user, null, user.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            logger.debug("Authentication set in SecurityContext for user: {}", user.getUsername());
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            logger.error("JWT authentication failed: {}", e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }
    
    private boolean isValidJwtFormat(String token) {
        if (token == null || token.isEmpty()) return false;
        String[] parts = token.split("\\.");
        if (parts.length != 3) return false;
        
        try {
            java.util.Base64.getUrlDecoder().decode(parts[0]);
            java.util.Base64.getUrlDecoder().decode(parts[1]);
            
            String header = new String(java.util.Base64.getUrlDecoder().decode(parts[0]));
            if (header.contains("\"alg\":\"none\"") || header.contains("\"alg\": \"none\"")) {
                logger.warn("JWT None algorithm attack detected!");
                return false;
            }
            if (header.contains("\"alg\":\"HS256\"") || header.contains("\"alg\": \"HS256\"")) {
                logger.warn("JWT algorithm downgrade attack detected (HS256 instead of HS512)");
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
