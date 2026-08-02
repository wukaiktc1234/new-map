package com.foodtraceability.dto.store.operation.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalTime;

/**
 * 班次明细视图对象VO
 * 用于返回日结对账中各班次的详细营业数据
 * 作为DailySettlementVO的子对象，展示早/晚/夜班的营业情况
 */
@Schema(description = "班次明细视图对象")
public class DailySettlementShiftVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 班次ID */
    @Schema(description = "班次ID", example = "9876543210987654321")
    private String shiftId;

    /**
     * 班次类型
     * morning: 早班（06:00-14:00）
     * evening: 晚班（14:00-22:00）
     * overnight: 夜班（22:00-06:00）
     */
    @Schema(description = "班次类型：morning=早班 evening=晚班 overnight=夜班", example = "morning")
    private String shiftType;

    /** 班次类型名称 */
    @Schema(description = "班次类型名称", example = "早班")
    private String shiftTypeName;

    /** 班次开始时间 */
    @Schema(description = "班次开始时间", example = "06:00:00")
    private LocalTime startTime;

    /** 班次结束时间 */
    @Schema(description = "班次结束时间", example = "14:00:00")
    private LocalTime endTime;

    /**
     * 班次营业收入（单位：元）
     * 由DataConverter将数据库中的分转换为元
     */
    @Schema(description = "班次营业收入（元）", example = "5800.00")
    private String revenue;

    /** 班次订单数量 */
    @Schema(description = "班次订单数量", example = "58")
    private Integer orderCount;

    /** 收银员ID */
    @Schema(description = "收银员ID", example = "cashier001")
    private String cashierId;

    /** 收银员姓名 */
    @Schema(description = "收银员姓名", example = "王五")
    private String cashierName;

    // ==================== Getter & Setter ====================

    public String getShiftId() { return shiftId; }
    public void setShiftId(String shiftId) { this.shiftId = shiftId; }

    public String getShiftType() { return shiftType; }
    public void setShiftType(String shiftType) { this.shiftType = shiftType; }

    public String getShiftTypeName() { return shiftTypeName; }
    public void setShiftTypeName(String shiftTypeName) { this.shiftTypeName = shiftTypeName; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public String getRevenue() { return revenue; }
    public void setRevenue(String revenue) { this.revenue = revenue; }

    public Integer getOrderCount() { return orderCount; }
    public void setOrderCount(Integer orderCount) { this.orderCount = orderCount; }

    public String getCashierId() { return cashierId; }
    public void setCashierId(String cashierId) { this.cashierId = cashierId; }

    public String getCashierName() { return cashierName; }
    public void setCashierName(String cashierName) { this.cashierName = cashierName; }

    public String getSettlementId() { return null; }
    public void setSettlementId(String settlementId) { }
}
