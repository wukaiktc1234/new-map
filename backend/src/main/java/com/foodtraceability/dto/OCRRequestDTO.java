package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * OCR识别请求DTO
 */
@Schema(description = "OCR识别请求")
public class OCRRequestDTO {
    @NotBlank(message = "图片数据不能为空")
    @Size(max = 50 * 1024 * 1024, message = "图片数据大小不能超过50MB")
    @Schema(description = "Base64编码的图片数据", required = true)
    private String imageBase64;

    public OCRRequestDTO() {
    }

    public String getImageBase64() {
        return this.imageBase64;
    }

    public void setImageBase64(final String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof OCRRequestDTO)) return false;
        final OCRRequestDTO other = (OCRRequestDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$imageBase64 = this.getImageBase64();
        final java.lang.Object other$imageBase64 = other.getImageBase64();
        if (this$imageBase64 == null ? other$imageBase64 != null : !this$imageBase64.equals(other$imageBase64)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof OCRRequestDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $imageBase64 = this.getImageBase64();
        result = result * PRIME + ($imageBase64 == null ? 43 : $imageBase64.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "OCRRequestDTO(imageBase64=" + this.getImageBase64() + ")";
    }
}
