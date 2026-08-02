package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * 付款单作废请求DTO
 */
@Schema(description = "付款单作废请求")
public class PaymentVoidDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "作废备注")
    @Size(max = 500, message = "作废备注长度不能超过500位")
    private String remark;

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
