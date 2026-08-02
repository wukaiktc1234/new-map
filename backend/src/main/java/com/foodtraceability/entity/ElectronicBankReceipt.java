package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("electronic_bank_receipt")
@Schema(description = "银行电子回单")
public class ElectronicBankReceipt {
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;
    @TableField("tenant_id")
    @Schema(description = "租户ID")
    private Long tenantId;
    @TableField("voucher_id")
    @Schema(description = "关联凭证ID")
    private Long voucherId;
    @TableField("receipt_number")
    @Schema(description = "回单编号")
    private String receiptNumber;
    @TableField("issue_date")
    @Schema(description = "开票日期")
    private LocalDate issueDate;
    @TableField("receipt_type")
    @Schema(description = "回单类型")
    private String receiptType;
    @TableField("credit_debit_flag")
    @Schema(description = "借贷标识")
    private String creditDebitFlag;
    @TableField("cash_transfer_flag")
    @Schema(description = "现转标识")
    private String cashTransferFlag;
    @TableField("bookkeeping_date")
    @Schema(description = "记账日期")
    private LocalDate bookkeepingDate;
    @TableField("bookkeeping_time")
    @Schema(description = "记账时间")
    private String bookkeepingTime;
    @TableField("bookkeeper")
    @Schema(description = "记账员")
    private String bookkeeper;
    @TableField("transaction_amount")
    @Schema(description = "交易金额")
    private BigDecimal transactionAmount;
    @TableField("currency")
    @Schema(description = "币种")
    private String currency;
    @TableField("payer_account_name")
    @Schema(description = "付款人户名")
    private String payerAccountName;
    @TableField("payer_account_number")
    @Schema(description = "付款人账号")
    private String payerAccountNumber;
    @TableField("payer_opening_bank")
    @Schema(description = "付款人开户行")
    private String payerOpeningBank;
    @TableField("payee_account_name")
    @Schema(description = "收款人户名")
    private String payeeAccountName;
    @TableField("payee_account_number")
    @Schema(description = "收款人账号")
    private String payeeAccountNumber;
    @TableField("payee_opening_bank")
    @Schema(description = "收款人开户行")
    private String payeeOpeningBank;
    @TableField("source_doc_type")
    @Schema(description = "原始凭证类型")
    private String sourceDocType;
    @TableField("source_doc_number")
    @Schema(description = "原始凭证号码")
    private String sourceDocNumber;
    @TableField("transaction_code")
    @Schema(description = "交易代码")
    private String transactionCode;
    @TableField("usage")
    @Schema(description = "用途")
    private String usage;
    @TableField("notes")
    @Schema(description = "备注")
    private String notes;
    @TableField("identifying_code")
    @Schema(description = "校验码")
    private String identifyingCode;
    @TableField("create_time")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @TableField("update_time")
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
    @TableField("deleted")
    @TableLogic
    @Schema(description = "逻辑删除")
    private Integer deleted;

    public ElectronicBankReceipt() {
    }

    public Long getId() {
        return this.id;
    }

    public Long getTenantId() {
        return this.tenantId;
    }

    public Long getVoucherId() {
        return this.voucherId;
    }

    public String getReceiptNumber() {
        return this.receiptNumber;
    }

    public LocalDate getIssueDate() {
        return this.issueDate;
    }

    public String getReceiptType() {
        return this.receiptType;
    }

    public String getCreditDebitFlag() {
        return this.creditDebitFlag;
    }

    public String getCashTransferFlag() {
        return this.cashTransferFlag;
    }

    public LocalDate getBookkeepingDate() {
        return this.bookkeepingDate;
    }

    public String getBookkeepingTime() {
        return this.bookkeepingTime;
    }

    public String getBookkeeper() {
        return this.bookkeeper;
    }

    public BigDecimal getTransactionAmount() {
        return this.transactionAmount;
    }

    public String getCurrency() {
        return this.currency;
    }

    public String getPayerAccountName() {
        return this.payerAccountName;
    }

    public String getPayerAccountNumber() {
        return this.payerAccountNumber;
    }

