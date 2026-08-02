package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 拒绝签署请求DTO
 *
 * <p>用于签署方拒绝签署合同时提交拒绝原因。</p>
 *
 * @author Liberty
 * @version 1.0
 * @since 2026-03-20
 */
@Schema(description = "拒绝签署请求DTO")
public class ContractRejectDTO {

    @NotBlank(message = "签署人类型不能为空")
    @Pattern(regexp = "^(employee|company)$", message = "签署人类型只能为 employee 或 company")
    @Schema(description = "签署人类型: employee-员工, company-公司", example = "employee", required = true)
    private String signerType;

    @NotBlank(message = "拒绝原因不能为空")
    @Size(max = 500, message = "拒绝原因不能超过500个字符")
    @Schema(description = "拒绝原因", example = "对合同薪资条款有异议", required = true)
    private String reason;

    public ContractRejectDTO() {
    }

    public String getSignerType() {
        return signerType;
    }

    public void setSignerType(String signerType) {
        this.signerType = signerType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
