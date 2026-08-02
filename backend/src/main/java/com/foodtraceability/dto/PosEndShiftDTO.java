package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * POS结束班次请求DTO
 * 用于收银端结束当前班次的请求参数
 */
@Schema(description = "POS结束班次请求DTO")
public class PosEndShiftDTO {

    @NotBlank(message = "终端ID不能为空")
    @Schema(description = "终端ID", example = "POS-001")
    private String terminalId;

    @Schema(description = "备注")
    private String remark;

    public PosEndShiftDTO() {
    }

    public String getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
