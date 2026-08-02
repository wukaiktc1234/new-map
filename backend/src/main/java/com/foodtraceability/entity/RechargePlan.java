package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 充值方案实体类
 * 定义不同的充值档位和赠送规则
 */
@TableName("recharge_plan")
public class RechargePlan implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 方案ID（自增主键） */
    @TableId(type = IdType.AUTO)
    @TableField("plan_id")
    private Long planId;

    /** 方案名称 */
    @TableField("plan_name")
    private String planName;

    /** 充值金额（分） */
    @TableField("recharge_amount")
    private Long rechargeAmount;

    /** 赠送金额（分） */
    @TableField("bonus_amount")
    private Long bonusAmount;

    /**
     * 赠送类型
     * 1赠送余额 2赠送积分 3两者都有
     */
    @TableField("bonus_type")
    private Integer bonusType;

    /** 赠送积分数 */
    @TableField("bonus_points")
    private Integer bonusPoints;

    /** 有效期天数（-1表示永久有效） */
    @TableField("validity_days")
    private Integer validityDays;

    /** 是否推荐 */
    @TableField("is_recommended")
    private Boolean isRecommended;

    /** 排序序号 */
    @TableField("sort_order")
    private Integer sortOrder;

    /** 状态：1启用 0停用 */
    @TableField("status")
    private Integer status;

    /** 方案描述 */
    @TableField("description")
    private String description;

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

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public Long getRechargeAmount() {
        return rechargeAmount;
    }

    public void setRechargeAmount(Long rechargeAmount) {
        this.rechargeAmount = rechargeAmount;
    }

    public Long getBonusAmount() {
        return bonusAmount;
    }

    public void setBonusAmount(Long bonusAmount) {
        this.bonusAmount = bonusAmount;
    }

    public Integer getBonusType() {
        return bonusType;
    }

    public void setBonusType(Integer bonusType) {
        this.bonusType = bonusType;
    }

    public Integer getBonusPoints() {
        return bonusPoints;
    }

    public void setBonusPoints(Integer bonusPoints) {
        this.bonusPoints = bonusPoints;
    }

    public Integer getValidityDays() {
        return validityDays;
    }

    public void setValidityDays(Integer validityDays) {
        this.validityDays = validityDays;
    }

    public Boolean getIsRecommended() {
        return isRecommended;
    }

    public void setIsRecommended(Boolean isRecommended) {
        this.isRecommended = isRecommended;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
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
