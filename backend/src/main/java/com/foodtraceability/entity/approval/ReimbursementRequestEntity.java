package com.foodtraceability.entity.approval;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 报销申请详情实体
 * 对应数据库表 reimbursement_requests
 * 存储报销总金额、票据数量、类别、收款账号等详细信息
 */
@TableName("reimbursement_requests")
@Schema(description = "报销申请详情实体")
public class ReimbursementRequestEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 报销记录ID（UUID自动生成） */
    @TableId(type = IdType.ASSIGN_UUID, value = "request_id")
    @Schema(description = "报销记录ID", example = "r1w2x3y4z5a6...")
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
     * 总金额（单位：分）
     * 所有报销项目金额之和
     */
    @TableField("total_amount")
    @Schema(description = "总金额（分）", example = "150000")
    private BigDecimal totalAmount;

    /**
     * 月度预算（单位：分）
     * 当月可用报销额度
     */
    @TableField("monthly_budget")
    @Schema(description = "月度预算（分）", example = "300000")
    private BigDecimal monthlyBudget;

    /**
     * 年度预算（单位：分）
     * 本年度累计可用报销额度
     */
    @TableField("year_budget")
    @Schema(description = "年度预算（分）", example = "3600000")
    private BigDecimal yearBudget;

    /** 票据/发票数量 */
    @TableField("receipt_count")
    @Schema(description = "票据数量", example = "5")
    private Integer receiptCount;

    /** 关联出差申请ID（差旅报销时关联） */
    @TableField("related_travel_id")
    @Schema(description = "关联出差申请ID", example = "travel001")
    private String relatedTravelId;

    /**
     * 报销类别
     * travel=差旅, office=办公, entertainment=招待, communication=通讯, other=其他
     */
    @TableField("reimbursement_category")
    @Schema(description = "报销类别", example = "travel")
    private String reimbursementCategory;

    /** 收款银行账号 */
    @TableField("bank_account")
    @Schema(description = "收款银行账号", example = "622848****1234")
    private String bankAccount;

    /** 收款人姓名 */
    @TableField("payee_name")
    @Schema(description = "收款人姓名", example = "张三")
    private String payeeName;

    /** 报销说明 */
    @TableField("description")
    @Schema(description = "报销说明", example = "参加上海餐饮展会的差旅费用报销")
    private String description;

    /**
     * 发票号码（逗号分隔）
     * 支持多张发票
     */
    @TableField("invoice_nos")
    @Schema(description = "发票号（逗号分隔）", example = "INV001,INV002,INV003")
    private String invoiceNos;

    /**
     * 政策备注
     * 超标提示或特殊政策说明
     */
    @TableField("policy_notes")
    @Schema(description = "政策备注", example = "超出标准部分需部门经理特别批准")
    private String policyNotes;

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

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getMonthlyBudget() {
        return monthlyBudget;
    }

    public void setMonthlyBudget(BigDecimal monthlyBudget) {
        this.monthlyBudget = monthlyBudget;
    }

    public BigDecimal getYearBudget() {
        return yearBudget;
    }

    public void setYearBudget(BigDecimal yearBudget) {
        this.yearBudget = yearBudget;
    }

    public Integer getReceiptCount() {
        return receiptCount;
    }

    public void setReceiptCount(Integer receiptCount) {
        this.receiptCount = receiptCount;
    }

    public String getRelatedTravelId() {
        return relatedTravelId;
    }

    public void setRelatedTravelId(String relatedTravelId) {
        this.relatedTravelId = relatedTravelId;
    }

    public String getReimbursementCategory() {
        return reimbursementCategory;
    }

    public void setReimbursementCategory(String reimbursementCategory) {
        this.reimbursementCategory = reimbursementCategory;
    }

    public String getBankAccount() {
        return bankAccount;
    }

    public void setBankAccount(String bankAccount) {
        this.bankAccount = bankAccount;
    }

    public String getPayeeName() {
        return payeeName;
    }

    public void setPayeeName(String payeeName) {
        this.payeeName = payeeName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInvoiceNos() {
        return invoiceNos;
    }

    public void setInvoiceNos(String invoiceNos) {
        this.invoiceNos = invoiceNos;
    }

    public String getPolicyNotes() {
        return policyNotes;
    }

    public void setPolicyNotes(String policyNotes) {
        this.policyNotes = policyNotes;
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
