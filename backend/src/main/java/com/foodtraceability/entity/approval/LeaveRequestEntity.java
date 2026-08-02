package com.foodtraceability.entity.approval;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 请假申请详情实体
 * 对应数据库表 leave_requests
 * 存储请假类型、日期范围、天数等详细信息
 */
@TableName("leave_requests")
@Schema(description = "请假申请详情实体")
public class LeaveRequestEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 请假记录ID（UUID自动生成） */
    @TableId(type = IdType.ASSIGN_UUID, value = "request_id")
    @Schema(description = "请假记录ID", example = "l1m2n3o4p5q6...")
    private String id;

    /** 关联审批主表ID */
    @TableField("approval_id")
    @Schema(description = "关联审批主表ID", example = "approval001")
    private String approvalId;

    /** 申请人ID */
    @TableField("employee_id")
    @Schema(description = "申请人ID", example = "emp001")
    private String employeeId;

    /**
     * 请假类型
     * annual=年假, sick=病假, personal=事假, compensatory=调休,
     * marriage=婚假, funeral=丧假, paternity=陪产假
     */
    @TableField("leave_type")
    @Schema(description = "请假类型", example = "annual")
    private String leaveType;

    /** 开始日期 */
    @TableField("start_date")
    @Schema(description = "开始日期", example = "2026-06-10")
    private LocalDate startDate;

    /** 结束日期 */
    @TableField("end_date")
    @Schema(description = "结束日期", example = "2026-06-12")
    private LocalDate endDate;

    /**
     * 请假天数（支持半天0.5）
     * 精度保留1位小数
     */
    @TableField("days")
    @Schema(description = "请假天数", example = "3.0")
    private BigDecimal days;

    /** 请假原因 */
    @TableField("reason")
    @Schema(description = "请假原因", example = "家庭事务处理")
    private String reason;

    /**
     * 提交时的年假总额快照
     * 用于防止并发修改导致余额不一致
     */
    @TableField("annual_total_snapshot")
    @Schema(description = "提交时年假总额快照", example = "10")
    private Integer annualTotalSnapshot;

    /**
     * 提交时已使用年假快照
     * 用于计算剩余可用天数
     */
    @TableField("annual_used_snapshot")
    @Schema(description = "提交时已用年假快照", example = "5")
    private Integer annualUsedSnapshot;

    /** 紧急联系电话 */
    @TableField("contact_phone")
    @Schema(description = "紧急联系电话", example = "13800138000")
    private String contactPhone;

    /** 工作交接人 */
    @TableField("handover_to")
    @Schema(description = "工作交接人", example = "李四")
    private String handoverTo;

    /** 交接说明 */
    @TableField("handover_note")
    @Schema(description = "交接说明", example = "负责前厅接待工作交接给李四")
    private String handoverNote;

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

    public String getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(String leaveType) {
        this.leaveType = leaveType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getDays() {
        return days;
    }

    public void setDays(BigDecimal days) {
        this.days = days;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Integer getAnnualTotalSnapshot() {
        return annualTotalSnapshot;
    }

    public void setAnnualTotalSnapshot(Integer annualTotalSnapshot) {
        this.annualTotalSnapshot = annualTotalSnapshot;
    }

    public Integer getAnnualUsedSnapshot() {
        return annualUsedSnapshot;
    }

    public void setAnnualUsedSnapshot(Integer annualUsedSnapshot) {
        this.annualUsedSnapshot = annualUsedSnapshot;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getHandoverTo() {
        return handoverTo;
    }

    public void setHandoverTo(String handoverTo) {
        this.handoverTo = handoverTo;
    }

    public String getHandoverNote() {
        return handoverNote;
    }

    public void setHandoverNote(String handoverNote) {
        this.handoverNote = handoverNote;
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
