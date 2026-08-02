package com.foodtraceability.entity.finance;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 报销明细实体
 *
 * <p>Sprint 3.1 P0 F-001：从 entity/ 根目录迁移至 entity/finance/，金额字段
 * BigDecimal → Long（分），时间字段 created_at → create_time，
 * 新增 related_invoice_id 字段指向 FinanceInvoice.invoice_id（关联进项发票）。</p>
 */
@TableName("invoice_reimbursement_item")
public class InvoiceReimbursementItem implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 明细ID（雪花算法） */
    @TableId(value = "item_id", type = IdType.ASSIGN_ID)
    private Long itemId;

    /** 租户ID */
    @TableField("tenant_id")
    private Long tenantId;

    /** 报销单ID（外键 → invoice_reimbursement.reimbursement_id） */
    @TableField("reimbursement_id")
    private Long reimbursementId;

    /** 关联发票ID（外键 → finance_invoices.invoice_id，可空） */
    @TableField("related_invoice_id")
    private Long relatedInvoiceId;

    /** 凭证ID（关联电子凭证） */
    @TableField("voucher_id")
    private Long voucherId;

    /** 凭证编号 */
    @TableField("voucher_no")
    private String voucherNo;

    /** 凭证类型（发票/收据/其他） */
    @TableField("voucher_type")
    private String voucherType;

    /** 明细金额（单位：分） */
    @TableField("amount")
    private Long amount;

    /** 审批通过金额（单位：分） */
    @TableField("approved_amount")
    private Long approvedAmount;

    /** 费用类型（差旅费/招待费/办公费/交通费/通讯费/其他） */
    @TableField("expense_type")
    private String expenseType;

    /** 费用说明 */
    @TableField("expense_description")
    private String expenseDescription;

    /** 创建人ID */
    @TableField("created_by")
    private Long createdBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新人ID */
    @TableField("updated_by")
    private Long updatedBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标识（0-未删除 1-已删除） */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    public InvoiceReimbursementItem() {
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getReimbursementId() {
        return reimbursementId;
    }

    public void setReimbursementId(Long reimbursementId) {
        this.reimbursementId = reimbursementId;
    }

    public Long getRelatedInvoiceId() {
        return relatedInvoiceId;
    }

    public void setRelatedInvoiceId(Long relatedInvoiceId) {
        this.relatedInvoiceId = relatedInvoiceId;
    }

    public Long getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(Long voucherId) {
        this.voucherId = voucherId;
    }

    public String getVoucherNo() {
        return voucherNo;
    }

    public void setVoucherNo(String voucherNo) {
        this.voucherNo = voucherNo;
    }

    public String getVoucherType() {
        return voucherType;
    }

    public void setVoucherType(String voucherType) {
        this.voucherType = voucherType;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public Long getApprovedAmount() {
        return approvedAmount;
    }

    public void setApprovedAmount(Long approvedAmount) {
        this.approvedAmount = approvedAmount;
    }

    public String getExpenseType() {
        return expenseType;
    }

    public void setExpenseType(String expenseType) {
        this.expenseType = expenseType;
    }

    public String getExpenseDescription() {
        return expenseDescription;
    }

    public void setExpenseDescription(String expenseDescription) {
        this.expenseDescription = expenseDescription;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
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
