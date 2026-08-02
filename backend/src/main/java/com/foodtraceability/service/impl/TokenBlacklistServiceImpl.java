package com.foodtraceability.service.impl;

import com.foodtraceability.service.TokenBlacklistService;
import com.foodtraceability.security.utils.JwtUtils;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.time.Duration;

/**
 * 令牌黑名单服务实现类
 * 用于管理已失效的JWT令牌
 * 注：实际生产环境应使用Redis等分布式缓存存储黑名单令牌
 */
@Service
public class TokenBlacklistServiceImpl implements TokenBlacklistService {
    
    private static final Logger logger = LoggerFactory.getLogger(TokenBlacklistServiceImpl.class);
    

    public TokenBlacklistServiceImpl(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    private final JwtUtils jwtUtils;

    private static final String KEY_PREFIX = "jwt:blacklist:";
    
    @Override
    @SuppressWarnings("null")
    public void addToBlacklist(String token) {
        try {
            // 使用JwtUtils的addToBlacklist方法
            jwtUtils.addToBlacklist(token);
            logger.info("令牌已加入黑名单");
        } catch (Exception e) {
            logger.error("将令牌加入黑名单失败: {}", e.getMessage(), e);
        }
    }
    
    @Override
    @SuppressWarnings("null")
    public boolean isBlacklisted(String token) {
        try {
            // 使用JwtUtils的isTokenInBlacklist方法
            boolean isBlacklisted = jwtUtils.isTokenInBlacklist(token);
            logger.debug("检查令牌黑名单状态，token: {}, isBlacklisted: {}", token.substring(0, Math.min(token.length(), 20)) + "...", isBlacklisted);
            return isBlacklisted;
        } catch (Exception e) {
            logger.error("检查黑名单失败: {}, token: {}", e.getMessage(), token.substring(0, Math.min(token.length(), 20)) + "...", e);
            // Redis连接失败时，直接返回false，不影响正常认证流程
            return false;
        }
    }
    
    @Override
    public void cleanupExpiredTokens() {
        // Redis TTL 会自动清理过期数据
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("sha256 failed", e);
        }
    }
}
