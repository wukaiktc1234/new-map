package com.foodtraceability.dto;

import com.foodtraceability.dto.UserInfoDTO;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 登录响应DTO
 */
@Schema(description = "登录响应")
public class LoginResponse {

    @Schema(description = "JWT令牌", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    @Schema(description = "刷新令牌", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;

    @Schema(description = "用户信息")
    private UserInfoDTO userInfo;

    @Schema(description = "是否需要验证码", example = "false")
    private Boolean requireCaptcha;

    @Schema(description = "验证码ID（需要验证码时返回）", example = "uuid-string")
    private String captchaId;

    @Schema(description = "验证码图片Base64（需要验证码时返回）")
    private String captchaImage;

    @Schema(description = "登录失败次数", example = "2")
    private Integer failedAttempts;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public UserInfoDTO getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserInfoDTO userInfo) {
        this.userInfo = userInfo;
    }

    public Boolean getRequireCaptcha() {
        return requireCaptcha;
    }

    public void setRequireCaptcha(Boolean requireCaptcha) {
        this.requireCaptcha = requireCaptcha;
    }

    public String getCaptchaId() {
        return captchaId;
    }

    public void setCaptchaId(String captchaId) {
        this.captchaId = captchaId;
    }

    public String getCaptchaImage() {
        return captchaImage;
    }

    public void setCaptchaImage(String captchaImage) {
        this.captchaImage = captchaImage;
    }

    public Integer getFailedAttempts() {
        return failedAttempts;
    }

    public void setFailedAttempts(Integer failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    /**
     * 登录成功判断方法
     */
    public boolean isSuccess() {
        return token != null && !token.isEmpty();
    }
}
