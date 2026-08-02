package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/**
 * 财务凭证DTO
 */
@Schema(description = "财务凭证")
public class FinanceVoucherDTO {
    @Schema(description = "凭证ID")
    private Long id;
    @Schema(description = "凭证字")
    private String voucherWord;
    @Schema(description = "凭证号")
    private String voucherNo;
    @Schema(description = "凭证日期")
    private LocalDate voucherDate;
    @Schema(description = "会计期间")
    private String accountingPeriod;
    @Schema(description = "附件张数")
    private Integer attachmentCount;
    @Schema(description = "来源类型")
    private String sourceType;
    @Schema(description = "来源ID")
    private Long sourceId;
    @Schema(description = "摘要")
    private String remark;
    @Schema(description = "借方合计（单位：分）")
    private Long totalDebitAmount;
    @Schema(description = "贷方合计（单位：分）")
    private Long totalCreditAmount;
    @Schema(description = "状态")
    private String status;
    @Schema(description = "凭证明细")
    private List<FinanceVoucherDetailDTO> details;

    public FinanceVoucherDTO() {
    }

    public Long getId() {
        return this.id;
    }

    public String getVoucherWord() {
        return this.voucherWord;
    }

    public String getVoucherNo() {
        return this.voucherNo;
    }

    public LocalDate getVoucherDate() {
        return this.voucherDate;
    }

    public String getAccountingPeriod() {
        return this.accountingPeriod;
    }

    public Integer getAttachmentCount() {
        return this.attachmentCount;
    }

    public String getSourceType() {
        return this.sourceType;
    }

    public Long getSourceId() {
        return this.sourceId;
    }

    public String getRemark() {
        return this.remark;
    }

    public Long getTotalDebitAmount() {
        return this.totalDebitAmount;
    }

    public Long getTotalCreditAmount() {
        return this.totalCreditAmount;
    }

    public String getStatus() {
        return this.status;
    }

