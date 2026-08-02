package com.foodtraceability.dto.schedule;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

/**
 * 自动排班生成请求DTO
 */
@Schema(description = "自动排班生成请求")
public class GenerateScheduleDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 计划ID */
    @Schema(description = "排班方案ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "排班方案ID不能为空")
    private String planId;

    /** 使用的模板ID（可选，不传则使用默认模板） */
    @Schema(description = "排班模板ID（不传则使用默认模板）")
    private String templateId;

    /** 是否覆盖已有排班 */
    @Schema(description = "是否覆盖已有排班")
    private Boolean overwriteExisting = false;

    /** 生效日期（可选，不传则从计划开始日期生效） */
    @Schema(description = "生效日期（不传则从计划开始日期生效）")
    private String effectiveDate;

    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }

    public String getTemplateId() { return templateId; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }

    public Boolean getOverwriteExisting() { return overwriteExisting; }
    public void setOverwriteExisting(Boolean overwriteExisting) { this.overwriteExisting = overwriteExisting; }

    public String getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(String effectiveDate) { this.effectiveDate = effectiveDate; }
}
