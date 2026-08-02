package com.foodtraceability.security.handler;

import com.foodtraceability.security.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JWT登出处理器
 * 用于处理用户登出时的逻辑，将JWT令牌添加到黑名单中
 */
@Component
public class JwtLogoutHandler implements LogoutHandler {

    private static final Logger logger = LoggerFactory.getLogger(JwtLogoutHandler.class);


    public JwtLogoutHandler(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    private final JwtUtils jwtUtils;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        try {
            // 从请求头中获取JWT令牌
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                
                // 将令牌添加到黑名单
                jwtUtils.addToBlacklist(token);
                logger.info("用户登出，令牌已添加到黑名单");
            }
        } catch (Exception e) {
            logger.error("处理用户登出失败: {}", e.getMessage(), e);
        }
    }
}
