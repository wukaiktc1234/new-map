package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * 银行打款记录确认请求DTO
 */
@Schema(description = "银行打款记录确认请求")
public class BankPaymentRecordConfirmDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "确认结果：success-成功 fail-失败")
    @NotBlank(message = "确认结果不能为空")
    private String confirmResult;

    @Schema(description = "确认备注")
    @Size(max = 500, message = "确认备注长度不能超过500位")
    private String confirmRemark;

    public String getConfirmResult() {
        return confirmResult;
    }

    public void setConfirmResult(String confirmResult) {
        this.confirmResult = confirmResult;
    }

    public String getConfirmRemark() {
        return confirmRemark;
    }

    public void setConfirmRemark(String confirmRemark) {
        this.confirmRemark = confirmRemark;
    }
}
