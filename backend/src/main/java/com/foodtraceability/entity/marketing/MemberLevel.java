package com.foodtraceability.entity.marketing;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员等级实体类（新版，字符串主键）
 * 等级编码完全自由化，只升不降
 *
 * 状态（status）：active-启用 inactive-停用
 */
@TableName("member_levels")
public class MemberLevel implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 等级ID */
    @TableId(value = "level_id", type = IdType.ASSIGN_ID)
    private String levelId;

    /** 等级名称 */
    @TableField("level_name")
    private String levelName;

    /** 等级编码 */
    @TableField("level_code")
    private String levelCode;

    /** 等级颜色（CSS变量值或色值） */
    @TableField("level_color")
    private String levelColor;

    /** 等级图标标识 */
    @TableField("level_icon")
    private String levelIcon;

    /** 升级所需最低累计消费（分） */
    @TableField("min_consumption")
    private Long minConsumption;

    /** 折扣率（1.0=无折扣） */
    @TableField("discount_rate")
    private BigDecimal discountRate;

    /** 积分倍率（1.0=标准） */
    @TableField("points_rate")
    private BigDecimal pointsRate;

    /** 生日折扣率 */
    @TableField("birthday_discount_rate")
    private BigDecimal birthdayDiscountRate;

    /** 生日额外赠送积分 */
    @TableField("birthday_bonus_points")
    private Integer birthdayBonusPoints;

    /** 等级权益说明 */
    @TableField("benefits_description")
    private String benefitsDescription;

    /** 排序序号 */
    @TableField("sort_order")
    private Integer sortOrder;

    /** 状态 */
    @TableField("status")
    private String status;

    /** 当前等级会员数 */
    @TableField("member_count")
    private Integer memberCount;

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

    // ==================== Getter & Setter ====================

    public String getLevelId() { return levelId; }
    public void setLevelId(String levelId) { this.levelId = levelId; }

    public String getLevelName() { return levelName; }
    public void setLevelName(String levelName) { this.levelName = levelName; }

    public String getLevelCode() { return levelCode; }
    public void setLevelCode(String levelCode) { this.levelCode = levelCode; }

    public String getLevelColor() { return levelColor; }
    public void setLevelColor(String levelColor) { this.levelColor = levelColor; }

    public String getLevelIcon() { return levelIcon; }
    public void setLevelIcon(String levelIcon) { this.levelIcon = levelIcon; }

    public Long getMinConsumption() { return minConsumption; }
    public void setMinConsumption(Long minConsumption) { this.minConsumption = minConsumption; }

    public BigDecimal getDiscountRate() { return discountRate; }
    public void setDiscountRate(BigDecimal discountRate) { this.discountRate = discountRate; }

    public BigDecimal getPointsRate() { return pointsRate; }
    public void setPointsRate(BigDecimal pointsRate) { this.pointsRate = pointsRate; }

    public BigDecimal getBirthdayDiscountRate() { return birthdayDiscountRate; }
    public void setBirthdayDiscountRate(BigDecimal birthdayDiscountRate) { this.birthdayDiscountRate = birthdayDiscountRate; }

    public Integer getBirthdayBonusPoints() { return birthdayBonusPoints; }
    public void setBirthdayBonusPoints(Integer birthdayBonusPoints) { this.birthdayBonusPoints = birthdayBonusPoints; }

    public String getBenefitsDescription() { return benefitsDescription; }
    public void setBenefitsDescription(String benefitsDescription) { this.benefitsDescription = benefitsDescription; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getMemberCount() { return memberCount; }
    public void setMemberCount(Integer memberCount) { this.memberCount = memberCount; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
