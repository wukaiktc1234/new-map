package com.foodtraceability.dto.marketing;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 充值方案查询DTO
 * 充值方案为列表查询（不分页），仅支持过滤条件
 */
@Schema(description = "充值方案查询请求")
public class RechargePlanQueryDTO {

    @Schema(description = "方案名称（模糊匹配）")
    private String planName;

    @Schema(description = "方案类型: standard/activity/tiered/custom")
    private String planType;

    @Schema(description = "状态: active/inactive")
    private String status;

    // ==================== Getter & Setter ====================

    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }

    public String getPlanType() { return planType; }
    public void setPlanType(String planType) { this.planType = planType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
