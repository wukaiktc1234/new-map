package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 生成记账凭证请求DTO
 */
@Schema(description = "生成记账凭证请求")
public class VoucherGenerateRequestDTO {
    @NotNull(message = "会计期间ID不能为空")
    @Schema(description = "会计期间ID")
    private Long accountingPeriodId;
    @Schema(description = "会计期间，格式：yyyy-MM")
    private String accountingPeriod;
    @Schema(description = "入账日期")
    private LocalDate postingDate;
    @NotEmpty(message = "凭证字不能为空")
    @Size(max = 10, message = "凭证字最长10个字符")
    @Schema(description = "凭证字")
    private String voucherWord;
    @Schema(description = "附件数量")
    private Integer attachmentCount;
    @Size(max = 500, message = "备注最长500个字符")
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "摘要")
    private String summary;
    @Schema(description = "部门ID")
    private Long departmentId;
    @Schema(description = "项目ID")
    private Long projectId;
    @Schema(description = "经办人ID")
    private Long handlerId;
    @Schema(description = "是否自动审核")
    private Boolean autoReview;
    @Valid
    @NotEmpty(message = "凭证明细不能为空")
    @Schema(description = "凭证明细行")
    private List<VoucherLineDTO> lines;


    /**
     * 凭证明细行DTO
     */
    @Schema(description = "凭证明细行")
    public static class VoucherLineDTO {
        @NotNull(message = "科目ID不能为空")
        @Schema(description = "科目ID")
        private Long subjectId;
        @Schema(description = "借方金额")
        private BigDecimal debitAmount;
        @Schema(description = "贷方金额")
        private BigDecimal creditAmount;
        @Size(max = 200, message = "摘要最长200个字符")
        @Schema(description = "摘要")
        private String summary;
        @Schema(description = "辅助核算信息JSON")
        private String auxiliaryData;

        public VoucherLineDTO() {
        }

        public Long getSubjectId() {
            return this.subjectId;
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

        public String getAuxiliaryData() {
            return this.auxiliaryData;
        }

        public void setSubjectId(final Long subjectId) {
            this.subjectId = subjectId;
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

        public void setAuxiliaryData(final String auxiliaryData) {
            this.auxiliaryData = auxiliaryData;
        }

        @java.lang.Override
        public boolean equals(final java.lang.Object o) {
            if (o == this) return true;
            if (!(o instanceof VoucherGenerateRequestDTO.VoucherLineDTO)) return false;
            final VoucherGenerateRequestDTO.VoucherLineDTO other = (VoucherGenerateRequestDTO.VoucherLineDTO) o;
            if (!other.canEqual((java.lang.Object) this)) return false;
            final java.lang.Object this$subjectId = this.getSubjectId();
            final java.lang.Object other$subjectId = other.getSubjectId();
            if (this$subjectId == null ? other$subjectId != null : !this$subjectId.equals(other$subjectId)) return false;
            final java.lang.Object this$debitAmount = this.getDebitAmount();
            final java.lang.Object other$debitAmount = other.getDebitAmount();
            if (this$debitAmount == null ? other$debitAmount != null : !this$debitAmount.equals(other$debitAmount)) return false;
            final java.lang.Object this$creditAmount = this.getCreditAmount();
            final java.lang.Object other$creditAmount = other.getCreditAmount();
            if (this$creditAmount == null ? other$creditAmount != null : !this$creditAmount.equals(other$creditAmount)) return false;
            final java.lang.Object this$summary = this.getSummary();
            final java.lang.Object other$summary = other.getSummary();
            if (this$summary == null ? other$summary != null : !this$summary.equals(other$summary)) return false;
            final java.lang.Object this$auxiliaryData = this.getAuxiliaryData();
            final java.lang.Object other$auxiliaryData = other.getAuxiliaryData();
            if (this$auxiliaryData == null ? other$auxiliaryData != null : !this$auxiliaryData.equals(other$auxiliaryData)) return false;
            return true;
        }

        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof VoucherGenerateRequestDTO.VoucherLineDTO;
        }

        @java.lang.Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final java.lang.Object $subjectId = this.getSubjectId();
            result = result * PRIME + ($subjectId == null ? 43 : $subjectId.hashCode());
            final java.lang.Object $debitAmount = this.getDebitAmount();
            result = result * PRIME + ($debitAmount == null ? 43 : $debitAmount.hashCode());
            final java.lang.Object $creditAmount = this.getCreditAmount();
            result = result * PRIME + ($creditAmount == null ? 43 : $creditAmount.hashCode());
            final java.lang.Object $summary = this.getSummary();
            result = result * PRIME + ($summary == null ? 43 : $summary.hashCode());
            final java.lang.Object $auxiliaryData = this.getAuxiliaryData();
            result = result * PRIME + ($auxiliaryData == null ? 43 : $auxiliaryData.hashCode());
            return result;
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "VoucherGenerateRequestDTO.VoucherLineDTO(subjectId=" + this.getSubjectId() + ", debitAmount=" + this.getDebitAmount() + ", creditAmount=" + this.getCreditAmount() + ", summary=" + this.getSummary() + ", auxiliaryData=" + this.getAuxiliaryData() + ")";
        }
    }

    public VoucherGenerateRequestDTO() {
    }

    public Long getAccountingPeriodId() {
        return this.accountingPeriodId;
    }

    public String getAccountingPeriod() {
        return this.accountingPeriod;
    }

    public LocalDate getPostingDate() {
        return this.postingDate;
    }

    public String getVoucherWord() {
        return this.voucherWord;
    }

    public Integer getAttachmentCount() {
        return this.attachmentCount;
    }

    public String getRemark() {
        return this.remark;
    }

    public String getSummary() {
        return this.summary;
    }

    public Long getDepartmentId() {
        return this.departmentId;
    }

    public Long getProjectId() {
        return this.projectId;
    }

    public Long getHandlerId() {
        return this.handlerId;
    }

    public Boolean getAutoReview() {
        return this.autoReview;
    }

    public List<VoucherLineDTO> getLines() {
        return this.lines;
    }

    public void setAccountingPeriodId(final Long accountingPeriodId) {
        this.accountingPeriodId = accountingPeriodId;
    }

    public void setAccountingPeriod(final String accountingPeriod) {
        this.accountingPeriod = accountingPeriod;
    }

    public void setPostingDate(final LocalDate postingDate) {
        this.postingDate = postingDate;
    }

    public void setVoucherWord(final String voucherWord) {
        this.voucherWord = voucherWord;
    }

    public void setAttachmentCount(final Integer attachmentCount) {
        this.attachmentCount = attachmentCount;
    }

    public void setRemark(final String remark) {
        this.remark = remark;
    }

    public void setSummary(final String summary) {
        this.summary = summary;
    }

    public void setDepartmentId(final Long departmentId) {
        this.departmentId = departmentId;
    }

    public void setProjectId(final Long projectId) {
        this.projectId = projectId;
    }

    public void setHandlerId(final Long handlerId) {
        this.handlerId = handlerId;
    }

    public void setAutoReview(final Boolean autoReview) {
        this.autoReview = autoReview;
    }

    public void setLines(final List<VoucherLineDTO> lines) {
        this.lines = lines;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof VoucherGenerateRequestDTO)) return false;
        final VoucherGenerateRequestDTO other = (VoucherGenerateRequestDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$accountingPeriodId = this.getAccountingPeriodId();
        final java.lang.Object other$accountingPeriodId = other.getAccountingPeriodId();
        if (this$accountingPeriodId == null ? other$accountingPeriodId != null : !this$accountingPeriodId.equals(other$accountingPeriodId)) return false;
        final java.lang.Object this$attachmentCount = this.getAttachmentCount();
        final java.lang.Object other$attachmentCount = other.getAttachmentCount();
        if (this$attachmentCount == null ? other$attachmentCount != null : !this$attachmentCount.equals(other$attachmentCount)) return false;
        final java.lang.Object this$departmentId = this.getDepartmentId();
        final java.lang.Object other$departmentId = other.getDepartmentId();
        if (this$departmentId == null ? other$departmentId != null : !this$departmentId.equals(other$departmentId)) return false;
        final java.lang.Object this$projectId = this.getProjectId();
        final java.lang.Object other$projectId = other.getProjectId();
        if (this$projectId == null ? other$projectId != null : !this$projectId.equals(other$projectId)) return false;
        final java.lang.Object this$handlerId = this.getHandlerId();
        final java.lang.Object other$handlerId = other.getHandlerId();
        if (this$handlerId == null ? other$handlerId != null : !this$handlerId.equals(other$handlerId)) return false;
        final java.lang.Object this$autoReview = this.getAutoReview();
        final java.lang.Object other$autoReview = other.getAutoReview();
        if (this$autoReview == null ? other$autoReview != null : !this$autoReview.equals(other$autoReview)) return false;
        final java.lang.Object this$accountingPeriod = this.getAccountingPeriod();
        final java.lang.Object other$accountingPeriod = other.getAccountingPeriod();
        if (this$accountingPeriod == null ? other$accountingPeriod != null : !this$accountingPeriod.equals(other$accountingPeriod)) return false;
        final java.lang.Object this$postingDate = this.getPostingDate();
        final java.lang.Object other$postingDate = other.getPostingDate();
        if (this$postingDate == null ? other$postingDate != null : !this$postingDate.equals(other$postingDate)) return false;
        final java.lang.Object this$voucherWord = this.getVoucherWord();
        final java.lang.Object other$voucherWord = other.getVoucherWord();
        if (this$voucherWord == null ? other$voucherWord != null : !this$voucherWord.equals(other$voucherWord)) return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) return false;
        final java.lang.Object this$summary = this.getSummary();
        final java.lang.Object other$summary = other.getSummary();
        if (this$summary == null ? other$summary != null : !this$summary.equals(other$summary)) return false;
        final java.lang.Object this$lines = this.getLines();
        final java.lang.Object other$lines = other.getLines();
        if (this$lines == null ? other$lines != null : !this$lines.equals(other$lines)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof VoucherGenerateRequestDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $accountingPeriodId = this.getAccountingPeriodId();
        result = result * PRIME + ($accountingPeriodId == null ? 43 : $accountingPeriodId.hashCode());
        final java.lang.Object $attachmentCount = this.getAttachmentCount();
        result = result * PRIME + ($attachmentCount == null ? 43 : $attachmentCount.hashCode());
        final java.lang.Object $departmentId = this.getDepartmentId();
        result = result * PRIME + ($departmentId == null ? 43 : $departmentId.hashCode());
        final java.lang.Object $projectId = this.getProjectId();
        result = result * PRIME + ($projectId == null ? 43 : $projectId.hashCode());
        final java.lang.Object $handlerId = this.getHandlerId();
        result = result * PRIME + ($handlerId == null ? 43 : $handlerId.hashCode());
        final java.lang.Object $autoReview = this.getAutoReview();
        result = result * PRIME + ($autoReview == null ? 43 : $autoReview.hashCode());
        final java.lang.Object $accountingPeriod = this.getAccountingPeriod();
        result = result * PRIME + ($accountingPeriod == null ? 43 : $accountingPeriod.hashCode());
        final java.lang.Object $postingDate = this.getPostingDate();
        result = result * PRIME + ($postingDate == null ? 43 : $postingDate.hashCode());
        final java.lang.Object $voucherWord = this.getVoucherWord();
        result = result * PRIME + ($voucherWord == null ? 43 : $voucherWord.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        final java.lang.Object $summary = this.getSummary();
        result = result * PRIME + ($summary == null ? 43 : $summary.hashCode());
        final java.lang.Object $lines = this.getLines();
        result = result * PRIME + ($lines == null ? 43 : $lines.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "VoucherGenerateRequestDTO(accountingPeriodId=" + this.getAccountingPeriodId() + ", accountingPeriod=" + this.getAccountingPeriod() + ", postingDate=" + this.getPostingDate() + ", voucherWord=" + this.getVoucherWord() + ", attachmentCount=" + this.getAttachmentCount() + ", remark=" + this.getRemark() + ", summary=" + this.getSummary() + ", departmentId=" + this.getDepartmentId() + ", projectId=" + this.getProjectId() + ", handlerId=" + this.getHandlerId() + ", autoReview=" + this.getAutoReview() + ", lines=" + this.getLines() + ")";
    }
}
