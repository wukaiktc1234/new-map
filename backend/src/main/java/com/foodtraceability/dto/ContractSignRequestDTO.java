package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 合同签署请求DTO（带签名数据）
 *
 * <p>用于多方签署流程中，签署方提交签名数据、验证码等信息完成签署。</p>
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-03-20
 */
@Schema(description = "合同签署请求DTO")
public class ContractSignRequestDTO {

    @NotBlank(message = "签署人类型不能为空")
    @Pattern(regexp = "^(employee|company)$", message = "签署人类型只能为 employee 或 company")
    @Schema(description = "签署人类型: employee-员工, company-公司", example = "employee", required = true)
    private String signerType;

    @NotBlank(message = "签名数据不能为空")
    @Schema(description = "签名数据（Base64）", example = "data:image/png;base64,iVBORw0KG...", required = true)
    private String signatureData;

    @NotBlank(message = "验证码不能为空")
    @Schema(description = "验证码", example = "123456", required = true)
    private String verifyCode;

    @Schema(description = "签署IP地址", example = "192.168.1.100")
    private String signIp;

    @Schema(description = "签署设备信息", example = "Chrome/Windows 10")
    private String signDevice;

    public ContractSignRequestDTO() {
    }

    public String getSignerType() {
        return signerType;
    }

    public void setSignerType(String signerType) {
        this.signerType = signerType;
    }

    public String getSignatureData() {
        return signatureData;
    }

    public void setSignatureData(String signatureData) {
        this.signatureData = signatureData;
    }

    public String getVerifyCode() {
        return verifyCode;
    }

    public void setVerifyCode(String verifyCode) {
        this.verifyCode = verifyCode;
    }

    public String getSignIp() {
        return signIp;
    }

    public void setSignIp(String signIp) {
        this.signIp = signIp;
    }

    public String getSignDevice() {
        return signDevice;
    }

    public void setSignDevice(String signDevice) {
        this.signDevice = signDevice;
    }
}
