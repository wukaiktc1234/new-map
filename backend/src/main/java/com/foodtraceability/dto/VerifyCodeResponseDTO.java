package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 验证码响应DTO
 *
 * <p>用于返回签署流程中生成的验证码及其过期时间。
 * 实际项目中验证码应通过短信/邮件发送，此处仅用于开发调试。</p>
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-03-20
 */
@Schema(description = "验证码响应DTO")
public class VerifyCodeResponseDTO {

    @Schema(description = "验证码", example = "123456")
    private String verifyCode;

    @Schema(description = "过期时间", example = "2026-03-20T15:30:00")
    private LocalDateTime expireTime;

    public VerifyCodeResponseDTO() {
    }

    public VerifyCodeResponseDTO(String verifyCode, LocalDateTime expireTime) {
        this.verifyCode = verifyCode;
        this.expireTime = expireTime;
    }

    public String getVerifyCode() {
        return verifyCode;
    }

    public void setVerifyCode(String verifyCode) {
        this.verifyCode = verifyCode;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }
}
