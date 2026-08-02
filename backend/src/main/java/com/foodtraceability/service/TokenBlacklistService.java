package com.foodtraceability.service;

/**
 * 令牌黑名单服务
 * 用于管理已失效的JWT令牌
 */
public interface TokenBlacklistService {
    
    /**
     * 将令牌加入黑名单
     * @param token JWT令牌
     */
    void addToBlacklist(String token);
    
    /**
     * 检查令牌是否在黑名单中
     * @param token JWT令牌
     * @return 如果令牌在黑名单中返回true，否则返回false
     */
    boolean isBlacklisted(String token);
    
    /**
     * 清除过期的黑名单令牌
     */
    void cleanupExpiredTokens();
}
