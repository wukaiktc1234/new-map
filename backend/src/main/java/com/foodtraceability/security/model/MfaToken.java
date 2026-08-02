package com.foodtraceability.security.model;

import java.io.Serializable;
import java.time.Instant;

/**
 * MFA令牌模型
 * 用于MFA验证流程中的临时令牌管理
 */
public class MfaToken implements Serializable {

    private static final long serialVersionUID = 1L;

    private String token;
    private String userId;
    private Instant createdAt;
    private Instant expiresAt;
    private boolean used;

    public MfaToken(String token, String userId, long expirationInSeconds) {
        this.token = token;
        this.userId = userId;
        this.createdAt = Instant.now();
        this.expiresAt = Instant.now().plusSeconds(expirationInSeconds);
        this.used = false;
    }

    /**
     * 检查令牌是否已过期
     * @return true-已过期，false-未过期
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    /**
     * 检查令牌是否有效（未过期且未使用）
     * @return true-有效，false-无效
     */
    public boolean isValid() {
        return !isExpired() && !used;
    }

    /**
     * 标记令牌为已使用
     */
    public void markAsUsed() {
        this.used = true;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }
}
