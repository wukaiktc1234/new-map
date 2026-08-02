package com.foodtraceability.dto.schedule;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

/**
 * 冲突查询DTO
 */
@Schema(description = "排班冲突查询条件")
public class ConflictQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 方案ID */
    @Schema(description = "排班方案ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "排班方案ID不能为空")
    private String planId;

    /** 冲突级别筛选 */
    @Schema(description = "冲突级别筛选: error/warning/info")
    private String level;

    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
}
