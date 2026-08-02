package com.foodtraceability.entity.approval;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

/**
 * 加班申请详情实体
 * 对应数据库表 overtime_requests
 * 存储加班日期、时间段、补偿方式等详细信息
 */
@TableName("overtime_requests")
@Schema(description = "加班申请详情实体")
public class OvertimeRequestEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 加班记录ID（UUID自动生成） */
    @TableId(type = IdType.ASSIGN_UUID, value = "request_id")
    @Schema(description = "加班记录ID", example = "o1p2q3r4s5t6...")
    private String id;

    /** 关联审批主表ID */
    @TableField("approval_id")
    @Schema(description = "关联审批主表ID", example = "approval001")
    private String approvalId;

    /** 申请人ID */
    @TableField("employee_id")
    @Schema(description = "申请人ID", example = "emp001")
    private String employeeId;

    /** 加班日期 */
    @TableField("overtime_date")
    @Schema(description = "加班日期", example = "2026-06-05")
    private LocalDate overtimeDate;

    /** 开始时间 */
    @TableField("start_time")
    @Schema(description = "开始时间", example = "18:00")
    private LocalTime startTime;

    /** 结束时间 */
    @TableField("end_time")
    @Schema(description = "结束时间", example = "22:00")
    private LocalTime endTime;

    /** 加班小时数（精度保留1位小数） */
    @TableField("hours")
    @Schema(description = "加班小时数", example = "4.0")
    private BigDecimal hours;

    /** 加班原因 */
    @TableField("reason")
    @Schema(description = "加班原因", example = "月末盘点需要延长工作时间")
    private String reason;

    /**
     * 补偿方式
     * overtime_pay=加班费, compensatory_leave=调休, mixed=混合补偿
     */
    @TableField("compensate_type")
    @Schema(description = "补偿方式", example = "compensatory_leave")
    private String compensateType;

    /**
     * 加班类型
     * weekday=工作日, weekend=周末, holiday=法定节假日
     */
    @TableField("overtime_type")
    @Schema(description = "加班类型", example = "weekend")
    private String overtimeType;

    /** 餐补金额（单位：元） */
    @TableField("meal_allowance")
    @Schema(description = "餐补金额（元）", example = "30.00")
    private BigDecimal mealAllowance;

    /** 交通补贴金额（单位：元） */
    @TableField("transport_allowance")
    @Schema(description = "交通补贴（元）", example = "50.00")
    private BigDecimal transportAllowance;

    /** 创建时间 */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    /** 更新时间 */
    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;

    /** 逻辑删除标记（0=未删除 1=已删除） */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记")
    private Integer deleted;

    /** 创建人ID */
    @TableField("created_by")
    @Schema(description = "创建人ID")
    private String createdBy;

    /** 更新人ID */
    @TableField("updated_by")
    @Schema(description = "更新人ID")
    private String updatedBy;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * 获取请求ID（别名方法，委托给 getId()）
     * 兼容历史调用方使用 requestId 语义访问主键
     * @return 请求ID
     */
    public String getRequestId() {
        return this.id;
    }

    /**
     * 设置请求ID（别名方法，委托给 setId()）
     * 兼容历史调用方使用 requestId 语义访问主键
     * @param requestId 请求ID
     */
    public void setRequestId(String requestId) {
        this.id = requestId;
    }

    public String getApprovalId() {
        return approvalId;
    }

    public void setApprovalId(String approvalId) {
        this.approvalId = approvalId;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public LocalDate getOvertimeDate() {
        return overtimeDate;
    }

    public void setOvertimeDate(LocalDate overtimeDate) {
        this.overtimeDate = overtimeDate;
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

    public BigDecimal getHours() {
        return hours;
    }

    public void setHours(BigDecimal hours) {
        this.hours = hours;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getCompensateType() {
        return compensateType;
    }

    public void setCompensateType(String compensateType) {
        this.compensateType = compensateType;
    }

    public String getOvertimeType() {
        return overtimeType;
    }

    public void setOvertimeType(String overtimeType) {
        this.overtimeType = overtimeType;
    }

    public BigDecimal getMealAllowance() {
        return mealAllowance;
    }

    public void setMealAllowance(BigDecimal mealAllowance) {
        this.mealAllowance = mealAllowance;
    }

    public BigDecimal getTransportAllowance() {
        return transportAllowance;
    }

    public void setTransportAllowance(BigDecimal transportAllowance) {
        this.transportAllowance = transportAllowance;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public LocalDateTime getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(LocalDateTime updatedTime) {
        this.updatedTime = updatedTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
