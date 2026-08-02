package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员等级实体类
 * 用于管理会员等级体系，包括普通会员、银卡、金卡、钻石等级
 */
@TableName("member_level")
public class MemberLevel implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 等级ID（自增主键） */
    @TableId(type = IdType.AUTO)
    @TableField("level_id")
    private Long levelId;

    /** 等级名称：普通会员/银卡/金卡/钻石 */
    @TableField("level_name")
    private String levelName;

    /** 等级编码：NORMAL/SILVER/GOLD/DIAMOND */
    @TableField("level_code")
    private String levelCode;

    /** 升级所需最低积分 */
    @TableField("min_points")
    private Integer minPoints;

    /** 升级所需最低消费金额（分） */
    @TableField("min_consumption")
    private Long minConsumption;

    /** 积分倍率（如1.0/1.2/1.5/2.0） */
    @TableField("points_rate")
    private BigDecimal pointsRate;

    /** 折扣率（1.0=无折扣，0.95=95折） */
    @TableField("discount_rate")
    private BigDecimal discountRate;

    /** 生日额外积分 */
    @TableField("birthday_bonus")
    private Integer birthdayBonus;

    /** 等级权益说明 */
    @TableField("benefits_description")
    private String benefitsDescription;

    /** 等级图标URL */
    @TableField("icon_url")
    private String iconUrl;

    /** 排序序号 */
    @TableField("sort_order")
    private Integer sortOrder;

    /** 状态：1启用 0停用 */
    @TableField("status")
    private Integer status;

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

    public Long getLevelId() {
        return levelId;
    }

    public void setLevelId(Long levelId) {
        this.levelId = levelId;
    }

    public String getLevelName() {
        return levelName;
    }

    public void setLevelName(String levelName) {
        this.levelName = levelName;
    }

    public String getLevelCode() {
        return levelCode;
    }

    public void setLevelCode(String levelCode) {
        this.levelCode = levelCode;
    }

    public Integer getMinPoints() {
        return minPoints;
    }

    public void setMinPoints(Integer minPoints) {
        this.minPoints = minPoints;
    }

    public Long getMinConsumption() {
        return minConsumption;
    }

    public void setMinConsumption(Long minConsumption) {
        this.minConsumption = minConsumption;
    }

    public BigDecimal getPointsRate() {
        return pointsRate;
    }

    public void setPointsRate(BigDecimal pointsRate) {
        this.pointsRate = pointsRate;
    }

    public BigDecimal getDiscountRate() {
        return discountRate;
    }

    public void setDiscountRate(BigDecimal discountRate) {
        this.discountRate = discountRate;
    }

    public Integer getBirthdayBonus() {
        return birthdayBonus;
    }

    public void setBirthdayBonus(Integer birthdayBonus) {
        this.birthdayBonus = birthdayBonus;
    }

    public String getBenefitsDescription() {
        return benefitsDescription;
    }

    public void setBenefitsDescription(String benefitsDescription) {
        this.benefitsDescription = benefitsDescription;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
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
