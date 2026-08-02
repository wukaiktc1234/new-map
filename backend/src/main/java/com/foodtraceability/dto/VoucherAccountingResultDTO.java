package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 电子凭证入账结果DTO
 *
 * 依据：财政部《电子凭证会计数据标准应用指南(推广应用版1.0)》
 */
@Schema(description = "电子凭证入账结果")
public class VoucherAccountingResultDTO {
    @Schema(description = "是否成功")
    private Boolean success;
    @Schema(description = "电子凭证ID")
    private Long electronicVoucherId;
    @Schema(description = "电子凭证编号")
    private String electronicVoucherNo;
    @Schema(description = "生成的记账凭证ID")
    private Long financeVoucherId;
    @Schema(description = "记账凭证号")
    private String financeVoucherNo;
    @Schema(description = "凭证字")
    private String voucherWord;
    @Schema(description = "凭证号")
    private String voucherNumber;
    @Schema(description = "会计期间")
    private String accountingPeriod;
    @Schema(description = "入账日期")
    private LocalDate postingDate;
    @Schema(description = "总金额")
    private BigDecimal totalAmount;
    @Schema(description = "借方金额合计")
    private BigDecimal totalDebit;
    @Schema(description = "贷方金额合计")
    private BigDecimal totalCredit;
    @Schema(description = "会计分录列表")
    private List<AccountingEntryResultDTO> entries;
    @Schema(description = "错误信息")
    private String errorMessage;
    @Schema(description = "入账时间")
    private LocalDateTime accountingTime;
    @Schema(description = "是否预览模式")
    private Boolean preview;


    @Schema(description = "会计分录结果")
    public static class AccountingEntryResultDTO {
        @Schema(description = "分录序号")
        private Integer entryNo;
        @Schema(description = "科目编码")
        private String accountCode;
        @Schema(description = "科目名称")
        private String accountName;
        @Schema(description = "借方金额")
        private BigDecimal debitAmount;
        @Schema(description = "贷方金额")
        private BigDecimal creditAmount;
        @Schema(description = "摘要")
        private String summary;
        @Schema(description = "辅助核算信息")
        private String auxiliaryInfo;

        public AccountingEntryResultDTO() {
        }

        public Integer getEntryNo() {
            return this.entryNo;
        }

        public String getAccountCode() {
            return this.accountCode;
        }

        public String getAccountName() {
            return this.accountName;
        }

        public BigDecimal getDebitAmount() {
            return this.debitAmount;
        }

        public BigDecimal getCreditAmount() {
            return this.creditAmount;
        }

        public String getSummary() {
            return this.summary;
        }

        public String getAuxiliaryInfo() {
            return this.auxiliaryInfo;
        }

        public void setEntryNo(final Integer entryNo) {
            this.entryNo = entryNo;
        }

        public void setAccountCode(final String accountCode) {
            this.accountCode = accountCode;
        }

        public void setAccountName(final String accountName) {
            this.accountName = accountName;
        }

        public void setDebitAmount(final BigDecimal debitAmount) {
            this.debitAmount = debitAmount;
        }

        public void setCreditAmount(final BigDecimal creditAmount) {
            this.creditAmount = creditAmount;
        }

        public void setSummary(final String summary) {
            this.summary = summary;
        }

        public void setAuxiliaryInfo(final String auxiliaryInfo) {
            this.auxiliaryInfo = auxiliaryInfo;
        }

