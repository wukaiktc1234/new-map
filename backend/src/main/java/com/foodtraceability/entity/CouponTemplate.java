package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 优惠券模板实体类
 * 定义优惠券的规则和属性，用户领取后生成具体的优惠券
 */
@TableName("coupon_template")
public class CouponTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 模板ID（自增主键） */
    @TableId(type = IdType.AUTO)
    @TableField("template_id")
    private Long templateId;

    /** 模板名称 */
    @TableField("template_name")
    private String templateName;

    /** 模板编码（唯一） */
    @TableField("template_code")
    private String templateCode;

    /**
     * 优惠券类型
     * 1满减券 2折扣券 3兑换券 4代金券 5新人券
     */
    @TableField("coupon_type")
    private Integer couponType;

    /**
     * 折扣类型
     * 1固定金额减免 2百分比折扣
     */
    @TableField("discount_type")
    private Integer discountType;

    /**
     * 优惠值
     * - 固定金额时：单位为分（如2000=20元）
     * - 百分比折扣时：乘以100（如9500=95折）
     */
    @TableField("discount_value")
    private Long discountValue;

    /** 最低消费门槛（分） */
    @TableField("min_consumption")
    private Long minConsumption;

    /** 最大优惠金额上限（分），防止无限折扣 */
    @TableField("max_discount")
    private Long maxDiscount;

    /** 发放总量（-1表示不限） */
    @TableField("total_quantity")
    private Integer totalQuantity;

    /** 每人限领数量 */
    @TableField("per_person_limit")
    private Integer perPersonLimit;

    /** 可领取开始时间 */
    @TableField("valid_start_date")
    private LocalDateTime validStartDate;

    /** 领取截止时间 */
    @TableField("valid_expiry_date")
    private LocalDateTime validExpiryDate;

    /** 领取后有效天数 */
    @TableField("valid_days")
    private Integer validDays;

    /**
     * 适用范围
     * 1全场通用 2指定品类 3指定商品 4指定渠道
     */
    @TableField("applicable_scope")
    private Integer applicableScope;

    /** 适用范围配置JSON */
    @TableField("scope_config")
    private String scopeConfig;

    /** 使用规则说明 */
    @TableField("usage_rules")
    private String usageRules;

    /** 状态：1启用 0停用 */
    @TableField("status")
    private Integer status;

    /** 已领取数量 */
    @TableField("created_count")
    private Integer createdCount;

    /** 已使用数量 */
    @TableField("used_count")
    private Integer usedCount;

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

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public Integer getCouponType() {
        return couponType;
    }

    public void setCouponType(Integer couponType) {
        this.couponType = couponType;
    }

    public Integer getDiscountType() {
        return discountType;
    }

    public void setDiscountType(Integer discountType) {
        this.discountType = discountType;
    }

    public Long getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(Long discountValue) {
        this.discountValue = discountValue;
    }

    public Long getMinConsumption() {
        return minConsumption;
    }

    public void setMinConsumption(Long minConsumption) {
        this.minConsumption = minConsumption;
    }

    public Long getMaxDiscount() {
        return maxDiscount;
    }

    public void setMaxDiscount(Long maxDiscount) {
        this.maxDiscount = maxDiscount;
    }

    public Integer getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public Integer getPerPersonLimit() {
        return perPersonLimit;
    }

    public void setPerPersonLimit(Integer perPersonLimit) {
        this.perPersonLimit = perPersonLimit;
    }

    public LocalDateTime getValidStartDate() {
        return validStartDate;
    }

    public void setValidStartDate(LocalDateTime validStartDate) {
        this.validStartDate = validStartDate;
    }

    public LocalDateTime getValidExpiryDate() {
        return validExpiryDate;
    }

    public void setValidExpiryDate(LocalDateTime validExpiryDate) {
        this.validExpiryDate = validExpiryDate;
    }

    public Integer getValidDays() {
        return validDays;
    }

    public void setValidDays(Integer validDays) {
        this.validDays = validDays;
    }

    public Integer getApplicableScope() {
        return applicableScope;
    }

    public void setApplicableScope(Integer applicableScope) {
        this.applicableScope = applicableScope;
    }

    public String getScopeConfig() {
        return scopeConfig;
    }

    public void setScopeConfig(String scopeConfig) {
        this.scopeConfig = scopeConfig;
    }

    public String getUsageRules() {
        return usageRules;
    }

    public void setUsageRules(String usageRules) {
        this.usageRules = usageRules;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getCreatedCount() {
        return createdCount;
    }

    public void setCreatedCount(Integer createdCount) {
        this.createdCount = createdCount;
    }

    public Integer getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(Integer usedCount) {
        this.usedCount = usedCount;
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