    public String getPayerOpeningBank() {
        return this.payerOpeningBank;
    }

    public String getPayeeAccountName() {
        return this.payeeAccountName;
    }

    public String getPayeeAccountNumber() {
        return this.payeeAccountNumber;
    }

    public String getPayeeOpeningBank() {
        return this.payeeOpeningBank;
    }

    public String getSourceDocType() {
        return this.sourceDocType;
    }

    public String getSourceDocNumber() {
        return this.sourceDocNumber;
    }

    public String getTransactionCode() {
        return this.transactionCode;
    }

    public String getUsage() {
        return this.usage;
    }

    public String getNotes() {
        return this.notes;
    }

    public String getIdentifyingCode() {
        return this.identifyingCode;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public Integer getDeleted() {
        return this.deleted;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setTenantId(final Long tenantId) {
        this.tenantId = tenantId;
    }

    public void setVoucherId(final Long voucherId) {
        this.voucherId = voucherId;
    }

    public void setReceiptNumber(final String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    public void setIssueDate(final LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public void setReceiptType(final String receiptType) {
        this.receiptType = receiptType;
    }

    public void setCreditDebitFlag(final String creditDebitFlag) {
        this.creditDebitFlag = creditDebitFlag;
    }

    public void setCashTransferFlag(final String cashTransferFlag) {
        this.cashTransferFlag = cashTransferFlag;
    }

    public void setBookkeepingDate(final LocalDate bookkeepingDate) {
        this.bookkeepingDate = bookkeepingDate;
    }

    public void setBookkeepingTime(final String bookkeepingTime) {
        this.bookkeepingTime = bookkeepingTime;
    }

    public void setBookkeeper(final String bookkeeper) {
        this.bookkeeper = bookkeeper;
    }

    public void setTransactionAmount(final BigDecimal transactionAmount) {
        this.transactionAmount = transactionAmount;
    }

    public void setCurrency(final String currency) {
        this.currency = currency;
    }

    public void setPayerAccountName(final String payerAccountName) {
        this.payerAccountName = payerAccountName;
    }

    public void setPayerAccountNumber(final String payerAccountNumber) {
        this.payerAccountNumber = payerAccountNumber;
    }

    public void setPayerOpeningBank(final String payerOpeningBank) {
        this.payerOpeningBank = payerOpeningBank;
    }

    public void setPayeeAccountName(final String payeeAccountName) {
        this.payeeAccountName = payeeAccountName;
    }

    public void setPayeeAccountNumber(final String payeeAccountNumber) {
        this.payeeAccountNumber = payeeAccountNumber;
    }

    public void setPayeeOpeningBank(final String payeeOpeningBank) {
        this.payeeOpeningBank = payeeOpeningBank;
    }

    public void setSourceDocType(final String sourceDocType) {
        this.sourceDocType = sourceDocType;
    }

    public void setSourceDocNumber(final String sourceDocNumber) {
        this.sourceDocNumber = sourceDocNumber;
    }

    public void setTransactionCode(final String transactionCode) {
        this.transactionCode = transactionCode;
    }

    public void setUsage(final String usage) {
        this.usage = usage;
    }

    public void setNotes(final String notes) {
        this.notes = notes;
    }

    public void setIdentifyingCode(final String identifyingCode) {
        this.identifyingCode = identifyingCode;
    }

    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(final LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof ElectronicBankReceipt)) return false;
        final ElectronicBankReceipt other = (ElectronicBankReceipt) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$tenantId = this.getTenantId();
        final java.lang.Object other$tenantId = other.getTenantId();
        if (this$tenantId == null ? other$tenantId != null : !this$tenantId.equals(other$tenantId)) return false;
        final java.lang.Object this$voucherId = this.getVoucherId();
        final java.lang.Object other$voucherId = other.getVoucherId();
        if (this$voucherId == null ? other$voucherId != null : !this$voucherId.equals(other$voucherId)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$receiptNumber = this.getReceiptNumber();
        final java.lang.Object other$receiptNumber = other.getReceiptNumber();
        if (this$receiptNumber == null ? other$receiptNumber != null : !this$receiptNumber.equals(other$receiptNumber)) return false;
        final java.lang.Object this$issueDate = this.getIssueDate();
        final java.lang.Object other$issueDate = other.getIssueDate();
        if (this$issueDate == null ? other$issueDate != null : !this$issueDate.equals(other$issueDate)) return false;
        final java.lang.Object this$receiptType = this.getReceiptType();
        final java.lang.Object other$receiptType = other.getReceiptType();
        if (this$receiptType == null ? other$receiptType != null : !this$receiptType.equals(other$receiptType)) return false;
        final java.lang.Object this$creditDebitFlag = this.getCreditDebitFlag();
        final java.lang.Object other$creditDebitFlag = other.getCreditDebitFlag();
        if (this$creditDebitFlag == null ? other$creditDebitFlag != null : !this$creditDebitFlag.equals(other$creditDebitFlag)) return false;
        final java.lang.Object this$cashTransferFlag = this.getCashTransferFlag();
        final java.lang.Object other$cashTransferFlag = other.getCashTransferFlag();
        if (this$cashTransferFlag == null ? other$cashTransferFlag != null : !this$cashTransferFlag.equals(other$cashTransferFlag)) return false;
        final java.lang.Object this$bookkeepingDate = this.getBookkeepingDate();
        final java.lang.Object other$bookkeepingDate = other.getBookkeepingDate();
        if (this$bookkeepingDate == null ? other$bookkeepingDate != null : !this$bookkeepingDate.equals(other$bookkeepingDate)) return false;
        final java.lang.Object this$bookkeepingTime = this.getBookkeepingTime();
        final java.lang.Object other$bookkeepingTime = other.getBookkeepingTime();
        if (this$bookkeepingTime == null ? other$bookkeepingTime != null : !this$bookkeepingTime.equals(other$bookkeepingTime)) return false;
        final java.lang.Object this$bookkeeper = this.getBookkeeper();
        final java.lang.Object other$bookkeeper = other.getBookkeeper();
        if (this$bookkeeper == null ? other$bookkeeper != null : !this$bookkeeper.equals(other$bookkeeper)) return false;
        final java.lang.Object this$transactionAmount = this.getTransactionAmount();
        final java.lang.Object other$transactionAmount = other.getTransactionAmount();
        if (this$transactionAmount == null ? other$transactionAmount != null : !this$transactionAmount.equals(other$transactionAmount)) return false;
        final java.lang.Object this$currency = this.getCurrency();
        final java.lang.Object other$currency = other.getCurrency();
        if (this$currency == null ? other$currency != null : !this$currency.equals(other$currency)) return false;
        final java.lang.Object this$payerAccountName = this.getPayerAccountName();
        final java.lang.Object other$payerAccountName = other.getPayerAccountName();
        if (this$payerAccountName == null ? other$payerAccountName != null : !this$payerAccountName.equals(other$payerAccountName)) return false;
        final java.lang.Object this$payerAccountNumber = this.getPayerAccountNumber();
        final java.lang.Object other$payerAccountNumber = other.getPayerAccountNumber();
        if (this$payerAccountNumber == null ? other$payerAccountNumber != null : !this$payerAccountNumber.equals(other$payerAccountNumber)) return false;
        final java.lang.Object this$payerOpeningBank = this.getPayerOpeningBank();
        final java.lang.Object other$payerOpeningBank = other.getPayerOpeningBank();
        if (this$payerOpeningBank == null ? other$payerOpeningBank != null : !this$payerOpeningBank.equals(other$payerOpeningBank)) return false;
        final java.lang.Object this$payeeAccountName = this.getPayeeAccountName();
        final java.lang.Object other$payeeAccountName = other.getPayeeAccountName();
        if (this$payeeAccountName == null ? other$payeeAccountName != null : !this$payeeAccountName.equals(other$payeeAccountName)) return false;
        final java.lang.Object this$payeeAccountNumber = this.getPayeeAccountNumber();
        final java.lang.Object other$payeeAccountNumber = other.getPayeeAccountNumber();
        if (this$payeeAccountNumber == null ? other$payeeAccountNumber != null : !this$payeeAccountNumber.equals(other$payeeAccountNumber)) return false;
        final java.lang.Object this$payeeOpeningBank = this.getPayeeOpeningBank();
        final java.lang.Object other$payeeOpeningBank = other.getPayeeOpeningBank();
        if (this$payeeOpeningBank == null ? other$payeeOpeningBank != null : !this$payeeOpeningBank.equals(other$payeeOpeningBank)) return false;
        final java.lang.Object this$sourceDocType = this.getSourceDocType();
        final java.lang.Object other$sourceDocType = other.getSourceDocType();
        if (this$sourceDocType == null ? other$sourceDocType != null : !this$sourceDocType.equals(other$sourceDocType)) return false;
        final java.lang.Object this$sourceDocNumber = this.getSourceDocNumber();
        final java.lang.Object other$sourceDocNumber = other.getSourceDocNumber();
        if (this$sourceDocNumber == null ? other$sourceDocNumber != null : !this$sourceDocNumber.equals(other$sourceDocNumber)) return false;
        final java.lang.Object this$transactionCode = this.getTransactionCode();
        final java.lang.Object other$transactionCode = other.getTransactionCode();
        if (this$transactionCode == null ? other$transactionCode != null : !this$transactionCode.equals(other$transactionCode)) return false;
        final java.lang.Object this$usage = this.getUsage();
        final java.lang.Object other$usage = other.getUsage();
        if (this$usage == null ? other$usage != null : !this$usage.equals(other$usage)) return false;
        final java.lang.Object this$notes = this.getNotes();
        final java.lang.Object other$notes = other.getNotes();
        if (this$notes == null ? other$notes != null : !this$notes.equals(other$notes)) return false;
        final java.lang.Object this$identifyingCode = this.getIdentifyingCode();
        final java.lang.Object other$identifyingCode = other.getIdentifyingCode();
        if (this$identifyingCode == null ? other$identifyingCode != null : !this$identifyingCode.equals(other$identifyingCode)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        final java.lang.Object this$updatedAt = this.getUpdatedAt();
        final java.lang.Object other$updatedAt = other.getUpdatedAt();
        if (this$updatedAt == null ? other$updatedAt != null : !this$updatedAt.equals(other$updatedAt)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ElectronicBankReceipt;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $tenantId = this.getTenantId();
        result = result * PRIME + ($tenantId == null ? 43 : $tenantId.hashCode());
        final java.lang.Object $voucherId = this.getVoucherId();
        result = result * PRIME + ($voucherId == null ? 43 : $voucherId.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $receiptNumber = this.getReceiptNumber();
        result = result * PRIME + ($receiptNumber == null ? 43 : $receiptNumber.hashCode());
        final java.lang.Object $issueDate = this.getIssueDate();
        result = result * PRIME + ($issueDate == null ? 43 : $issueDate.hashCode());
        final java.lang.Object $receiptType = this.getReceiptType();
        result = result * PRIME + ($receiptType == null ? 43 : $receiptType.hashCode());
        final java.lang.Object $creditDebitFlag = this.getCreditDebitFlag();
        result = result * PRIME + ($creditDebitFlag == null ? 43 : $creditDebitFlag.hashCode());
        final java.lang.Object $cashTransferFlag = this.getCashTransferFlag();
        result = result * PRIME + ($cashTransferFlag == null ? 43 : $cashTransferFlag.hashCode());
        final java.lang.Object $bookkeepingDate = this.getBookkeepingDate();
        result = result * PRIME + ($bookkeepingDate == null ? 43 : $bookkeepingDate.hashCode());
        final java.lang.Object $bookkeepingTime = this.getBookkeepingTime();
        result = result * PRIME + ($bookkeepingTime == null ? 43 : $bookkeepingTime.hashCode());
        final java.lang.Object $bookkeeper = this.getBookkeeper();
        result = result * PRIME + ($bookkeeper == null ? 43 : $bookkeeper.hashCode());
        final java.lang.Object $transactionAmount = this.getTransactionAmount();
        result = result * PRIME + ($transactionAmount == null ? 43 : $transactionAmount.hashCode());
        final java.lang.Object $currency = this.getCurrency();
        result = result * PRIME + ($currency == null ? 43 : $currency.hashCode());
        final java.lang.Object $payerAccountName = this.getPayerAccountName();
        result = result * PRIME + ($payerAccountName == null ? 43 : $payerAccountName.hashCode());
        final java.lang.Object $payerAccountNumber = this.getPayerAccountNumber();
        result = result * PRIME + ($payerAccountNumber == null ? 43 : $payerAccountNumber.hashCode());
        final java.lang.Object $payerOpeningBank = this.getPayerOpeningBank();
        result = result * PRIME + ($payerOpeningBank == null ? 43 : $payerOpeningBank.hashCode());
        final java.lang.Object $payeeAccountName = this.getPayeeAccountName();
        result = result * PRIME + ($payeeAccountName == null ? 43 : $payeeAccountName.hashCode());
        final java.lang.Object $payeeAccountNumber = this.getPayeeAccountNumber();
        result = result * PRIME + ($payeeAccountNumber == null ? 43 : $payeeAccountNumber.hashCode());
        final java.lang.Object $payeeOpeningBank = this.getPayeeOpeningBank();
        result = result * PRIME + ($payeeOpeningBank == null ? 43 : $payeeOpeningBank.hashCode());
        final java.lang.Object $sourceDocType = this.getSourceDocType();
        result = result * PRIME + ($sourceDocType == null ? 43 : $sourceDocType.hashCode());
        final java.lang.Object $sourceDocNumber = this.getSourceDocNumber();
        result = result * PRIME + ($sourceDocNumber == null ? 43 : $sourceDocNumber.hashCode());
        final java.lang.Object $transactionCode = this.getTransactionCode();
        result = result * PRIME + ($transactionCode == null ? 43 : $transactionCode.hashCode());
        final java.lang.Object $usage = this.getUsage();
        result = result * PRIME + ($usage == null ? 43 : $usage.hashCode());
        final java.lang.Object $notes = this.getNotes();
        result = result * PRIME + ($notes == null ? 43 : $notes.hashCode());
        final java.lang.Object $identifyingCode = this.getIdentifyingCode();
        result = result * PRIME + ($identifyingCode == null ? 43 : $identifyingCode.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        final java.lang.Object $updatedAt = this.getUpdatedAt();
        result = result * PRIME + ($updatedAt == null ? 43 : $updatedAt.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "ElectronicBankReceipt(id=" + this.getId() + ", tenantId=" + this.getTenantId() + ", voucherId=" + this.getVoucherId() + ", receiptNumber=" + this.getReceiptNumber() + ", issueDate=" + this.getIssueDate() + ", receiptType=" + this.getReceiptType() + ", creditDebitFlag=" + this.getCreditDebitFlag() + ", cashTransferFlag=" + this.getCashTransferFlag() + ", bookkeepingDate=" + this.getBookkeepingDate() + ", bookkeepingTime=" + this.getBookkeepingTime() + ", bookkeeper=" + this.getBookkeeper() + ", transactionAmount=" + this.getTransactionAmount() + ", currency=" + this.getCurrency() + ", payerAccountName=" + this.getPayerAccountName() + ", payerAccountNumber=" + this.getPayerAccountNumber() + ", payerOpeningBank=" + this.getPayerOpeningBank() + ", payeeAccountName=" + this.getPayeeAccountName() + ", payeeAccountNumber=" + this.getPayeeAccountNumber() + ", payeeOpeningBank=" + this.getPayeeOpeningBank() + ", sourceDocType=" + this.getSourceDocType() + ", sourceDocNumber=" + this.getSourceDocNumber() + ", transactionCode=" + this.getTransactionCode() + ", usage=" + this.getUsage() + ", notes=" + this.getNotes() + ", identifyingCode=" + this.getIdentifyingCode() + ", createdAt=" + this.getCreatedAt() + ", updatedAt=" + this.getUpdatedAt() + ", deleted=" + this.getDeleted() + ")";
    }
}
