package com.foodtraceability.dto.finance;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 报销明细DTO
 *
 * <p>Sprint 3.1 P0 F-001：从 dto/ 根目录迁移至 dto/finance/，金额字段
 * BigDecimal → Long（分），对齐 InvoiceReimbursementItem 实体字段。
 * 与 dto/approval/ReimbursementItemDTO（HR 审批流程）不是同一个 DTO。</p>
 */
public class ReimbursementItemDTO {

    /** 关联发票ID（外键 → finance_invoices.invoice_id，可空） */
    private Long relatedInvoiceId;

    /** 凭证ID（关联电子凭证，可空） */
    private Long voucherId;

    /** 凭证编号 */
    @Size(max = 50, message = "凭证编号长度不能超过50个字符")
    private String voucherNo;

    /** 凭证类型（发票/收据/其他） */
    @Size(max = 30, message = "凭证类型长度不能超过30个字符")
    private String voucherType;

    /** 明细金额（单位：分，必填，必须大于0） */
    @NotNull(message = "报销明细金额不能为空")
    @Min(value = 1, message = "报销明细金额必须大于0")
    private Long amount;

    /** 费用类型（差旅费/招待费/办公费/交通费/通讯费/其他，必填） */
    @NotBlank(message = "费用类型不能为空")
    @Size(max = 30, message = "费用类型长度不能超过30个字符")
    private String expenseType;

    /** 费用说明 */
    @Size(max = 255, message = "费用说明长度不能超过255个字符")
    private String expenseDescription;

    public ReimbursementItemDTO() {
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
}
