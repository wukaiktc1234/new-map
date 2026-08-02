package com.foodtraceability.dto.marketing;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 充值方案VO
 * 金额字段以元为单位（字符串），与前端契约一致
 */
@Schema(description = "充值方案信息")
public class RechargePlanVO {

    @Schema(description = "方案ID")
    private String planId;

    @Schema(description = "方案名称")
    private String planName;

    @Schema(description = "方案类型")
    private String planType;

    @Schema(description = "充值金额（元）")
    private String rechargeAmount;

    @Schema(description = "赠送金额（元）")
    private String bonusAmount;

    @Schema(description = "赠送比例（百分比）")
    private Integer bonusRate;

    @Schema(description = "赠送积分")
    private Integer bonusPoints;

    @Schema(description = "赠送类型")
    private String bonusType;

    @Schema(description = "赠送有效期天数")
    private Integer validityDays;

    @Schema(description = "是否推荐")
    private Boolean isRecommended;

    @Schema(description = "排序序号")
    private Integer sortOrder;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "方案描述")
    private String description;

    @Schema(description = "关联促销活动ID")
    private String promotionId;

    @Schema(description = "方案生效时间")
    private String startTime;

    @Schema(description = "方案失效时间")
    private String endTime;

    @Schema(description = "目标人群")
    private String targetAudience;

    @Schema(description = "创建时间")
    private String createdAt;

    @Schema(description = "更新时间")
    private String updatedAt;

    // ==================== Getter & Setter ====================

    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }

    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }

    public String getPlanType() { return planType; }
    public void setPlanType(String planType) { this.planType = planType; }

    public String getRechargeAmount() { return rechargeAmount; }
    public void setRechargeAmount(String rechargeAmount) { this.rechargeAmount = rechargeAmount; }

    public String getBonusAmount() { return bonusAmount; }
    public void setBonusAmount(String bonusAmount) { this.bonusAmount = bonusAmount; }

    public Integer getBonusRate() { return bonusRate; }
    public void setBonusRate(Integer bonusRate) { this.bonusRate = bonusRate; }

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

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
