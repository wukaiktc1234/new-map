package com.foodtraceability.dto.marketing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 促销活动创建DTO
 */
public class PromotionCreateDTO {

    @NotBlank(message = "活动名称不能为空")
    private String promotionName;

    @NotBlank(message = "活动编码不能为空")
    private String promotionCode;

    /** 1满减 2折扣 3买赠 4第二件半价 5限时秒杀 6会员日双倍 7新人专享 */
    @NotNull(message = "活动类型不能为空")
    private Integer promotionType;

    /** 优惠规则JSON（复杂条件表达式） */
    @NotBlank(message = "优惠规则不能为空")
    private String discountRule;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    /** 1全部会员 2指定等级 3新会员 4沉睡唤醒 5高价值客户 */
    private Integer targetAudience = 1;

    /** 目标等级ID列表JSON数组 */
    private String targetLevelIds;

    /** 参与条件JSON */
    private String participationCondition;

    /** 活动预算总额（分） */
    private Long budgetTotal;

    /** 活动描述 */
    private String description;

    // Getter & Setter
    public String getPromotionName() { return promotionName; }
    public void setPromotionName(String promotionName) { this.promotionName = promotionName; }
    public String getPromotionCode() { return promotionCode; }
    public void setPromotionCode(String promotionCode) { this.promotionCode = promotionCode; }
    public Integer getPromotionType() { return promotionType; }
    public void setPromotionType(Integer promotionType) { this.promotionType = promotionType; }
    public String getDiscountRule() { return discountRule; }
    public void setDiscountRule(String discountRule) { this.discountRule = discountRule; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public Integer getTargetAudience() { return targetAudience; }
    public void setTargetAudience(Integer targetAudience) { this.targetAudience = targetAudience; }
    public String getTargetLevelIds() { return targetLevelIds; }
    public void setTargetLevelIds(String targetLevelIds) { this.targetLevelIds = targetLevelIds; }
    public String getParticipationCondition() { return participationCondition; }
    public void setParticipationCondition(String participationCondition) { this.participationCondition = participationCondition; }
    public Long getBudgetTotal() { return budgetTotal; }
    public void setBudgetTotal(Long budgetTotal) { this.budgetTotal = budgetTotal; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
