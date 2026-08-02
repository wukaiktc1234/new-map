package com.foodtraceability.dto.finance;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 自动凭证VO
 * 展示自动生成的记账凭证信息，包含分录明细
 */
public class AutoVoucherVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 凭证ID */
    private Long voucherId;

    /** 凭证号 */
    private String voucherNo;

    /** 来源事件名称 */
    private String sourceEvent;

    /** 来源事件ID */
    private Long sourceEventId;

    /** 凭证日期 */
    private LocalDate voucherDate;

    /** 分录列表 */
    private List<VoucherEntryVO> entries;

    /** 借方合计（单位：分） */
    private Long totalDebit;

    /** 贷方合计（单位：分） */
    private Long totalCredit;

    /** 凭证状态（pending/reviewed/posted/voided） */
    private String status;

    /** 审核人 */
    private String reviewedBy;

    /** 审核时间 */
    private LocalDateTime reviewedAt;

    /** 过账人 */
    private String postedBy;

    /** 过账时间 */
    private LocalDateTime postedAt;

    /** 创建时间 */
    private LocalDateTime createdAt;

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

    public String getSourceEvent() {
        return sourceEvent;
    }

    public void setSourceEvent(String sourceEvent) {
        this.sourceEvent = sourceEvent;
    }

    public Long getSourceEventId() {
        return sourceEventId;
    }

    public void setSourceEventId(Long sourceEventId) {
        this.sourceEventId = sourceEventId;
    }

    public LocalDate getVoucherDate() {
        return voucherDate;
    }

    public void setVoucherDate(LocalDate voucherDate) {
        this.voucherDate = voucherDate;
    }

    public List<VoucherEntryVO> getEntries() {
        return entries;
    }

    public void setEntries(List<VoucherEntryVO> entries) {
        this.entries = entries;
    }

    public Long getTotalDebit() {
        return totalDebit;
    }

    public void setTotalDebit(Long totalDebit) {
        this.totalDebit = totalDebit;
    }

    public Long getTotalCredit() {
        return totalCredit;
    }

    public void setTotalCredit(Long totalCredit) {
        this.totalCredit = totalCredit;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getPostedBy() {
        return postedBy;
    }

    public void setPostedBy(String postedBy) {
        this.postedBy = postedBy;
    }

    public LocalDateTime getPostedAt() {
        return postedAt;
    }

    public void setPostedAt(LocalDateTime postedAt) {
        this.postedAt = postedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 凭证分录VO
     */
    public static class VoucherEntryVO implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 分录ID */
        private Long entryId;

        /** 科目编码 */
        private String accountCode;

        /** 科目名称 */
        private String accountName;

        /** 借方金额（单位：分） */
        private Long debitAmount;

        /** 贷方金额（单位：分） */
        private Long creditAmount;

        /** 摘要 */
        private String description;

        /** 辅助信息 */
        private java.util.Map<String, String> auxiliaryInfo;

        public Long getEntryId() {
            return entryId;
        }

        public void setEntryId(Long entryId) {
            this.entryId = entryId;
        }

        public String getAccountCode() {
            return accountCode;
        }

        public void setAccountCode(String accountCode) {
            this.accountCode = accountCode;
        }

        public String getAccountName() {
            return accountName;
        }

        public void setAccountName(String accountName) {
            this.accountName = accountName;
        }

        public Long getDebitAmount() {
            return debitAmount;
        }

        public void setDebitAmount(Long debitAmount) {
            this.debitAmount = debitAmount;
        }

        public Long getCreditAmount() {
            return creditAmount;
        }

        public void setCreditAmount(Long creditAmount) {
            this.creditAmount = creditAmount;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public java.util.Map<String, String> getAuxiliaryInfo() {
            return auxiliaryInfo;
        }

        public void setAuxiliaryInfo(java.util.Map<String, String> auxiliaryInfo) {
            this.auxiliaryInfo = auxiliaryInfo;
        }
    }
}
