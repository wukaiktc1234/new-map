package com.foodtraceability.service;

import com.foodtraceability.dto.*;
import com.foodtraceability.entity.User;
import java.util.Map;

/**
 * 认证服务接口
 * 定义认证相关的业务方法
 */
public interface AuthService {

    /**
     * 生成验证码
     */
    CaptchaResponse generateCaptcha();
    
    /**
     * 验证验证码
     */
    boolean validateCaptcha(String captchaId, String captcha);
    
    /**
     * 用户登录
     */
    LoginResponse login(LoginRequest loginRequest);
    
    /**
     * 用户注册
     */
    RegisterResponse register(RegisterRequest registerRequest);
    
    /**
     * 获取用户信息
     */
    UserInfoResponse getUserInfo(String userId);
    
    /**
     * 用户退出
     */
    void logout(String token);
    
    /**
     * 修改密码
     */
    void changePassword(String userId, ChangePasswordRequest changePasswordRequest);
    
    /**
     * 重置密码
     */
    void resetPassword(ResetPasswordRequest resetPasswordRequest);
    
    /**
     * 刷新令牌
     */
    String refreshToken(String token);
    
    /**
     * 检查用户名是否可用
     */
    boolean checkUsername(String username);
    
    /**
     * 检查邮箱是否可用
     */
    boolean checkEmail(String email);
    
    /**
     * 发送邮箱验证码
     */
    void sendEmailCode(String email, String type);
    
    /**
     * 验证邮箱验证码
     */
    boolean verifyEmailCode(String email, String code);
    
    /**
     * 获取用户权限
     */
    PermissionResponse getPermissions(String userId);
    
    /**
     * 获取用户菜单
     */
    MenuResponse getMenus(String userId);
    
    /**
     * 更新用户信息
     */
    UserInfoResponse updateUserInfo(String userId, UpdateUserRequest updateUserRequest);
    
    /**
     * 根据用户名获取用户
     */
    User getUserByUsername(String username);
    
    /**
     * 根据邮箱获取用户
     */
    User getUserByEmail(String email);

    /**
     * 从JWT令牌中获取用户ID
     */
    String getUserIdFromToken(String token);

    // ==================== 忘记密码相关方法 ====================

    /**
     * 发送验证码（忘记密码用）
     *
     * @param usernameOrEmail 用户名或邮箱
     * @param type            类型：EMAIL/PHONE
     */
    void sendVerificationCode(String usernameOrEmail, String type);

    /**
     * 验证双因素验证码（忘记密码第一步）
     *
     * @param usernameOrEmail 用户名或邮箱
     * @param emailCode       邮箱验证码
     * @param phoneCode       手机验证码
     * @return 重置令牌
     */
    String verifyForResetPassword(String usernameOrEmail, String emailCode, String phoneCode);

    /**
     * 重置密码（使用令牌）
     *
     * @param resetToken  重置令牌
     * @param newPassword 新密码
     */
    void resetPasswordWithToken(String resetToken, String newPassword);

    /**
     * 验证重置令牌是否有效
     *
     * @param resetToken 重置令牌
     * @return 是否有效
     */
    boolean validateResetToken(String resetToken);

    /**
     * 重置用户登录失败计数并解锁账号
     * 用于运维人员在用户被锁定时手动重置
     *
     * @param username 用户名
     */
    void resetLoginFailCount(String username);

    /**
     * 校验邀请码有效性（注册前预校验，不绑定用户信息）
     * 仅校验邀请码是否存在、未使用、未过期，不校验绑定信息
     *
     * @param code 邀请码
     * @return Map 包含：valid（是否有效）、boundName（绑定姓名，仅有效时返回）、message（提示信息）
     */
    Map<String, Object> validateInvitationCode(String code);
}
