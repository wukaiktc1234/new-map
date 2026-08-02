package com.foodtraceability.dto.schedule;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

/**
 * 换班申请创建DTO
 */
@Schema(description = "换班申请创建请求")
public class SwapRequestCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 目标计划ID */
    @Schema(description = "排班方案ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "排班方案ID不能为空")
    private String planId;

    /** 发起人要换出的条目ID */
    @Schema(description = "发起人换出的排班条目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "发起人排班条目ID不能为空")
    private String initiatorEntryId;

    /** 目标员工ID */
    @Schema(description = "换班目标员工ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "目标员工ID不能为空")
    private String targetEmployeeId;

    /** 目标员工的条目ID */
    @Schema(description = "目标员工的排班条目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "目标员工排班条目ID不能为空")
    private String targetEntryId;

    /** 换班原因（可选） */
    @Schema(description = "换班原因")
    private String reason;

    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }

    public String getInitiatorEntryId() { return initiatorEntryId; }
    public void setInitiatorEntryId(String initiatorEntryId) { this.initiatorEntryId = initiatorEntryId; }

    public String getTargetEmployeeId() { return targetEmployeeId; }
    public void setTargetEmployeeId(String targetEmployeeId) { this.targetEmployeeId = targetEmployeeId; }

    public String getTargetEntryId() { return targetEntryId; }
    public void setTargetEntryId(String targetEntryId) { this.targetEntryId = targetEntryId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