        @java.lang.Override
        public boolean equals(final java.lang.Object o) {
            if (o == this) return true;
            if (!(o instanceof VoucherAccountingResultDTO.AccountingEntryResultDTO)) return false;
            final VoucherAccountingResultDTO.AccountingEntryResultDTO other = (VoucherAccountingResultDTO.AccountingEntryResultDTO) o;
            if (!other.canEqual((java.lang.Object) this)) return false;
            final java.lang.Object this$entryNo = this.getEntryNo();
            final java.lang.Object other$entryNo = other.getEntryNo();
            if (this$entryNo == null ? other$entryNo != null : !this$entryNo.equals(other$entryNo)) return false;
            final java.lang.Object this$accountCode = this.getAccountCode();
            final java.lang.Object other$accountCode = other.getAccountCode();
            if (this$accountCode == null ? other$accountCode != null : !this$accountCode.equals(other$accountCode)) return false;
            final java.lang.Object this$accountName = this.getAccountName();
            final java.lang.Object other$accountName = other.getAccountName();
            if (this$accountName == null ? other$accountName != null : !this$accountName.equals(other$accountName)) return false;
            final java.lang.Object this$debitAmount = this.getDebitAmount();
            final java.lang.Object other$debitAmount = other.getDebitAmount();
            if (this$debitAmount == null ? other$debitAmount != null : !this$debitAmount.equals(other$debitAmount)) return false;
            final java.lang.Object this$creditAmount = this.getCreditAmount();
            final java.lang.Object other$creditAmount = other.getCreditAmount();
            if (this$creditAmount == null ? other$creditAmount != null : !this$creditAmount.equals(other$creditAmount)) return false;
            final java.lang.Object this$summary = this.getSummary();
            final java.lang.Object other$summary = other.getSummary();
            if (this$summary == null ? other$summary != null : !this$summary.equals(other$summary)) return false;
            final java.lang.Object this$auxiliaryInfo = this.getAuxiliaryInfo();
            final java.lang.Object other$auxiliaryInfo = other.getAuxiliaryInfo();
            if (this$auxiliaryInfo == null ? other$auxiliaryInfo != null : !this$auxiliaryInfo.equals(other$auxiliaryInfo)) return false;
            return true;
        }

        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof VoucherAccountingResultDTO.AccountingEntryResultDTO;
        }

