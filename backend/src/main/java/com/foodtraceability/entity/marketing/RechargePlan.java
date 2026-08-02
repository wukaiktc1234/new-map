package com.foodtraceability.entity.marketing;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 充值方案实体类
 * 定义不同的充值档位、赠送规则与活动方案
 *
 * 方案类型（plan_type）：
 * - standard: 标准方案
 * - activity: 活动方案（限时）
 * - tiered: 阶梯方案
 * - custom: 自定义方案
 *
 * 状态（status）：active-启用 inactive-停用
 */
@TableName("recharge_plans")
public class RechargePlan implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 方案ID */
    @TableId(value = "plan_id", type = IdType.ASSIGN_ID)
    private String planId;

    /** 方案名称 */
    @TableField("plan_name")
    private String planName;

    /** 方案类型 */
    @TableField("plan_type")
    private String planType;

    /** 充值金额（分） */
    @TableField("recharge_amount")
    private Long rechargeAmount;

    /** 赠送金额（分） */
    @TableField("bonus_amount")
    private Long bonusAmount;

    /** 赠送比例（百分比） */
    @TableField("bonus_rate")
    private Integer bonusRate;

    /** 赠送积分 */
    @TableField("bonus_points")
    private Integer bonusPoints;

    /** 赠送类型 */
    @TableField("bonus_type")
    private String bonusType;

    /** 赠送有效期天数（-1永久，0跟随系统默认） */
    @TableField("validity_days")
    private Integer validityDays;

    /** 是否推荐 */
    @TableField("is_recommended")
    private Boolean isRecommended;

    /** 排序序号 */
    @TableField("sort_order")
    private Integer sortOrder;

    /** 状态 */
    @TableField("status")
    private String status;

    /** 方案描述 */
    @TableField("description")
    private String description;

    /** 关联促销活动ID（活动方案时填写） */
    @TableField("promotion_id")
    private String promotionId;

    /** 方案生效时间（活动方案时填写） */
    @TableField("start_time")
    private LocalDateTime startTime;

    /** 方案失效时间（活动方案时填写） */
    @TableField("end_time")
    private LocalDateTime endTime;

    /** 目标人群：all/new_member/specified_level */
    @TableField("target_audience")
    private String targetAudience;

    /** 额外赠送配置（JSON字符串） */
    @TableField("extra_bonus")
    private String extraBonus;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    // ==================== 状态常量 ====================
    /** 状态：启用 */
    public static final String STATUS_ACTIVE = "active";
    /** 状态：停用 */
    public static final String STATUS_INACTIVE = "inactive";
    /** 方案类型：标准 */
    public static final String TYPE_STANDARD = "standard";
    /** 方案类型：活动 */
    public static final String TYPE_ACTIVITY = "activity";
    /** 赠送类型：余额 */
    public static final String BONUS_BALANCE = "balance";
    /** 赠送类型：券 */
    public static final String BONUS_COUPON = "coupon";
    /** 赠送类型：积分 */
    public static final String BONUS_POINTS = "points";
    /** 赠送类型：混合 */
    public static final String BONUS_MIXED = "mixed";

    // ==================== Getter & Setter ====================

    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }

    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }

    public String getPlanType() { return planType; }
    public void setPlanType(String planType) { this.planType = planType; }

    public Long getRechargeAmount() { return rechargeAmount; }
    public void setRechargeAmount(Long rechargeAmount) { this.rechargeAmount = rechargeAmount; }

    public Long getBonusAmount() { return bonusAmount; }
    public void setBonusAmount(Long bonusAmount) { this.bonusAmount = bonusAmount; }

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

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getTargetAudience() { return targetAudience; }
    public void setTargetAudience(String targetAudience) { this.targetAudience = targetAudience; }

    public String getExtraBonus() { return extraBonus; }
    public void setExtraBonus(String extraBonus) { this.extraBonus = extraBonus; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
