package com.foodtraceability.security.service;

import com.foodtraceability.security.model.JwtToken;
import com.foodtraceability.security.model.SecurityUser;

import java.util.Optional;

public interface TokenService {

    /**
     * 生成JWT令牌
     * @param user 安全用户
     * @return JWT令牌
     */
    JwtToken generateToken(SecurityUser user);

    /**
     * 验证JWT令牌
     * @param token JWT令牌
     * @return 是否有效
     */
    boolean validateToken(String token);

    /**
     * 从令牌中获取用户信息
     * @param token JWT令牌
     * @return 安全用户
     */
    Optional<SecurityUser> getUserFromToken(String token);

    /**
     * 刷新令牌
     * @param refreshToken 刷新令牌
     * @return 新的JWT令牌
     */
    Optional<JwtToken> refreshToken(String refreshToken);

    /**
     * 使令牌失效
     * @param token JWT令牌
     * @return 是否成功
     */
    boolean invalidateToken(String token);

    /**
     * 检查令牌是否已失效
     * @param token JWT令牌
     * @return 是否已失效
     */
    boolean isTokenInvalidated(String token);

    /**
     * 获取令牌剩余有效期
     * @param token JWT令牌
     * @return 剩余有效期（毫秒）
     */
    long getTokenRemainingValidity(String token);
}
