package com.foodtraceability.security.filter;

import com.foodtraceability.security.config.KdsAuthProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * KDS 托盘绑定专用认证过滤器。
 * <p>
 * 匹配格式：Authorization: Bearer kds_{storeId}_{secret}
 * 验证通过后在 SecurityContext 中设置 principal = storeId，authorities = [kitchen:edit]。
 * 不影响普通 JWT 用户认证（普通 token 不以 "kds_" 开头，直接跳过）。
 */
@Component
public class KdsTokenFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(KdsTokenFilter.class);
    private static final String KDS_PREFIX = "kds_";

    private final KdsAuthProperties kdsAuthProperties;

    public KdsTokenFilter(KdsAuthProperties kdsAuthProperties) {
        this.kdsAuthProperties = kdsAuthProperties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!kdsAuthProperties.isEnabled()) {
            return true;
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return true;
        }
        String token = authHeader.substring(7).trim();
        return !token.startsWith(KDS_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String authHeader = request.getHeader("Authorization");
            String token = authHeader.substring(7).trim();
            // 格式: kds_{storeId}_{secret}
            String[] parts = token.split("_", 3);
            if (parts.length != 3) {
                log.warn("KDS token 格式无效: 期望 kds_{{storeId}}_{{secret}}, 实际长度段数={}", parts.length);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"message\":\"KDS token 格式无效\"}");
                return;
            }

            String storeId = parts[1];
            String secret = parts[2];

            if (!kdsAuthProperties.isValidToken(storeId, secret)) {
                log.warn("KDS token 验证失败: storeId={}", storeId);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"message\":\"KDS token 无效或 store_id 不匹配\"}");
                return;
            }

            log.debug("KDS token 验证成功: storeId={}", storeId);

            KdsPrincipal principal = new KdsPrincipal(storeId);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null,
                            List.of(new SimpleGrantedAuthority("kitchen:edit")));
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            log.error("KDS 认证异常", e);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * KDS 设备主体，仅携带 storeId。
     */
    public static class KdsPrincipal {
        private final String storeId;

        public KdsPrincipal(String storeId) {
            this.storeId = storeId;
        }

        public String getStoreId() {
            return storeId;
        }

        @Override
        public String toString() {
            return "KDS:" + storeId;
        }
    }
}
