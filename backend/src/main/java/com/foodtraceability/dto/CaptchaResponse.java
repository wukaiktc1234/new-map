package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 验证码响应DTO
 */
@Schema(description = "验证码响应")
public class CaptchaResponse {
    @Schema(description = "验证码ID", example = "1234567890")
    private String captchaId;
    @Schema(description = "验证码图片Base64字符串")
    private String captchaImage;

    public void setCaptchaId(String captchaId) {
        this.captchaId = captchaId;
    }

    public void setCaptchaImage(String captchaImage) {
        this.captchaImage = captchaImage;
    }

    public CaptchaResponse() {
    }

    public String getCaptchaId() {
        return this.captchaId;
    }

    public String getCaptchaImage() {
        return this.captchaImage;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof CaptchaResponse)) return false;
        final CaptchaResponse other = (CaptchaResponse) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$captchaId = this.getCaptchaId();
        final java.lang.Object other$captchaId = other.getCaptchaId();
        if (this$captchaId == null ? other$captchaId != null : !this$captchaId.equals(other$captchaId)) return false;
        final java.lang.Object this$captchaImage = this.getCaptchaImage();
        final java.lang.Object other$captchaImage = other.getCaptchaImage();
        if (this$captchaImage == null ? other$captchaImage != null : !this$captchaImage.equals(other$captchaImage)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof CaptchaResponse;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $captchaId = this.getCaptchaId();
        result = result * PRIME + ($captchaId == null ? 43 : $captchaId.hashCode());
        final java.lang.Object $captchaImage = this.getCaptchaImage();
        result = result * PRIME + ($captchaImage == null ? 43 : $captchaImage.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "CaptchaResponse(captchaId=" + this.getCaptchaId() + ", captchaImage=" + this.getCaptchaImage() + ")";
    }
}
