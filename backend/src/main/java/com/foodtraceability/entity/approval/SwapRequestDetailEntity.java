package com.foodtraceability.entity.approval;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 换班申请详情实体
 * 对应数据库表 swap_request_details
 * 存储换班的发起人、目标人员、原班次和目标班次等信息
 * 注意：与现有 schedule.SwapRequest 区分，本实体是审批模块的扩展详情
 */
@TableName("swap_request_details")
@Schema(description = "换班申请详情实体")
public class SwapRequestDetailEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 换班详情ID（UUID自动生成） */
    @TableId(type = IdType.ASSIGN_UUID, value = "detail_id")
    @Schema(description = "换班详情ID", example = "s1t2u3v4w5x6...")
    private String id;

    /** 关联审批主表ID */
    @TableField("approval_id")
    @Schema(description = "关联审批主表ID", example = "approval001")
    private String approvalId;

    /** 发起人ID */
    @TableField("initiator_id")
    @Schema(description = "发起人ID", example = "emp001")
    private String initiatorId;

    /** 目标换班人ID */
    @TableField("target_employee_id")
    @Schema(description = "目标换班人ID", example = "emp002")
    private String targetEmployeeId;

    /** 目标换班人姓名 */
    @TableField("target_employee_name")
    @Schema(description = "目标换班人姓名", example = "李四")
    private String targetEmployeeName;

    /** 原班次日期 */
    @TableField("original_date")
    @Schema(description = "原班次日期", example = "2026-06-08")
    private LocalDate originalDate;

    /**
     * 原班次类型
     * morning=早班, afternoon=中班, evening=晚班, night=夜班
     */
    @TableField("original_shift_type")
    @Schema(description = "原班次类型", example = "morning")
    private String originalShiftType;

    /** 目标班次日期 */
    @TableField("target_date")
    @Schema(description = "目标班次日期", example = "2026-06-10")
    private LocalDate targetDate;

    /**
     * 目标班次类型
     * morning=早班, afternoon=中班, evening=晚班, night=夜班
     */
    @TableField("target_shift_type")
    @Schema(description = "目标班次类型", example = "afternoon")
    private String targetShiftType;

    /** 对方是否确认（需对方同意后才能进入审批流程） */
    @TableField("partner_confirmed")
    @Schema(description = "对方是否确认", example = "false")
    private Boolean partnerConfirmed;

    /** 对方确认时间 */
    @TableField("confirm_time")
    @Schema(description = "对方确认时间", example = "2026-06-05T14:30:00")
    private LocalDateTime confirmTime;

    /** 覆盖方案说明（如何保证原班次有人值守） */
    @TableField("coverage_plan")
    @Schema(description = "覆盖方案说明", example = "由王五临时顶替原班次")
    private String coveragePlan;

    /** 换班原因 */
    @TableField("reason")
    @Schema(description = "换班原因", example = "个人有事需要调换班次")
    private String reason;

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

    public String getInitiatorId() {
        return initiatorId;
    }

    public void setInitiatorId(String initiatorId) {
        this.initiatorId = initiatorId;
    }

    public String getTargetEmployeeId() {
        return targetEmployeeId;
    }

    public void setTargetEmployeeId(String targetEmployeeId) {
        this.targetEmployeeId = targetEmployeeId;
    }

    public String getTargetEmployeeName() {
        return targetEmployeeName;
    }

    public void setTargetEmployeeName(String targetEmployeeName) {
        this.targetEmployeeName = targetEmployeeName;
    }

    public LocalDate getOriginalDate() {
        return originalDate;
    }

    public void setOriginalDate(LocalDate originalDate) {
        this.originalDate = originalDate;
    }

    public String getOriginalShiftType() {
        return originalShiftType;
    }

    public void setOriginalShiftType(String originalShiftType) {
        this.originalShiftType = originalShiftType;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public String getTargetShiftType() {
        return targetShiftType;
    }

    public void setTargetShiftType(String targetShiftType) {
        this.targetShiftType = targetShiftType;
    }

    public Boolean getPartnerConfirmed() {
        return partnerConfirmed;
    }

    public void setPartnerConfirmed(Boolean partnerConfirmed) {
        this.partnerConfirmed = partnerConfirmed;
    }

    public LocalDateTime getConfirmTime() {
        return confirmTime;
    }

    public void setConfirmTime(LocalDateTime confirmTime) {
        this.confirmTime = confirmTime;
    }

    public String getCoveragePlan() {
        return coveragePlan;
    }

    public void setCoveragePlan(String coveragePlan) {
        this.coveragePlan = coveragePlan;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
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
