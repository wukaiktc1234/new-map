package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 收货确认单签名请求
 */
@Schema(description = "收货确认单签名请求")
public class ReceiptSignatureDTO {

    @NotBlank(message = "签名方类型不能为空")
    @Schema(description = "签名方（RECEIVER-收货方, SUPPLIER-供应商/司机）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String signerType;

    @Schema(description = "签名人姓名（收货方由后端取当前用户，供应商/司机必填）")
    private String signerName;

    @NotBlank(message = "签名图片不能为空")
    @Size(max = 1000000, message = "签名图片过大")
    @Schema(description = "签名图片（base64 PNG）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String signatureImage;

    public String getSignerType() {
        return signerType;
    }

    public void setSignerType(String signerType) {
        this.signerType = signerType;
    }

    public String getSignerName() {
        return signerName;
    }

    public void setSignerName(String signerName) {
        this.signerName = signerName;
    }

    public String getSignatureImage() {
        return signatureImage;
    }

    public void setSignatureImage(String signatureImage) {
        this.signatureImage = signatureImage;
    }
}
