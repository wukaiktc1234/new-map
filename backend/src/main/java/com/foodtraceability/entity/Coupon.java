package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券实体类
 */
@TableName("coupon")
public class Coupon {

    /** 优惠券ID */
    @TableId(type = IdType.INPUT)
    @TableField("coupon_id")
    private String couponId;

    /** 优惠券编码 */
    @TableField("code")
    private String code;

    /** 优惠券名称 */
    @TableField("name")
    private String name;

    /** 折扣类型：fixed-固定金额, percent-百分比 */
    @TableField("discount_type")
    private String discountType;

    /** 折扣值 */
    @TableField("discount_value")
    private BigDecimal discountValue;

    /** 最低订单金额 */
    @TableField("min_order_amount")
    private BigDecimal minOrderAmount;

    /** 最大折扣金额 */
    @TableField("max_discount")
    private BigDecimal maxDiscount;

    /** 生效开始时间 */
    @TableField("valid_from")
    private LocalDateTime validFrom;

    /** 生效结束时间 */
    @TableField("valid_to")
    private LocalDateTime validTo;

    /** 使用次数限制，-1表示不限 */
    @TableField("usage_limit")
    private Integer usageLimit;

    /** 已使用次数 */
    @TableField("used_count")
    private Integer usedCount;

    /** 状态：active-有效, inactive-无效 */
    @TableField("status")
    private String status;

    /** 门店ID */
    @TableField("store_id")
    private Long storeId;

    /** 门店名称 */
    @TableField("store_name")
    private String storeName;

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

    public Coupon() {
    }

    public String getCouponId() {
        return couponId;
    }

    public void setCouponId(String couponId) {
        this.couponId = couponId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    public BigDecimal getMinOrderAmount() {
        return minOrderAmount;
    }

    public void setMinOrderAmount(BigDecimal minOrderAmount) {
        this.minOrderAmount = minOrderAmount;
    }

    public BigDecimal getMaxDiscount() {
        return maxDiscount;
    }

    public void setMaxDiscount(BigDecimal maxDiscount) {
        this.maxDiscount = maxDiscount;
    }

    public LocalDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDateTime getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDateTime validTo) {
        this.validTo = validTo;
    }

    public Integer getUsageLimit() {
        return usageLimit;
    }

    public void setUsageLimit(Integer usageLimit) {
        this.usageLimit = usageLimit;
    }

    public Integer getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(Integer usedCount) {
        this.usedCount = usedCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
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
