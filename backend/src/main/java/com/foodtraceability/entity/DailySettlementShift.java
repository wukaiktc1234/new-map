package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 班次明细子表实体
 * 对应数据库表 daily_settlement_shifts
 * 用于记录日结对账中各班次的详细营业数据，关联主表daily_settlements
 */
@TableName("daily_settlement_shifts")
@Schema(description = "班次明细子表实体")
public class DailySettlementShift implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 班次ID（雪花算法生成） */
    @TableId(type = IdType.ASSIGN_ID)
    @TableField("shift_id")
    @Schema(description = "班次ID")
    private String shiftId;

    /** 关联的日结主表ID（外键，级联删除） */
    @TableField("settlement_id")
    @Schema(description = "关联日结主表ID")
    private String settlementId;

    /** 班次类型（morning=早班 evening=晚班 overnight=夜班） */
    @TableField("shift_type")
    @Schema(description = "班次类型：morning=早班 evening=晚班 overnight=夜班")
    private String shiftType;

    /** 班次开始时间 */
    @TableField("start_time")
    @Schema(description = "班次开始时间")
    private LocalTime startTime;

    /** 班次结束时间 */
    @TableField("end_time")
    @Schema(description = "班次结束时间")
    private LocalTime endTime;

    /** 班次营业收入（单位：分） */
    @TableField("revenue")
    @Schema(description = "班次营业收入（单位：分）")
    private Long revenue;

    /** 班次订单数量 */
    @TableField("order_count")
    @Schema(description = "班次订单数量")
    private Integer orderCount;

    /** 收银员ID */
    @TableField("cashier_id")
    @Schema(description = "收银员ID")
    private String cashierId;

    /** 收银员姓名 */
    @TableField("cashier_name")
    @Schema(description = "收银员姓名")
    private String cashierName;

    /** 支付方式明细（JSONB格式，同DailySettlement结构） */
    @TableField("payment_breakdown")
    @Schema(description = "该班次支付方式明细JSONB")
    private String paymentBreakdown;

    /** 乐观锁版本号 */
    @Version
    @TableField("version")
    @Schema(description = "乐观锁版本号")
    private Long version;

    /** 逻辑删除标记（0=未删除 1=已删除） */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记")
    private Integer deleted;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    // ==================== Getter & Setter 方法 ====================

    public String getShiftId() {
        return shiftId;
    }

    public void setShiftId(String shiftId) {
        this.shiftId = shiftId;
    }

    public String getSettlementId() {
        return settlementId;
    }

    public void setSettlementId(String settlementId) {
        this.settlementId = settlementId;
    }

    public String getShiftType() {
        return shiftType;
    }

    public void setShiftType(String shiftType) {
        this.shiftType = shiftType;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public Long getRevenue() {
        return revenue;
    }

    public void setRevenue(Long revenue) {
        this.revenue = revenue;
    }

    public Integer getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Integer orderCount) {
        this.orderCount = orderCount;
    }

    public String getCashierId() {
        return cashierId;
    }

    public void setCashierId(String cashierId) {
        this.cashierId = cashierId;
    }

    public String getCashierName() {
        return cashierName;
    }

    public void setCashierName(String cashierName) {
        this.cashierName = cashierName;
    }

    public String getPaymentBreakdown() {
        return paymentBreakdown;
    }

    public void setPaymentBreakdown(String paymentBreakdown) {
        this.paymentBreakdown = paymentBreakdown;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
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
}
