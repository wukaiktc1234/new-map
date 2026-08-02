package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交接班记录实体类
 * 记录门店收银员的交接班信息，包括金额、订单数、差异等
 */
@TableName("shift_records")
public class ShiftRecord {

    /** 交接班记录ID */
    @TableId(type = IdType.AUTO)
    private Long shiftId;

    /** 交接班单号（唯一） */
    private String shiftCode;

    /** 门店ID */
    private Long storeId;

    /**
     * 班次类型
     * 1-早班 2-中班 3-晚班 4-通宵
     */
    private Integer shiftType;

    /** 收银员ID */
    private Long cashierUserId;

    /** 开班时间 */
    private LocalDateTime startTime;

    /** 交班时间 */
    private LocalDateTime endTime;

    /**
     * 开机金额（分）
     * 班次开始时的现金金额
     */
    private BigDecimal openingCash;

    /**
     * 交班金额（分）
     * 班次结束时的现金金额
     */
    private BigDecimal closingCash;

    /**
     * 销售额（分）
     * 本班次的销售总额
     */
    private BigDecimal salesAmount;

    /** 订单数 */
    private Integer orderCount;

    /**
     * 退款额（分）
     * 本班次的退款总额
     */
    private BigDecimal refundAmount;

    /**
     * 差异金额（分）
     * 实际金额与系统金额的差异
     */
    private BigDecimal differenceAmount;

    /** 交接给谁（用户ID） */
    private Long handoverToUserId;

    /** 交接备注 */
    private String handoverRemark;

    /**
     * 交接状态
     * 1-交接中 2-已完成 3-异常
     */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    private Integer deleted;

    // ==================== Getter & Setter 方法 ====================

    public Long getShiftId() {
        return shiftId;
    }

    public void setShiftId(Long shiftId) {
        this.shiftId = shiftId;
    }

    public String getShiftCode() {
        return shiftCode;
    }

    public void setShiftCode(String shiftCode) {
        this.shiftCode = shiftCode;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public Integer getShiftType() {
        return shiftType;
    }

    public void setShiftType(Integer shiftType) {
        this.shiftType = shiftType;
    }

    public Long getCashierUserId() {
        return cashierUserId;
    }

    public void setCashierUserId(Long cashierUserId) {
        this.cashierUserId = cashierUserId;
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

    public BigDecimal getOpeningCash() {
        return openingCash;
    }

    public void setOpeningCash(BigDecimal openingCash) {
        this.openingCash = openingCash;
    }

    public BigDecimal getClosingCash() {
        return closingCash;
    }

    public void setClosingCash(BigDecimal closingCash) {
        this.closingCash = closingCash;
    }

    public BigDecimal getSalesAmount() {
        return salesAmount;
    }

    public void setSalesAmount(BigDecimal salesAmount) {
        this.salesAmount = salesAmount;
    }

    public Integer getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Integer orderCount) {
        this.orderCount = orderCount;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public BigDecimal getDifferenceAmount() {
        return differenceAmount;
    }

    public void setDifferenceAmount(BigDecimal differenceAmount) {
        this.differenceAmount = differenceAmount;
    }

    public Long getHandoverToUserId() {
        return handoverToUserId;
    }

    public void setHandoverToUserId(Long handoverToUserId) {
        this.handoverToUserId = handoverToUserId;
    }

    public String getHandoverRemark() {
        return handoverRemark;
    }

    public void setHandoverRemark(String handoverRemark) {
        this.handoverRemark = handoverRemark;
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