        @java.lang.Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final java.lang.Object $entryNo = this.getEntryNo();
            result = result * PRIME + ($entryNo == null ? 43 : $entryNo.hashCode());
            final java.lang.Object $accountCode = this.getAccountCode();
            result = result * PRIME + ($accountCode == null ? 43 : $accountCode.hashCode());
            final java.lang.Object $accountName = this.getAccountName();
            result = result * PRIME + ($accountName == null ? 43 : $accountName.hashCode());
            final java.lang.Object $debitAmount = this.getDebitAmount();
            result = result * PRIME + ($debitAmount == null ? 43 : $debitAmount.hashCode());
            final java.lang.Object $creditAmount = this.getCreditAmount();
            result = result * PRIME + ($creditAmount == null ? 43 : $creditAmount.hashCode());
            final java.lang.Object $summary = this.getSummary();
            result = result * PRIME + ($summary == null ? 43 : $summary.hashCode());
            final java.lang.Object $auxiliaryInfo = this.getAuxiliaryInfo();
            result = result * PRIME + ($auxiliaryInfo == null ? 43 : $auxiliaryInfo.hashCode());
            return result;
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "VoucherAccountingResultDTO.AccountingEntryResultDTO(entryNo=" + this.getEntryNo() + ", accountCode=" + this.getAccountCode() + ", accountName=" + this.getAccountName() + ", debitAmount=" + this.getDebitAmount() + ", creditAmount=" + this.getCreditAmount() + ", summary=" + this.getSummary() + ", auxiliaryInfo=" + this.getAuxiliaryInfo() + ")";
        }
    }

    public static VoucherAccountingResultDTO success(Long electronicVoucherId, String electronicVoucherNo, Long financeVoucherId, String financeVoucherNo) {
        VoucherAccountingResultDTO result = new VoucherAccountingResultDTO();
        result.setSuccess(true);
        result.setElectronicVoucherId(electronicVoucherId);
        result.setElectronicVoucherNo(electronicVoucherNo);
        result.setFinanceVoucherId(financeVoucherId);
        result.setFinanceVoucherNo(financeVoucherNo);
        result.setAccountingTime(LocalDateTime.now());
        result.setPreview(false);
        return result;
    }

    public static VoucherAccountingResultDTO preview(Long electronicVoucherId, String electronicVoucherNo) {
        VoucherAccountingResultDTO result = new VoucherAccountingResultDTO();
        result.setSuccess(true);
        result.setElectronicVoucherId(electronicVoucherId);
        result.setElectronicVoucherNo(electronicVoucherNo);
        result.setPreview(true);
        return result;
    }

    public static VoucherAccountingResultDTO fail(Long electronicVoucherId, String errorMessage) {
        VoucherAccountingResultDTO result = new VoucherAccountingResultDTO();
        result.setSuccess(false);
        result.setElectronicVoucherId(electronicVoucherId);
        result.setErrorMessage(errorMessage);
        result.setAccountingTime(LocalDateTime.now());
        return result;
    }

    public VoucherAccountingResultDTO() {
    }

    public Boolean getSuccess() {
        return this.success;
    }

    public Long getElectronicVoucherId() {
        return this.electronicVoucherId;
    }

    public String getElectronicVoucherNo() {
        return this.electronicVoucherNo;
    }

    public Long getFinanceVoucherId() {
        return this.financeVoucherId;
    }

    public String getFinanceVoucherNo() {
        return this.financeVoucherNo;
    }

    public String getVoucherWord() {
        return this.voucherWord;
    }

    public String getVoucherNumber() {
        return this.voucherNumber;
    }

    public String getAccountingPeriod() {
        return this.accountingPeriod;
    }

    public LocalDate getPostingDate() {
        return this.postingDate;
    }

    public BigDecimal getTotalAmount() {
        return this.totalAmount;
    }

    public BigDecimal getTotalDebit() {
        return this.totalDebit;
    }

    public BigDecimal getTotalCredit() {
        return this.totalCredit;
    }

    public List<AccountingEntryResultDTO> getEntries() {
        return this.entries;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    public LocalDateTime getAccountingTime() {
        return this.accountingTime;
    }

    public Boolean getPreview() {
        return this.preview;
    }

    public void setSuccess(final Boolean success) {
        this.success = success;
    }

    public void setElectronicVoucherId(final Long electronicVoucherId) {
        this.electronicVoucherId = electronicVoucherId;
    }

    public void setElectronicVoucherNo(final String electronicVoucherNo) {
        this.electronicVoucherNo = electronicVoucherNo;
    }

    public void setFinanceVoucherId(final Long financeVoucherId) {
        this.financeVoucherId = financeVoucherId;
    }

    public void setFinanceVoucherNo(final String financeVoucherNo) {
        this.financeVoucherNo = financeVoucherNo;
    }

    public void setVoucherWord(final String voucherWord) {
        this.voucherWord = voucherWord;
    }

    public void setVoucherNumber(final String voucherNumber) {
        this.voucherNumber = voucherNumber;
    }

    public void setAccountingPeriod(final String accountingPeriod) {
        this.accountingPeriod = accountingPeriod;
    }

    public void setPostingDate(final LocalDate postingDate) {
        this.postingDate = postingDate;
    }

    public void setTotalAmount(final BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setTotalDebit(final BigDecimal totalDebit) {
        this.totalDebit = totalDebit;
    }

    public void setTotalCredit(final BigDecimal totalCredit) {
        this.totalCredit = totalCredit;
    }

    public void setEntries(final List<AccountingEntryResultDTO> entries) {
        this.entries = entries;
    }

    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setAccountingTime(final LocalDateTime accountingTime) {
        this.accountingTime = accountingTime;
    }

    public void setPreview(final Boolean preview) {
        this.preview = preview;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof VoucherAccountingResultDTO)) return false;
        final VoucherAccountingResultDTO other = (VoucherAccountingResultDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$success = this.getSuccess();
        final java.lang.Object other$success = other.getSuccess();
        if (this$success == null ? other$success != null : !this$success.equals(other$success)) return false;
        final java.lang.Object this$electronicVoucherId = this.getElectronicVoucherId();
        final java.lang.Object other$electronicVoucherId = other.getElectronicVoucherId();
        if (this$electronicVoucherId == null ? other$electronicVoucherId != null : !this$electronicVoucherId.equals(other$electronicVoucherId)) return false;
        final java.lang.Object this$financeVoucherId = this.getFinanceVoucherId();
        final java.lang.Object other$financeVoucherId = other.getFinanceVoucherId();
        if (this$financeVoucherId == null ? other$financeVoucherId != null : !this$financeVoucherId.equals(other$financeVoucherId)) return false;
        final java.lang.Object this$preview = this.getPreview();
        final java.lang.Object other$preview = other.getPreview();
        if (this$preview == null ? other$preview != null : !this$preview.equals(other$preview)) return false;
        final java.lang.Object this$electronicVoucherNo = this.getElectronicVoucherNo();
        final java.lang.Object other$electronicVoucherNo = other.getElectronicVoucherNo();
        if (this$electronicVoucherNo == null ? other$electronicVoucherNo != null : !this$electronicVoucherNo.equals(other$electronicVoucherNo)) return false;
        final java.lang.Object this$financeVoucherNo = this.getFinanceVoucherNo();
        final java.lang.Object other$financeVoucherNo = other.getFinanceVoucherNo();
        if (this$financeVoucherNo == null ? other$financeVoucherNo != null : !this$financeVoucherNo.equals(other$financeVoucherNo)) return false;
        final java.lang.Object this$voucherWord = this.getVoucherWord();
        final java.lang.Object other$voucherWord = other.getVoucherWord();
        if (this$voucherWord == null ? other$voucherWord != null : !this$voucherWord.equals(other$voucherWord)) return false;
        final java.lang.Object this$voucherNumber = this.getVoucherNumber();
        final java.lang.Object other$voucherNumber = other.getVoucherNumber();
        if (this$voucherNumber == null ? other$voucherNumber != null : !this$voucherNumber.equals(other$voucherNumber)) return false;
        final java.lang.Object this$accountingPeriod = this.getAccountingPeriod();
        final java.lang.Object other$accountingPeriod = other.getAccountingPeriod();
        if (this$accountingPeriod == null ? other$accountingPeriod != null : !this$accountingPeriod.equals(other$accountingPeriod)) return false;
        final java.lang.Object this$postingDate = this.getPostingDate();
        final java.lang.Object other$postingDate = other.getPostingDate();
        if (this$postingDate == null ? other$postingDate != null : !this$postingDate.equals(other$postingDate)) return false;
        final java.lang.Object this$totalAmount = this.getTotalAmount();
        final java.lang.Object other$totalAmount = other.getTotalAmount();
        if (this$totalAmount == null ? other$totalAmount != null : !this$totalAmount.equals(other$totalAmount)) return false;
        final java.lang.Object this$totalDebit = this.getTotalDebit();
        final java.lang.Object other$totalDebit = other.getTotalDebit();
        if (this$totalDebit == null ? other$totalDebit != null : !this$totalDebit.equals(other$totalDebit)) return false;
        final java.lang.Object this$totalCredit = this.getTotalCredit();
        final java.lang.Object other$totalCredit = other.getTotalCredit();
        if (this$totalCredit == null ? other$totalCredit != null : !this$totalCredit.equals(other$totalCredit)) return false;
        final java.lang.Object this$entries = this.getEntries();
        final java.lang.Object other$entries = other.getEntries();
        if (this$entries == null ? other$entries != null : !this$entries.equals(other$entries)) return false;
        final java.lang.Object this$errorMessage = this.getErrorMessage();
        final java.lang.Object other$errorMessage = other.getErrorMessage();
        if (this$errorMessage == null ? other$errorMessage != null : !this$errorMessage.equals(other$errorMessage)) return false;
        final java.lang.Object this$accountingTime = this.getAccountingTime();
        final java.lang.Object other$accountingTime = other.getAccountingTime();
        if (this$accountingTime == null ? other$accountingTime != null : !this$accountingTime.equals(other$accountingTime)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof VoucherAccountingResultDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $success = this.getSuccess();
        result = result * PRIME + ($success == null ? 43 : $success.hashCode());
        final java.lang.Object $electronicVoucherId = this.getElectronicVoucherId();
        result = result * PRIME + ($electronicVoucherId == null ? 43 : $electronicVoucherId.hashCode());
        final java.lang.Object $financeVoucherId = this.getFinanceVoucherId();
        result = result * PRIME + ($financeVoucherId == null ? 43 : $financeVoucherId.hashCode());
        final java.lang.Object $preview = this.getPreview();
        result = result * PRIME + ($preview == null ? 43 : $preview.hashCode());
        final java.lang.Object $electronicVoucherNo = this.getElectronicVoucherNo();
        result = result * PRIME + ($electronicVoucherNo == null ? 43 : $electronicVoucherNo.hashCode());
        final java.lang.Object $financeVoucherNo = this.getFinanceVoucherNo();
        result = result * PRIME + ($financeVoucherNo == null ? 43 : $financeVoucherNo.hashCode());
        final java.lang.Object $voucherWord = this.getVoucherWord();
        result = result * PRIME + ($voucherWord == null ? 43 : $voucherWord.hashCode());
        final java.lang.Object $voucherNumber = this.getVoucherNumber();
        result = result * PRIME + ($voucherNumber == null ? 43 : $voucherNumber.hashCode());
        final java.lang.Object $accountingPeriod = this.getAccountingPeriod();
        result = result * PRIME + ($accountingPeriod == null ? 43 : $accountingPeriod.hashCode());
        final java.lang.Object $postingDate = this.getPostingDate();
        result = result * PRIME + ($postingDate == null ? 43 : $postingDate.hashCode());
        final java.lang.Object $totalAmount = this.getTotalAmount();
        result = result * PRIME + ($totalAmount == null ? 43 : $totalAmount.hashCode());
        final java.lang.Object $totalDebit = this.getTotalDebit();
        result = result * PRIME + ($totalDebit == null ? 43 : $totalDebit.hashCode());
        final java.lang.Object $totalCredit = this.getTotalCredit();
        result = result * PRIME + ($totalCredit == null ? 43 : $totalCredit.hashCode());
        final java.lang.Object $entries = this.getEntries();
        result = result * PRIME + ($entries == null ? 43 : $entries.hashCode());
        final java.lang.Object $errorMessage = this.getErrorMessage();
        result = result * PRIME + ($errorMessage == null ? 43 : $errorMessage.hashCode());
        final java.lang.Object $accountingTime = this.getAccountingTime();
        result = result * PRIME + ($accountingTime == null ? 43 : $accountingTime.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "VoucherAccountingResultDTO(success=" + this.getSuccess() + ", electronicVoucherId=" + this.getElectronicVoucherId() + ", electronicVoucherNo=" + this.getElectronicVoucherNo() + ", financeVoucherId=" + this.getFinanceVoucherId() + ", financeVoucherNo=" + this.getFinanceVoucherNo() + ", voucherWord=" + this.getVoucherWord() + ", voucherNumber=" + this.getVoucherNumber() + ", accountingPeriod=" + this.getAccountingPeriod() + ", postingDate=" + this.getPostingDate() + ", totalAmount=" + this.getTotalAmount() + ", totalDebit=" + this.getTotalDebit() + ", totalCredit=" + this.getTotalCredit() + ", entries=" + this.getEntries() + ", errorMessage=" + this.getErrorMessage() + ", accountingTime=" + this.getAccountingTime() + ", preview=" + this.getPreview() + ")";
    }
}