    public List<FinanceVoucherDetailDTO> getDetails() {
        return this.details;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setVoucherWord(final String voucherWord) {
        this.voucherWord = voucherWord;
    }

    public void setVoucherNo(final String voucherNo) {
        this.voucherNo = voucherNo;
    }

    public void setVoucherDate(final LocalDate voucherDate) {
        this.voucherDate = voucherDate;
    }

    public void setAccountingPeriod(final String accountingPeriod) {
        this.accountingPeriod = accountingPeriod;
    }

    public void setAttachmentCount(final Integer attachmentCount) {
        this.attachmentCount = attachmentCount;
    }

    public void setSourceType(final String sourceType) {
        this.sourceType = sourceType;
    }

    public void setSourceId(final Long sourceId) {
        this.sourceId = sourceId;
    }

    public void setRemark(final String remark) {
        this.remark = remark;
    }

    public void setTotalDebitAmount(final Long totalDebitAmount) {
        this.totalDebitAmount = totalDebitAmount;
    }

    public void setTotalCreditAmount(final Long totalCreditAmount) {
        this.totalCreditAmount = totalCreditAmount;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public void setDetails(final List<FinanceVoucherDetailDTO> details) {
        this.details = details;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof FinanceVoucherDTO)) return false;
        final FinanceVoucherDTO other = (FinanceVoucherDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$attachmentCount = this.getAttachmentCount();
        final java.lang.Object other$attachmentCount = other.getAttachmentCount();
        if (this$attachmentCount == null ? other$attachmentCount != null : !this$attachmentCount.equals(other$attachmentCount)) return false;
        final java.lang.Object this$sourceId = this.getSourceId();
        final java.lang.Object other$sourceId = other.getSourceId();
        if (this$sourceId == null ? other$sourceId != null : !this$sourceId.equals(other$sourceId)) return false;
        final java.lang.Object this$voucherWord = this.getVoucherWord();
        final java.lang.Object other$voucherWord = other.getVoucherWord();
        if (this$voucherWord == null ? other$voucherWord != null : !this$voucherWord.equals(other$voucherWord)) return false;
        final java.lang.Object this$voucherNo = this.getVoucherNo();
        final java.lang.Object other$voucherNo = other.getVoucherNo();
        if (this$voucherNo == null ? other$voucherNo != null : !this$voucherNo.equals(other$voucherNo)) return false;
        final java.lang.Object this$voucherDate = this.getVoucherDate();
        final java.lang.Object other$voucherDate = other.getVoucherDate();
        if (this$voucherDate == null ? other$voucherDate != null : !this$voucherDate.equals(other$voucherDate)) return false;
        final java.lang.Object this$accountingPeriod = this.getAccountingPeriod();
        final java.lang.Object other$accountingPeriod = other.getAccountingPeriod();
        if (this$accountingPeriod == null ? other$accountingPeriod != null : !this$accountingPeriod.equals(other$accountingPeriod)) return false;
        final java.lang.Object this$sourceType = this.getSourceType();
        final java.lang.Object other$sourceType = other.getSourceType();
        if (this$sourceType == null ? other$sourceType != null : !this$sourceType.equals(other$sourceType)) return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) return false;
        final java.lang.Object this$totalDebitAmount = this.getTotalDebitAmount();
        final java.lang.Object other$totalDebitAmount = other.getTotalDebitAmount();
        if (this$totalDebitAmount == null ? other$totalDebitAmount != null : !this$totalDebitAmount.equals(other$totalDebitAmount)) return false;
        final java.lang.Object this$totalCreditAmount = this.getTotalCreditAmount();
        final java.lang.Object other$totalCreditAmount = other.getTotalCreditAmount();
        if (this$totalCreditAmount == null ? other$totalCreditAmount != null : !this$totalCreditAmount.equals(other$totalCreditAmount)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$details = this.getDetails();
        final java.lang.Object other$details = other.getDetails();
        if (this$details == null ? other$details != null : !this$details.equals(other$details)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof FinanceVoucherDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $attachmentCount = this.getAttachmentCount();
        result = result * PRIME + ($attachmentCount == null ? 43 : $attachmentCount.hashCode());
        final java.lang.Object $sourceId = this.getSourceId();
        result = result * PRIME + ($sourceId == null ? 43 : $sourceId.hashCode());
        final java.lang.Object $voucherWord = this.getVoucherWord();
        result = result * PRIME + ($voucherWord == null ? 43 : $voucherWord.hashCode());
        final java.lang.Object $voucherNo = this.getVoucherNo();
        result = result * PRIME + ($voucherNo == null ? 43 : $voucherNo.hashCode());
        final java.lang.Object $voucherDate = this.getVoucherDate();
        result = result * PRIME + ($voucherDate == null ? 43 : $voucherDate.hashCode());
        final java.lang.Object $accountingPeriod = this.getAccountingPeriod();
        result = result * PRIME + ($accountingPeriod == null ? 43 : $accountingPeriod.hashCode());
        final java.lang.Object $sourceType = this.getSourceType();
        result = result * PRIME + ($sourceType == null ? 43 : $sourceType.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        final java.lang.Object $totalDebitAmount = this.getTotalDebitAmount();
        result = result * PRIME + ($totalDebitAmount == null ? 43 : $totalDebitAmount.hashCode());
        final java.lang.Object $totalCreditAmount = this.getTotalCreditAmount();
        result = result * PRIME + ($totalCreditAmount == null ? 43 : $totalCreditAmount.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $details = this.getDetails();
        result = result * PRIME + ($details == null ? 43 : $details.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "FinanceVoucherDTO(id=" + this.getId() + ", voucherWord=" + this.getVoucherWord() + ", voucherNo=" + this.getVoucherNo() + ", voucherDate=" + this.getVoucherDate() + ", accountingPeriod=" + this.getAccountingPeriod() + ", attachmentCount=" + this.getAttachmentCount() + ", sourceType=" + this.getSourceType() + ", sourceId=" + this.getSourceId() + ", remark=" + this.getRemark() + ", totalDebitAmount=" + this.getTotalDebitAmount() + ", totalCreditAmount=" + this.getTotalCreditAmount() + ", status=" + this.getStatus() + ", details=" + this.getDetails() + ")";
    }
}
