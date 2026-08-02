package com.foodtraceability.security.filter;

import com.foodtraceability.security.model.SecurityUser;
import com.foodtraceability.security.service.MfaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * MFA认证过滤器
 * 在JWT过滤器之后执行，检查已认证用户是否需要完成MFA验证
 * 通过检查SecurityUser中的mfaVerified标记判断MFA验证状态
 */
@Component
public class MfaAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(MfaAuthenticationFilter.class);

    private static final Set<String> MFA_EXCLUDED_PATHS = Set.of(
            "/v1/auth/mfa/verify",
            "/v1/auth/login",
            "/v1/auth/captcha",
            "/v1/auth/refresh",
            "/v1/auth/forgot-password/send-code",
            "/v1/auth/forgot-password/verify",
            "/v1/auth/forgot-password/reset",
            "/v1/auth/forgot-password/validate-token",
            "/v1/auth/register"
    );

    private final MfaService mfaService;

    private final ObjectMapper objectMapper;

    public MfaAuthenticationFilter(MfaService mfaService, ObjectMapper objectMapper) {
        this.mfaService = mfaService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestPath = request.getRequestURI();

        if (isExcludedPath(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        Object principal = authentication.getPrincipal();
        if (principal == null || "anonymousUser".equals(principal.toString())) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!(principal instanceof SecurityUser)) {
            filterChain.doFilter(request, response);
            return;
        }

        SecurityUser securityUser = (SecurityUser) principal;
        String userId = securityUser.getUserId();

        if (!mfaService.isMfaEnabled(userId)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (securityUser.isMfaVerified()) {
            filterChain.doFilter(request, response);
            return;
        }

        logger.warn("用户已启用MFA但未完成验证，拒绝访问: userId={}, path={}", userId, requestPath);
        sendMfaRequiredResponse(response);
    }

    private boolean isExcludedPath(String requestPath) {
        String path = requestPath;
        if (MFA_EXCLUDED_PATHS.contains(path)) {
            return true;
        }
        if (path.startsWith("/v1/public/") || path.startsWith("/v1/auth/mfa/")) {
            return true;
        }
        return false;
    }

    private void sendMfaRequiredResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("code", 403);
        responseBody.put("message", "MFA验证Required");
        responseBody.put("data", "MFA_REQUIRED");
        responseBody.put("timestamp", System.currentTimeMillis());

        response.getWriter().write(objectMapper.writeValueAsString(responseBody));
        response.getWriter().flush();
    }
}
