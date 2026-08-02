package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 电子凭证入账请求DTO
 *
 * 依据：财政部《电子凭证会计数据标准应用指南(推广应用版1.0)》
 */
@Schema(description = "电子凭证入账请求")
public class VoucherAccountingRequestDTO {
    @NotNull(message = "会计期间不能为空")
    @Schema(description = "会计期间，格式：yyyy-MM")
    private String accountingPeriod;
    @NotNull(message = "入账日期不能为空")
    @Schema(description = "入账日期")
    private LocalDate postingDate;
    @Schema(description = "凭证字")
    private String voucherWord;
    @Schema(description = "附件数量")
    private Integer attachmentCount;
    @Schema(description = "自定义摘要")
    private String customSummary;
    @Schema(description = "部门ID")
    private Long departmentId;
    @Schema(description = "项目ID")
    private Long projectId;
    @Schema(description = "经办人ID")
    private Long handlerId;
    @Schema(description = "是否自动审核")
    private Boolean autoReview;
    @Schema(description = "会计分录列表")
    private List<AccountingEntryDTO> entries;


    @Schema(description = "会计分录")
    public static class AccountingEntryDTO {
        @NotBlank(message = "科目编码不能为空")
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
        @Schema(description = "辅助核算-部门ID")
        private Long auxiliaryDeptId;
        @Schema(description = "辅助核算-项目ID")
        private Long auxiliaryProjectId;
        @Schema(description = "辅助核算-客户ID")
        private Long auxiliaryCustomerId;
        @Schema(description = "辅助核算-供应商ID")
        private Long auxiliarySupplierId;
        @Schema(description = "辅助核算-员工ID")
        private Long auxiliaryEmployeeId;

        public AccountingEntryDTO() {
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

        public Long getAuxiliaryDeptId() {
            return this.auxiliaryDeptId;
        }

        public Long getAuxiliaryProjectId() {
            return this.auxiliaryProjectId;
        }

        public Long getAuxiliaryCustomerId() {
            return this.auxiliaryCustomerId;
        }

        public Long getAuxiliarySupplierId() {
            return this.auxiliarySupplierId;
        }

        public Long getAuxiliaryEmployeeId() {
            return this.auxiliaryEmployeeId;
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

        public void setAuxiliaryDeptId(final Long auxiliaryDeptId) {
            this.auxiliaryDeptId = auxiliaryDeptId;
        }

        public void setAuxiliaryProjectId(final Long auxiliaryProjectId) {
            this.auxiliaryProjectId = auxiliaryProjectId;
        }

        public void setAuxiliaryCustomerId(final Long auxiliaryCustomerId) {
            this.auxiliaryCustomerId = auxiliaryCustomerId;
        }

        public void setAuxiliarySupplierId(final Long auxiliarySupplierId) {
            this.auxiliarySupplierId = auxiliarySupplierId;
        }

        public void setAuxiliaryEmployeeId(final Long auxiliaryEmployeeId) {
            this.auxiliaryEmployeeId = auxiliaryEmployeeId;
        }

        @java.lang.Override
        public boolean equals(final java.lang.Object o) {
            if (o == this) return true;
            if (!(o instanceof VoucherAccountingRequestDTO.AccountingEntryDTO)) return false;
            final VoucherAccountingRequestDTO.AccountingEntryDTO other = (VoucherAccountingRequestDTO.AccountingEntryDTO) o;
            if (!other.canEqual((java.lang.Object) this)) return false;
            final java.lang.Object this$auxiliaryDeptId = this.getAuxiliaryDeptId();
            final java.lang.Object other$auxiliaryDeptId = other.getAuxiliaryDeptId();
            if (this$auxiliaryDeptId == null ? other$auxiliaryDeptId != null : !this$auxiliaryDeptId.equals(other$auxiliaryDeptId)) return false;
            final java.lang.Object this$auxiliaryProjectId = this.getAuxiliaryProjectId();
            final java.lang.Object other$auxiliaryProjectId = other.getAuxiliaryProjectId();
            if (this$auxiliaryProjectId == null ? other$auxiliaryProjectId != null : !this$auxiliaryProjectId.equals(other$auxiliaryProjectId)) return false;
            final java.lang.Object this$auxiliaryCustomerId = this.getAuxiliaryCustomerId();
            final java.lang.Object other$auxiliaryCustomerId = other.getAuxiliaryCustomerId();
            if (this$auxiliaryCustomerId == null ? other$auxiliaryCustomerId != null : !this$auxiliaryCustomerId.equals(other$auxiliaryCustomerId)) return false;
            final java.lang.Object this$auxiliarySupplierId = this.getAuxiliarySupplierId();
            final java.lang.Object other$auxiliarySupplierId = other.getAuxiliarySupplierId();
            if (this$auxiliarySupplierId == null ? other$auxiliarySupplierId != null : !this$auxiliarySupplierId.equals(other$auxiliarySupplierId)) return false;
            final java.lang.Object this$auxiliaryEmployeeId = this.getAuxiliaryEmployeeId();
            final java.lang.Object other$auxiliaryEmployeeId = other.getAuxiliaryEmployeeId();
            if (this$auxiliaryEmployeeId == null ? other$auxiliaryEmployeeId != null : !this$auxiliaryEmployeeId.equals(other$auxiliaryEmployeeId)) return false;
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
            return true;
        }

        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof VoucherAccountingRequestDTO.AccountingEntryDTO;
        }

        @java.lang.Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final java.lang.Object $auxiliaryDeptId = this.getAuxiliaryDeptId();
            result = result * PRIME + ($auxiliaryDeptId == null ? 43 : $auxiliaryDeptId.hashCode());
            final java.lang.Object $auxiliaryProjectId = this.getAuxiliaryProjectId();
            result = result * PRIME + ($auxiliaryProjectId == null ? 43 : $auxiliaryProjectId.hashCode());
            final java.lang.Object $auxiliaryCustomerId = this.getAuxiliaryCustomerId();
            result = result * PRIME + ($auxiliaryCustomerId == null ? 43 : $auxiliaryCustomerId.hashCode());
            final java.lang.Object $auxiliarySupplierId = this.getAuxiliarySupplierId();
            result = result * PRIME + ($auxiliarySupplierId == null ? 43 : $auxiliarySupplierId.hashCode());
            final java.lang.Object $auxiliaryEmployeeId = this.getAuxiliaryEmployeeId();
            result = result * PRIME + ($auxiliaryEmployeeId == null ? 43 : $auxiliaryEmployeeId.hashCode());
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
            return result;
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "VoucherAccountingRequestDTO.AccountingEntryDTO(accountCode=" + this.getAccountCode() + ", accountName=" + this.getAccountName() + ", debitAmount=" + this.getDebitAmount() + ", creditAmount=" + this.getCreditAmount() + ", summary=" + this.getSummary() + ", auxiliaryDeptId=" + this.getAuxiliaryDeptId() + ", auxiliaryProjectId=" + this.getAuxiliaryProjectId() + ", auxiliaryCustomerId=" + this.getAuxiliaryCustomerId() + ", auxiliarySupplierId=" + this.getAuxiliarySupplierId() + ", auxiliaryEmployeeId=" + this.getAuxiliaryEmployeeId() + ")";
        }
    }

