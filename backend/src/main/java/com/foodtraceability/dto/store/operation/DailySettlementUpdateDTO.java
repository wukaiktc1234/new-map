package com.foodtraceability.dto.store.operation;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 日结对账更新DTO
 * 用于更新对账记录状态，支持确认或标记异常
 */
@Schema(description = "日结对账更新DTO")
public class DailySettlementUpdateDTO {

    /**
     * 结算状态
     * confirmed: 已确认
     * abnormal: 异常
     */
    @NotBlank(message = "结算状态不能为空")
    @Pattern(regexp = "^(confirmed|abnormal)$", message = "结算状态只能是confirmed或abnormal")
    @Schema(description = "结算状态（confirmed=已确认 abnormal=异常）", example = "confirmed", requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;

    /** 审核人ID */
    @Schema(description = "审核人ID", example = "auditor001")
    private String auditorId;

    /** 审核人姓名 */
    @Schema(description = "审核人姓名", example = "李四")
    private String auditorName;

    /** 备注/异常说明 */
    @Size(max = 500, message = "备注长度不能超过500个字符")
    @Schema(description = "备注/异常说明", example = "数据核对无误")
    private String remark;

    // ==================== Getter & Setter ====================

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAuditorId() {
        return auditorId;
    }

    public void setAuditorId(String auditorId) {
        this.auditorId = auditorId;
    }

    public String getAuditorName() {
        return auditorName;
    }

    public void setAuditorName(String auditorName) {
        this.auditorName = auditorName;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
