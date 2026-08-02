package com.foodtraceability.dto.marketing;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 促销活动更新DTO
 */
public class PromotionUpdateDTO {

    private String promotionName;

    /** 优惠规则JSON（复杂条件表达式） */
    private String discountRule;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /** 1全部会员 2指定等级 3新会员 4沉睡唤醒 5高价值客户 */
    private Integer targetAudience;

    /** 目标等级ID列表JSON数组 */
    private String targetLevelIds;

    /** 参与条件JSON */
    private String participationCondition;

    /** 活动预算总额（分） */
    private Long budgetTotal;

    /**
     * 状态
     * 1草稿 2进行中 3已结束 4已暂停 5已作废
     */
    @NotNull(message = "状态不能为空")
    private Integer status;

    /** 活动描述 */
    private String description;

    // Getter & Setter
    public String getPromotionName() { return promotionName; }
    public void setPromotionName(String promotionName) { this.promotionName = promotionName; }
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
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
