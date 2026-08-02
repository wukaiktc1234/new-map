package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 促销活动实体类
 * 管理各类营销促销活动，包括满减、折扣、买赠等
 */
@TableName("marketing_promotion")
public class MarketingPromotion implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 活动ID（自增主键） */
    @TableId(type = IdType.AUTO)
    @TableField("promotion_id")
    private Long promotionId;

    /** 活动名称 */
    @TableField("promotion_name")
    private String promotionName;

    /** 活动编码（唯一） */
    @TableField("promotion_code")
    private String promotionCode;

    /**
     * 活动类型
     * 1满减 2折扣 3买赠 4第二件半价 5限时秒杀 6会员日双倍 7新人专享
     */
    @TableField("promotion_type")
    private Integer promotionType;

    /** 优惠规则JSON（复杂条件表达式） */
    @TableField("discount_rule")
    private String discountRule;

    /** 活动开始时间 */
    @TableField("start_time")
    private LocalDateTime startTime;

    /** 活动结束时间 */
    @TableField("end_time")
    private LocalDateTime endTime;

    /**
     * 目标人群
     * 1全部会员 2指定等级 3新会员 4沉睡唤醒 5高价值客户
     */
    @TableField("target_audience")
    private Integer targetAudience;

    /** 目标等级ID列表JSON数组 */
    @TableField("target_level_ids")
    private String targetLevelIds;

    /** 参与条件JSON（最低消费、首次消费等） */
    @TableField("participation_condition")
    private String participationCondition;

    /** 活动预算总额（分） */
    @TableField("budget_total")
    private Long budgetTotal;

    /** 已使用预算（分） */
    @TableField("budget_used")
    private Long budgetUsed;

    /** 参与人数 */
    @TableField("participant_count")
    private Integer participantCount;

    /** 成功成交笔数 */
    @TableField("success_count")
    private Integer successCount;

    /** 活动销售额（分） */
    @TableField("sales_amount")
    private Long salesAmount;

    /** 优惠总额（分） */
    @TableField("discount_amount")
    private Long discountAmount;

    /**
     * 状态
     * 1草稿 2进行中 3已结束 4已暂停 5已作废
     */
    @TableField("status")
    private Integer status;

    /** 活动描述 */
    @TableField("description")
    private String description;

    /** 创建人ID */
    @TableField("create_user_id")
    private Long createUserId;

    /** 创建时间 */
    @TableField("create_time")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField("update_time")
    private LocalDateTime updateTime;

    /** 逻辑删除标记：0未删除，1已删除 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    // ==================== Getter & Setter ====================

    public Long getPromotionId() {
        return promotionId;
    }

    public void setPromotionId(Long promotionId) {
        this.promotionId = promotionId;
    }

    public String getPromotionName() {
        return promotionName;
    }

    public void setPromotionName(String promotionName) {
        this.promotionName = promotionName;
    }

    public String getPromotionCode() {
        return promotionCode;
    }

    public void setPromotionCode(String promotionCode) {
        this.promotionCode = promotionCode;
    }

    public Integer getPromotionType() {
        return promotionType;
    }

    public void setPromotionType(Integer promotionType) {
        this.promotionType = promotionType;
    }

    public String getDiscountRule() {
        return discountRule;
    }

    public void setDiscountRule(String discountRule) {
        this.discountRule = discountRule;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Integer getTargetAudience() {
        return targetAudience;
    }

    public void setTargetAudience(Integer targetAudience) {
        this.targetAudience = targetAudience;
    }

    public String getTargetLevelIds() {
        return targetLevelIds;
    }

    public void setTargetLevelIds(String targetLevelIds) {
        this.targetLevelIds = targetLevelIds;
    }

    public String getParticipationCondition() {
        return participationCondition;
    }

    public void setParticipationCondition(String participationCondition) {
        this.participationCondition = participationCondition;
    }

    public Long getBudgetTotal() {
        return budgetTotal;
    }

    public void setBudgetTotal(Long budgetTotal) {
        this.budgetTotal = budgetTotal;
    }

    public Long getBudgetUsed() {
        return budgetUsed;
    }

    public void setBudgetUsed(Long budgetUsed) {
        this.budgetUsed = budgetUsed;
    }

    public Integer getParticipantCount() {
        return participantCount;
    }

    public void setParticipantCount(Integer participantCount) {
        this.participantCount = participantCount;
    }

    public Integer getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Integer successCount) {
        this.successCount = successCount;
    }

    public Long getSalesAmount() {
        return salesAmount;
    }

    public void setSalesAmount(Long salesAmount) {
        this.salesAmount = salesAmount;
    }

    public Long getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(Long discountAmount) {
        this.discountAmount = discountAmount;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(Long createUserId) {
        this.createUserId = createUserId;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
