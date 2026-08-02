package com.foodtraceability.dto.marketing;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 充值方案创建DTO
 * 金额字段以元为单位（字符串），由Service转换为分存储
 */
@Schema(description = "充值方案创建请求")
public class RechargePlanCreateDTO {

    @Schema(description = "方案名称", example = "超值充值")
    @NotBlank(message = "方案名称不能为空")
    private String planName;

    @Schema(description = "方案类型: standard/activity/tiered/custom", example = "standard")
    @NotBlank(message = "方案类型不能为空")
    private String planType;

    @Schema(description = "充值金额（元）", example = "300.00")
    @NotBlank(message = "充值金额不能为空")
    private String rechargeAmount;

    @Schema(description = "赠送金额（元）", example = "30.00")
    private String bonusAmount;

    @Schema(description = "赠送积分", example = "300")
    private Integer bonusPoints;

    @Schema(description = "赠送类型: balance/coupon/points/mixed", example = "balance")
    private String bonusType;

    @Schema(description = "赠送有效期天数（-1永久，0跟随系统默认）", example = "180")
    private Integer validityDays;

    @Schema(description = "是否推荐", example = "false")
    private Boolean isRecommended;

    @Schema(description = "排序序号", example = "1")
    private Integer sortOrder;

    @Schema(description = "状态: active/inactive", example = "active")
    private String status;

    @Schema(description = "方案描述")
    private String description;

    @Schema(description = "关联促销活动ID（活动方案时填写）")
    private String promotionId;

    @Schema(description = "方案生效时间（活动方案，YYYY-MM-DD HH:mm:ss）")
    private String startTime;

    @Schema(description = "方案失效时间（活动方案，YYYY-MM-DD HH:mm:ss）")
    private String endTime;

    @Schema(description = "目标人群: all/new_member/specified_level")
    private String targetAudience;

    // ==================== Getter & Setter ====================

    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }

    public String getPlanType() { return planType; }
    public void setPlanType(String planType) { this.planType = planType; }

    public String getRechargeAmount() { return rechargeAmount; }
    public void setRechargeAmount(String rechargeAmount) { this.rechargeAmount = rechargeAmount; }

    public String getBonusAmount() { return bonusAmount; }
    public void setBonusAmount(String bonusAmount) { this.bonusAmount = bonusAmount; }

    public Integer getBonusPoints() { return bonusPoints; }
    public void setBonusPoints(Integer bonusPoints) { this.bonusPoints = bonusPoints; }

    public String getBonusType() { return bonusType; }
    public void setBonusType(String bonusType) { this.bonusType = bonusType; }

    public Integer getValidityDays() { return validityDays; }
    public void setValidityDays(Integer validityDays) { this.validityDays = validityDays; }

    public Boolean getIsRecommended() { return isRecommended; }
    public void setIsRecommended(Boolean isRecommended) { this.isRecommended = isRecommended; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPromotionId() { return promotionId; }
    public void setPromotionId(String promotionId) { this.promotionId = promotionId; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getTargetAudience() { return targetAudience; }
    public void setTargetAudience(String targetAudience) { this.targetAudience = targetAudience; }
}