    public VoucherAccountingRequestDTO() {
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

    public String getCustomSummary() {
        return this.customSummary;
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

    public List<AccountingEntryDTO> getEntries() {
        return this.entries;
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

    public void setCustomSummary(final String customSummary) {
        this.customSummary = customSummary;
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

    public void setEntries(final List<AccountingEntryDTO> entries) {
        this.entries = entries;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof VoucherAccountingRequestDTO)) return false;
        final VoucherAccountingRequestDTO other = (VoucherAccountingRequestDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
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
        final java.lang.Object this$customSummary = this.getCustomSummary();
        final java.lang.Object other$customSummary = other.getCustomSummary();
        if (this$customSummary == null ? other$customSummary != null : !this$customSummary.equals(other$customSummary)) return false;
        final java.lang.Object this$entries = this.getEntries();
        final java.lang.Object other$entries = other.getEntries();
        if (this$entries == null ? other$entries != null : !this$entries.equals(other$entries)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof VoucherAccountingRequestDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
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
        final java.lang.Object $customSummary = this.getCustomSummary();
        result = result * PRIME + ($customSummary == null ? 43 : $customSummary.hashCode());
        final java.lang.Object $entries = this.getEntries();
        result = result * PRIME + ($entries == null ? 43 : $entries.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "VoucherAccountingRequestDTO(accountingPeriod=" + this.getAccountingPeriod() + ", postingDate=" + this.getPostingDate() + ", voucherWord=" + this.getVoucherWord() + ", attachmentCount=" + this.getAttachmentCount() + ", customSummary=" + this.getCustomSummary() + ", departmentId=" + this.getDepartmentId() + ", projectId=" + this.getProjectId() + ", handlerId=" + this.getHandlerId() + ", autoReview=" + this.getAutoReview() + ", entries=" + this.getEntries() + ")";
    }
}
