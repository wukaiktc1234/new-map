package com.foodtraceability.entity.approval;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 员工日常审批主表实体
 * 对应数据库表 employee_approvals
 * 统一管理各类审批流程（请假/加班/换班/出差/报销/领用）的主记录
 */
@TableName("employee_approvals")
@Schema(description = "员工日常审批主表实体")
public class EmployeeApproval implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 审批ID（UUID自动生成） */
    @TableId(type = IdType.ASSIGN_UUID, value = "approval_id")
    @Schema(description = "审批ID", example = "a1b2c3d4e5f6...")
    private String id;

    /** 审批编号（如 AP20260301001），全局唯一 */
    @TableField("approval_no")
    @Schema(description = "审批编号", example = "AP20260301001")
    private String approvalNo;

    /**
     * 审批类型
     * leave=请假, overtime=加班, swap=换班, travel=出差, reimbursement=报销, requisition=领用
     */
    @TableField("type")
    @Schema(description = "审批类型", example = "leave")
    private String type;

    /** 标题（如 "年假申请-张三"） */
    @TableField("title")
    @Schema(description = "标题", example = "年假申请-张三")
    private String title;

    /** 申请人ID（关联employees表） */
    @TableField("applicant_id")
    @Schema(description = "申请人ID", example = "emp001")
    private String applicantId;

    /**
     * 申请人姓名（非数据库字段，通过关联查询填充）
     * 用于列表展示时避免额外查询
     */
    @TableField(exist = false)
    @Schema(description = "申请人姓名", example = "张三")
    private String applicantName;

    /**
     * 审批状态
     * pending=待审批, approved=已通过, rejected=已驳回, withdrawn=已撤回
     */
    @TableField("status")
    @Schema(description = "审批状态", example = "pending")
    private String status;

    /**
     * 优先级
     * normal=普通, urgent=紧急, critical=特急
     */
    @TableField("priority")
    @Schema(description = "优先级", example = "normal")
    private String priority;

    /** 当前审批人ID */
    @TableField("current_approver_id")
    @Schema(description = "当前审批人ID", example = "approver001")
    private String currentApproverId;

    /**
     * 表单摘要（JSON字符串）
     * 存储提交的核心信息快照，用于列表预览和搜索
     */
    @TableField("form_summary")
    @Schema(description = "表单摘要（JSON）")
    private String formSummary;

    /**
     * contextData类型标识
     * 与type字段一致，用于反序列化contextData时确定具体类型
     */
    @TableField("context_data_type")
    @Schema(description = "上下文数据类型标识", example = "leave")
    private String contextDataType;

    /**
     * 风险等级
     * null=无风险, info=提示, warning=警告, danger=危险, prohibited=禁止
     */
    @TableField("risk_level")
    @Schema(description = "风险等级", example = "info")
    private String riskLevel;

    /** 备注 */
    @TableField("remark")
    @Schema(description = "备注", example = "需要提前安排工作交接")
    private String remark;

    /** 门店ID */
    @TableField("store_id")
    @Schema(description = "门店ID", example = "store001")
    private String storeId;

    /** 部门ID */
    @TableField("department_id")
    @Schema(description = "部门ID", example = "dept001")
    private String departmentId;

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

    /**
     * 最近催办时间（非数据库字段，内存态使用）
     * 用于记录最后一次催办的时间
     */
    @TableField(exist = false)
    @Schema(description = "最近催办时间")
    private LocalDateTime lastUrgeTime;

    /**
     * 催办次数（非数据库字段，内存态使用）
     * 用于记录该审批被催办的总次数
     */
    @TableField(exist = false)
    @Schema(description = "催办次数")
    private Integer urgeCount;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * 获取审批ID（别名方法，委托给 getId()）
     * 兼容历史调用方使用 approvalId 语义访问主键
     * @return 审批ID
     */
    public String getApprovalId() {
        return this.id;
    }

    /**
     * 设置审批ID（别名方法，委托给 setId()）
     * 兼容历史调用方使用 approvalId 语义访问主键
     * @param approvalId 审批ID
     */
    public void setApprovalId(String approvalId) {
        this.id = approvalId;
    }

    public LocalDateTime getLastUrgeTime() {
        return lastUrgeTime;
    }

    public void setLastUrgeTime(LocalDateTime lastUrgeTime) {
        this.lastUrgeTime = lastUrgeTime;
    }

    public Integer getUrgeCount() {
        return urgeCount;
    }

    public void setUrgeCount(Integer urgeCount) {
        this.urgeCount = urgeCount;
    }

    public String getApprovalNo() {
        return approvalNo;
    }

    public void setApprovalNo(String approvalNo) {
        this.approvalNo = approvalNo;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getApplicantId() {
        return applicantId;
    }

    public void setApplicantId(String applicantId) {
        this.applicantId = applicantId;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getCurrentApproverId() {
        return currentApproverId;
    }

    public void setCurrentApproverId(String currentApproverId) {
        this.currentApproverId = currentApproverId;
    }

    public String getFormSummary() {
        return formSummary;
    }

    public void setFormSummary(String formSummary) {
        this.formSummary = formSummary;
    }

    public String getContextDataType() {
        return contextDataType;
    }

    public void setContextDataType(String contextDataType) {
        this.contextDataType = contextDataType;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
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
