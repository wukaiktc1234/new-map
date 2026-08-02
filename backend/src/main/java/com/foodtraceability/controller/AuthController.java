package com.foodtraceability.controller;

import com.foodtraceability.annotation.RateLimit;
import com.foodtraceability.annotation.RateLimitType;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.*;
import com.foodtraceability.entity.User;
import com.foodtraceability.exception.BusinessException;
import com.foodtraceability.exception.CaptchaRequiredException;
import com.foodtraceability.security.utils.PasswordUtils;
import com.foodtraceability.service.AuthService;
import com.foodtraceability.service.SessionService;
import com.foodtraceability.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "认证管理", description = "登录、注册、令牌管理等认证相关接口")
@RestController
@RequestMapping("/v1/auth")
@Validated
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);


    public AuthController(AuthService authService, UserService userService, SessionService sessionService) {
        this.authService = authService;
        this.userService = userService;
        this.sessionService = sessionService;
    }

    private final AuthService authService;

    private final UserService userService;
    
    private final SessionService sessionService;

    @Operation(summary = "获取验证码")
    @GetMapping("/captcha")
    @RateLimit(type = RateLimitType.IP, time = 60, value = 10, message = "验证码请求过于频繁，请稍后重试")
    public Result<CaptchaResponse> getCaptcha() {
        try {
            CaptchaResponse captchaResponse = authService.generateCaptcha();
            return Result.success(captchaResponse);
        } catch (Exception e) {
            logger.error("生成验证码失败", e);
            return Result.error("生成验证码失败");
        }
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    @RateLimit(type = RateLimitType.IP, time = 60, value = 5, message = "登录请求过于频繁，请稍后重试")
    public Result<LoginResponse> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request) {
        try {
            // 标准化用户名为小写，防止大小写绕过账户锁定和限流
            if (loginRequest.getUsername() != null) {
                loginRequest.setUsername(loginRequest.getUsername().toLowerCase().trim());
            }
            LoginResponse response = authService.login(loginRequest);
            if (response != null && response.isSuccess()) {
                String deviceInfo = extractDeviceInfo(request);
                String ipAddress = getClientIpAddress(request);
                sessionService.registerSession(
                    response.getUserInfo().getId(),
                    response.getToken(),
                    deviceInfo,
                    ipAddress
                );
                return Result.success(response);
            }
            return Result.error("登录失败");
        } catch (CaptchaRequiredException e) {
            logger.warn("需要验证码: username={}", loginRequest.getUsername());
            return Result.error(461, e.getMessage(), e.getLoginResponse());
        } catch (BusinessException e) {
            logger.warn("登录失败: username={}, code={}, reason={}", loginRequest.getUsername(), e.getCode(), e.getMessage());
            return Result.error(e.getCode(), e.getMessage());
        } catch (Exception e) {
            logger.error("登录服务异常: username={}", loginRequest.getUsername(), e);
            return Result.error("登录服务异常");
        }
    }
    
    @Operation(summary = "获取用户菜单")
    @GetMapping("/menus")
    public Result<MenuResponse> getMenus(@RequestParam(required = false) String userId, Principal principal) {
        // 如果没有提供userId参数，尝试从当前登录用户的信息中获取
        if (userId == null || userId.isEmpty()) {
            if (principal != null) {
                userId = principal.getName();
            } else {
                throw new BusinessException("请先登录");
            }
        }

        MenuResponse response = authService.getMenus(userId);
        return Result.success(response);
    }

    @Operation(summary = "用户注册（邀请制）")
    @PostMapping("/register")
    @RateLimit(type = RateLimitType.IP, time = 60, value = 3, message = "注册请求过于频繁，请稍后重试")
    public Result<RegisterResponse> register(@Valid @RequestBody InviteRegisterRequest registerRequest) {
        try {
            return Result.success(authService.register(registerRequest));
        } catch (BusinessException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            logger.error("注册失败", e);
            return Result.error("注册失败，请稍后重试");
        }
    }

    /**
     * 邀请码预校验（注册前校验邀请码有效性）
     * 公开接口，无需认证（/v1/auth/** 已在 SecurityConfig 中配置 permitAll）
     * 仅校验邀请码是否存在、未使用、未过期，不校验绑定信息（绑定信息在注册提交时校验）
     */
    @Operation(summary = "校验邀请码有效性", description = "注册前校验邀请码是否有效，返回绑定姓名用于友好提示")
    @PostMapping("/validate-invitation-code/{code}")
    @RateLimit(type = RateLimitType.IP, time = 60, value = 10, message = "邀请码校验请求过于频繁，请稍后重试")
    public Result<Map<String, Object>> validateInvitationCode(@Parameter(description = "邀请码") @PathVariable String code) {
        try {
            if (code == null || code.trim().isEmpty()) {
                return Result.error("邀请码不能为空");
            }
            Map<String, Object> result = authService.validateInvitationCode(code.trim());
            return Result.success(result);
        } catch (Exception e) {
            logger.error("邀请码校验失败: code={}", code, e);
            return Result.error("邀请码校验失败，请稍后重试");
        }
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    @RateLimit(type = RateLimitType.USER, time = 60, value = 3, message = "密码修改请求过于频繁，请稍后重试")
    public Result<String> changePassword(@Valid @RequestBody ChangePasswordRequest changePasswordRequest, Principal principal) {
        try {
            String userId;
            if (principal instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken) {
                Object principalObj = ((org.springframework.security.authentication.UsernamePasswordAuthenticationToken) principal).getPrincipal();
                if (principalObj instanceof com.foodtraceability.security.model.SecurityUser) {
                    userId = ((com.foodtraceability.security.model.SecurityUser) principalObj).getUserId();
                } else {
                    userId = principal.getName();
                }
            } else {
                userId = principal.getName();
            }
            authService.changePassword(userId, changePasswordRequest);
            return Result.success("密码修改成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "刷新令牌")
    @PostMapping("/refresh")
    // 安全修复（P1-1）：刷新令牌时 access token 已过期，SecurityContext 无 Authentication，
    // RateLimitType.USER 会退化为 anonymous 共享桶，导致所有用户共享同一限流配额。
    // 改为 IP 限流，确保每个IP独立计数。
    @RateLimit(type = RateLimitType.IP, time = 60, value = 10, message = "令牌刷新请求过于频繁，请稍后重试")
    public Result<String> refreshToken(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        try {
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ") || authorizationHeader.length() <= 7) {
                return Result.error("无效的令牌格式");
            }
            String token = authorizationHeader.substring(7);
            String newToken = authService.refreshToken(token);
            return Result.success(newToken);
        } catch (Exception e) {
            return Result.error("刷新令牌失败");
        }
    }

    @Operation(summary = "注销")
    @PostMapping("/logout")
    @RateLimit(type = RateLimitType.USER, time = 60, value = 5, message = "注销请求过于频繁，请稍后重试")
    public Result<String> logout(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        try {
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ") || authorizationHeader.length() <= 7) {
                return Result.error("无效的令牌格式");
            }
            String token = authorizationHeader.substring(7);
            authService.logout(token);
            return Result.success("注销成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public Result<UserInfoResponse> getCurrentUser(Principal principal) {
        try {
            if (principal == null) {
                return Result.error(401, "未认证，请先登录");
            }
            String userId;
            if (principal instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken) {
                Object principalObj = ((org.springframework.security.authentication.UsernamePasswordAuthenticationToken) principal).getPrincipal();
                if (principalObj instanceof com.foodtraceability.security.model.SecurityUser) {
                    userId = ((com.foodtraceability.security.model.SecurityUser) principalObj).getUserId();
                } else {
                    userId = principal.getName();
                }
            } else {
                userId = principal.getName();
            }
            return Result.success(authService.getUserInfo(userId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "获取用户信息")
    @GetMapping("/user-info")
    public Result<UserInfoResponse> getUserInfo(@RequestParam(required = false) String userId, Principal principal) {
        try {
            if (userId == null || userId.isEmpty()) {
                if (principal == null) {
                    return Result.error("未登录或未提供用户ID");
                }
                if (principal instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken) {
                    Object principalObj = ((org.springframework.security.authentication.UsernamePasswordAuthenticationToken) principal).getPrincipal();
                    if (principalObj instanceof com.foodtraceability.security.model.SecurityUser) {
                        userId = ((com.foodtraceability.security.model.SecurityUser) principalObj).getUserId();
                    } else {
                        userId = principal.getName();
                    }
                } else {
                    userId = principal.getName();
                }
            }

            return Result.success(authService.getUserInfo(userId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    // ==================== 忘记密码相关接口 ====================

    @Operation(summary = "发送验证码（忘记密码）")
    @PostMapping("/forgot-password/send-code")
    @RateLimit(type = RateLimitType.IP, time = 60, value = 3, message = "验证码发送请求过于频繁，请稍后重试")
    public Result<String> sendVerificationCode(@Valid @RequestBody SendVerificationCodeRequest request) {
        try {
            authService.sendVerificationCode(request.getUsernameOrEmail(), request.getType());
            return Result.success("验证码已发送");
        } catch (BusinessException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            logger.error("发送验证码失败", e);
            return Result.error("发送验证码失败，请稍后重试");
        }
    }

    @Operation(summary = "验证双因素验证码（忘记密码第一步）")
    @PostMapping("/forgot-password/verify")
    @RateLimit(type = RateLimitType.IP, time = 60, value = 5, message = "验证请求过于频繁，请稍后重试")
    public Result<String> verifyForResetPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            String resetToken = authService.verifyForResetPassword(
                    request.getUsernameOrEmail(),
                    request.getEmailCode(),
                    request.getPhoneCode()
            );
            return Result.success(resetToken);
        } catch (BusinessException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            logger.error("验证失败", e);
            return Result.error("验证失败，请稍后重试");
        }
    }

    @Operation(summary = "重置密码（使用令牌）")
    @PostMapping("/forgot-password/reset")
    @RateLimit(type = RateLimitType.IP, time = 60, value = 3, message = "密码重置请求过于频繁，请稍后重试")
    public Result<String> resetPasswordWithToken(@Valid @RequestBody ResetPasswordWithTokenRequest request) {
        try {
            // 验证两次密码是否一致
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                return Result.error("两次输入的密码不一致");
            }

            authService.resetPasswordWithToken(request.getResetToken(), request.getNewPassword());
            return Result.success("密码重置成功");
        } catch (BusinessException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            logger.error("重置密码失败", e);
            return Result.error("重置密码失败，请稍后重试");
        }
    }

    @Operation(summary = "验证重置令牌是否有效")
    @GetMapping("/forgot-password/validate-token")
    @RateLimit(type = RateLimitType.IP, time = 60, value = 5, message = "令牌验证请求过于频繁，请稍后重试")
    public Result<Boolean> validateResetToken(@RequestParam String token) {
        try {
            boolean valid = authService.validateResetToken(token);
            return Result.success(valid);
        } catch (Exception e) {
            return Result.success(false);
        }
    }

    @Operation(summary = "重置用户登录失败计数（管理员运维接口）")
    @PostMapping("/reset-fail-count/{username}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'admin')")
    public Result<String> resetLoginFailCount(@PathVariable String username) {
        try {
            authService.resetLoginFailCount(username);
            return Result.success("登录失败计数已重置");
        } catch (BusinessException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            logger.error("重置登录失败计数失败: username={}", username, e);
            return Result.error("重置失败计数失败");
        }
    }

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId(Principal principal) {
        if (principal == null) {
            return null;
        }
        try {
            if (principal instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken) {
                Object principalObj = ((org.springframework.security.authentication.UsernamePasswordAuthenticationToken) principal).getPrincipal();
                if (principalObj instanceof com.foodtraceability.security.model.SecurityUser) {
                    String userId = ((com.foodtraceability.security.model.SecurityUser) principalObj).getUserId();
                    return Long.parseLong(userId);
                }
            }
            return Long.parseLong(principal.getName());
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 提取设备信息
     */
    private String extractDeviceInfo(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.isEmpty()) {
            return "Unknown Device";
        }
        
        StringBuilder deviceInfo = new StringBuilder();
        
        if (userAgent.contains("Mobile") || userAgent.contains("Android") || userAgent.contains("iPhone")) {
            deviceInfo.append("Mobile/");
        } else {
            deviceInfo.append("Desktop/");
        }
        
        if (userAgent.contains("Chrome")) {
            deviceInfo.append("Chrome");
        } else if (userAgent.contains("Firefox")) {
            deviceInfo.append("Firefox");
        } else if (userAgent.contains("Safari")) {
            deviceInfo.append("Safari");
        } else if (userAgent.contains("Edge")) {
            deviceInfo.append("Edge");
        } else {
            deviceInfo.append("Other");
        }
        
        if (userAgent.contains("Windows")) {
            deviceInfo.append("/Windows");
        } else if (userAgent.contains("Mac")) {
            deviceInfo.append("/Mac");
        } else if (userAgent.contains("Linux")) {
            deviceInfo.append("/Linux");
        } else if (userAgent.contains("Android")) {
            deviceInfo.append("/Android");
        } else if (userAgent.contains("iOS") || userAgent.contains("iPhone")) {
            deviceInfo.append("/iOS");
        }
        
        return deviceInfo.toString();
    }
    
    /**
     * 获取客户端真实IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
