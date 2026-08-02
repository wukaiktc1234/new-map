package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * POS班次实体类
 * 用于管理收银终端的班次信息
 */
@TableName("pos_shifts")
@Schema(description = "POS班次实体")
public class PosShift {

    /**
     * 班次ID（主键）
     */
    @TableId(type = IdType.AUTO, value = "shift_id")
    @Schema(description = "班次ID", example = "1")
    private Long shiftId;

    /**
     * 终端ID
     */
    @Schema(description = "终端ID", example = "POS001")
    private String terminalId;

    /**
     * 员工ID
     */
    @Schema(description = "员工ID", example = "EMP001")
    private String employeeId;

    /**
     * 员工姓名
     */
    @Schema(description = "员工姓名", example = "张三")
    private String employeeName;

    /**
     * 班次类型：day-日班，night-夜班，custom-自定义
     */
    @Schema(description = "班次类型", example = "day")
    private String shiftType;

    /**
     * 开始时间
     */
    @Schema(description = "开始时间", example = "2024-01-01 08:00:00")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @Schema(description = "结束时间", example = "2024-01-01 16:00:00")
    private LocalDateTime endTime;

    /**
     * 开机现金（起始金额）
     */
    @Schema(description = "开机现金", example = "500.00")
    private BigDecimal openingCash;

    /**
     * 关机现金（结束金额）
     */
    @Schema(description = "关机现金", example = "1250.00")
    private BigDecimal closingCash;

    /**
     * 总订单数
     */
    @Schema(description = "总订单数", example = "50")
    private Integer totalOrders;

    /**
     * 总金额
     */
    @Schema(description = "总金额", example = "750.00")
    private BigDecimal totalAmount;

    /**
     * 班次状态：active-进行中，completed-已完成，cancelled-已取消
     */
    @Schema(description = "班次状态", example = "active")
    private String status;

    /**
     * 交接备注
     */
    @Schema(description = "交接备注", example = "交接正常")
    private String handoverRemark;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2024-01-01 08:00:00")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间", example = "2024-01-01 16:00:00")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 是否删除（逻辑删除）
     */
    @Schema(description = "是否删除", example = "0")
    @TableLogic
    @TableField(value = "deleted")
    private Integer deleted;

    // Getter and Setter methods

    public Long getShiftId() {
        return shiftId;
    }

    public void setShiftId(Long shiftId) {
        this.shiftId = shiftId;
    }

    public String getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getShiftType() {
        return shiftType;
    }

    public void setShiftType(String shiftType) {
        this.shiftType = shiftType;
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

    public Integer getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(Integer totalOrders) {
        this.totalOrders = totalOrders;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getHandoverRemark() {
        return handoverRemark;
    }

    public void setHandoverRemark(String handoverRemark) {
        this.handoverRemark = handoverRemark;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
