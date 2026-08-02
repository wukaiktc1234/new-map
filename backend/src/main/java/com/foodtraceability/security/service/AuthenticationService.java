package com.foodtraceability.security.service;

import com.foodtraceability.security.model.JwtToken;
import com.foodtraceability.security.model.MfaToken;
import com.foodtraceability.security.model.SecurityUser;
import com.foodtraceability.security.service.dto.LoginRequest;
import com.foodtraceability.security.service.dto.LoginResponse;
import com.foodtraceability.security.service.dto.RegisterRequest;
import com.foodtraceability.security.service.dto.UserProfileUpdateRequest;
import com.foodtraceability.security.service.dto.PasswordChangeRequest;

import java.util.Optional;

public interface AuthenticationService {

    /**
     * 用户登录
     * @param loginRequest 登录请求
     * @return 登录响应
     */
    LoginResponse login(LoginRequest loginRequest);

    /**
     * 验证多因素认证代码
     * @param userId 用户ID
     * @param mfaToken MFA令牌
     * @param code 验证码
     * @return JWT令牌
     */
    Optional<JwtToken> verifyMfa(String userId, String mfaToken, String code);

    /**
     * 用户注册
     * @param registerRequest 注册请求
     * @return 安全用户
     */
    SecurityUser register(RegisterRequest registerRequest);

    /**
     * 更新用户资料
     * @param userId 用户ID
     * @param profileUpdateRequest 资料更新请求
     * @return 安全用户
     */
    SecurityUser updateUserProfile(String userId, UserProfileUpdateRequest profileUpdateRequest);

    /**
     * 修改密码
     * @param userId 用户ID
     * @param passwordChangeRequest 密码修改请求
     * @return 是否成功
     */
    boolean changePassword(String userId, PasswordChangeRequest passwordChangeRequest);

    /**
     * 刷新JWT令牌
     * @param refreshToken 刷新令牌
     * @return 新的JWT令牌
     */
    Optional<JwtToken> refreshToken(String refreshToken);

    /**
     * 注销
     * @param token JWT令牌
     * @return 是否成功
     */
    boolean logout(String token);

    /**
     * 获取当前用户
     * @return 安全用户
     */
    Optional<SecurityUser> getCurrentUser();

    /**
     * 根据用户名获取用户
     * @param username 用户名
     * @return 安全用户
     */
    Optional<SecurityUser> getUserByUsername(String username);

    /**
     * 根据用户ID获取用户
     * @param userId 用户ID
     * @return 安全用户
     */
    Optional<SecurityUser> getUserById(String userId);
}
