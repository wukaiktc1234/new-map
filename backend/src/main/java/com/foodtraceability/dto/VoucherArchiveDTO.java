package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 电子凭证归档DTO
 *
 * 依据：财政部《电子凭证会计数据标准应用指南(推广应用版1.0)》
 */
@Schema(description = "电子凭证归档信息")
public class VoucherArchiveDTO {
    @Schema(description = "归档ID")
    private Long id;
    @Schema(description = "归档编号")
    private String archiveNo;
    @Schema(description = "归档名称")
    private String archiveName;
    @Schema(description = "会计年度")
    private String fiscalYear;
    @Schema(description = "会计期间起")
    private String periodFrom;
    @Schema(description = "会计期间止")
    private String periodTo;
    @Schema(description = "归档日期")
    private LocalDate archiveDate;
    @Schema(description = "归档类型：1-月度归档，2-季度归档，3-年度归档")
    private Integer archiveType;
    @Schema(description = "归档状态：0-待归档，1-已归档，2-已封存")
    private Integer status;
    @Schema(description = "凭证数量")
    private Integer voucherCount;
    @Schema(description = "归档文件大小(字节)")
    private Long fileSize;
    @Schema(description = "归档文件路径")
    private String filePath;
    @Schema(description = "XBRL文件路径")
    private String xbrlPath;
    @Schema(description = "归档人ID")
    private Long archivistId;
    @Schema(description = "归档人姓名")
    private String archivistName;
    @Schema(description = "审核人ID")
    private Long reviewerId;
    @Schema(description = "审核人姓名")
    private String reviewerName;
    @Schema(description = "审核时间")
    private LocalDateTime reviewTime;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @Schema(description = "电子凭证ID列表")
    private List<Long> voucherIds;
    @Schema(description = "归档明细列表")
    private List<VoucherArchiveItemDTO> items;


    @Schema(description = "归档明细项")
    public static class VoucherArchiveItemDTO {
        @Schema(description = "明细ID")
        private Long id;
        @Schema(description = "电子凭证ID")
        private Long voucherId;
        @Schema(description = "电子凭证编号")
        private String voucherNo;
        @Schema(description = "凭证类型")
        private String voucherType;
        @Schema(description = "凭证类型名称")
        private String voucherTypeName;
        @Schema(description = "金额")
        private java.math.BigDecimal amount;
        @Schema(description = "发生日期")
        private LocalDate issueDate;
        @Schema(description = "源文件路径")
        private String sourceFilePath;
        @Schema(description = "是否验签通过")
        private Boolean signatureValid;
        @Schema(description = "是否验真通过")
        private Boolean verifyValid;
        @Schema(description = "关联记账凭证ID")
        private Long financeVoucherId;

        public VoucherArchiveItemDTO() {
        }

        public Long getId() {
            return this.id;
        }

        public Long getVoucherId() {
            return this.voucherId;
        }

        public String getVoucherNo() {
            return this.voucherNo;
        }

        public String getVoucherType() {
            return this.voucherType;
        }

        public String getVoucherTypeName() {
            return this.voucherTypeName;
        }

        public java.math.BigDecimal getAmount() {
            return this.amount;
        }

        public LocalDate getIssueDate() {
            return this.issueDate;
        }

        public String getSourceFilePath() {
            return this.sourceFilePath;
        }

        public Boolean getSignatureValid() {
            return this.signatureValid;
        }

        public Boolean getVerifyValid() {
            return this.verifyValid;
        }

        public Long getFinanceVoucherId() {
            return this.financeVoucherId;
        }

        public void setId(final Long id) {
            this.id = id;
        }

        public void setVoucherId(final Long voucherId) {
            this.voucherId = voucherId;
        }

        public void setVoucherNo(final String voucherNo) {
            this.voucherNo = voucherNo;
        }

        public void setVoucherType(final String voucherType) {
            this.voucherType = voucherType;
        }

        public void setVoucherTypeName(final String voucherTypeName) {
            this.voucherTypeName = voucherTypeName;
        }

        public void setAmount(final java.math.BigDecimal amount) {
            this.amount = amount;
        }

        public void setIssueDate(final LocalDate issueDate) {
            this.issueDate = issueDate;
        }

        public void setSourceFilePath(final String sourceFilePath) {
            this.sourceFilePath = sourceFilePath;
        }

        public void setSignatureValid(final Boolean signatureValid) {
            this.signatureValid = signatureValid;
        }

        public void setVerifyValid(final Boolean verifyValid) {
            this.verifyValid = verifyValid;
        }

        public void setFinanceVoucherId(final Long financeVoucherId) {
            this.financeVoucherId = financeVoucherId;
        }

        @java.lang.Override
        public boolean equals(final java.lang.Object o) {
            if (o == this) return true;
            if (!(o instanceof VoucherArchiveDTO.VoucherArchiveItemDTO)) return false;
            final VoucherArchiveDTO.VoucherArchiveItemDTO other = (VoucherArchiveDTO.VoucherArchiveItemDTO) o;
            if (!other.canEqual((java.lang.Object) this)) return false;
            final java.lang.Object this$id = this.getId();
            final java.lang.Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            final java.lang.Object this$voucherId = this.getVoucherId();
            final java.lang.Object other$voucherId = other.getVoucherId();
            if (this$voucherId == null ? other$voucherId != null : !this$voucherId.equals(other$voucherId)) return false;
            final java.lang.Object this$signatureValid = this.getSignatureValid();
            final java.lang.Object other$signatureValid = other.getSignatureValid();
            if (this$signatureValid == null ? other$signatureValid != null : !this$signatureValid.equals(other$signatureValid)) return false;
            final java.lang.Object this$verifyValid = this.getVerifyValid();
            final java.lang.Object other$verifyValid = other.getVerifyValid();
            if (this$verifyValid == null ? other$verifyValid != null : !this$verifyValid.equals(other$verifyValid)) return false;
            final java.lang.Object this$financeVoucherId = this.getFinanceVoucherId();
            final java.lang.Object other$financeVoucherId = other.getFinanceVoucherId();
            if (this$financeVoucherId == null ? other$financeVoucherId != null : !this$financeVoucherId.equals(other$financeVoucherId)) return false;
            final java.lang.Object this$voucherNo = this.getVoucherNo();
            final java.lang.Object other$voucherNo = other.getVoucherNo();
            if (this$voucherNo == null ? other$voucherNo != null : !this$voucherNo.equals(other$voucherNo)) return false;
            final java.lang.Object this$voucherType = this.getVoucherType();
            final java.lang.Object other$voucherType = other.getVoucherType();
            if (this$voucherType == null ? other$voucherType != null : !this$voucherType.equals(other$voucherType)) return false;
            final java.lang.Object this$voucherTypeName = this.getVoucherTypeName();
            final java.lang.Object other$voucherTypeName = other.getVoucherTypeName();
            if (this$voucherTypeName == null ? other$voucherTypeName != null : !this$voucherTypeName.equals(other$voucherTypeName)) return false;
            final java.lang.Object this$amount = this.getAmount();
            final java.lang.Object other$amount = other.getAmount();
            if (this$amount == null ? other$amount != null : !this$amount.equals(other$amount)) return false;
            final java.lang.Object this$issueDate = this.getIssueDate();
            final java.lang.Object other$issueDate = other.getIssueDate();
            if (this$issueDate == null ? other$issueDate != null : !this$issueDate.equals(other$issueDate)) return false;
            final java.lang.Object this$sourceFilePath = this.getSourceFilePath();
            final java.lang.Object other$sourceFilePath = other.getSourceFilePath();
            if (this$sourceFilePath == null ? other$sourceFilePath != null : !this$sourceFilePath.equals(other$sourceFilePath)) return false;
            return true;
        }

        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof VoucherArchiveDTO.VoucherArchiveItemDTO;
        }

        @java.lang.Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final java.lang.Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            final java.lang.Object $voucherId = this.getVoucherId();
            result = result * PRIME + ($voucherId == null ? 43 : $voucherId.hashCode());
            final java.lang.Object $signatureValid = this.getSignatureValid();
            result = result * PRIME + ($signatureValid == null ? 43 : $signatureValid.hashCode());
            final java.lang.Object $verifyValid = this.getVerifyValid();
            result = result * PRIME + ($verifyValid == null ? 43 : $verifyValid.hashCode());
            final java.lang.Object $financeVoucherId = this.getFinanceVoucherId();
            result = result * PRIME + ($financeVoucherId == null ? 43 : $financeVoucherId.hashCode());
            final java.lang.Object $voucherNo = this.getVoucherNo();
            result = result * PRIME + ($voucherNo == null ? 43 : $voucherNo.hashCode());
            final java.lang.Object $voucherType = this.getVoucherType();
            result = result * PRIME + ($voucherType == null ? 43 : $voucherType.hashCode());
            final java.lang.Object $voucherTypeName = this.getVoucherTypeName();
            result = result * PRIME + ($voucherTypeName == null ? 43 : $voucherTypeName.hashCode());
            final java.lang.Object $amount = this.getAmount();
            result = result * PRIME + ($amount == null ? 43 : $amount.hashCode());
            final java.lang.Object $issueDate = this.getIssueDate();
            result = result * PRIME + ($issueDate == null ? 43 : $issueDate.hashCode());
            final java.lang.Object $sourceFilePath = this.getSourceFilePath();
            result = result * PRIME + ($sourceFilePath == null ? 43 : $sourceFilePath.hashCode());
            return result;
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "VoucherArchiveDTO.VoucherArchiveItemDTO(id=" + this.getId() + ", voucherId=" + this.getVoucherId() + ", voucherNo=" + this.getVoucherNo() + ", voucherType=" + this.getVoucherType() + ", voucherTypeName=" + this.getVoucherTypeName() + ", amount=" + this.getAmount() + ", issueDate=" + this.getIssueDate() + ", sourceFilePath=" + this.getSourceFilePath() + ", signatureValid=" + this.getSignatureValid() + ", verifyValid=" + this.getVerifyValid() + ", financeVoucherId=" + this.getFinanceVoucherId() + ")";
        }
    }

    public VoucherArchiveDTO() {
    }

    public Long getId() {
        return this.id;
    }

    public String getArchiveNo() {
        return this.archiveNo;
    }

    public String getArchiveName() {
        return this.archiveName;
    }

    public String getFiscalYear() {
        return this.fiscalYear;
    }

    public String getPeriodFrom() {
        return this.periodFrom;
    }

    public String getPeriodTo() {
        return this.periodTo;
    }

    public LocalDate getArchiveDate() {
        return this.archiveDate;
    }

    public Integer getArchiveType() {
        return this.archiveType;
    }

    public Integer getStatus() {
        return this.status;
    }

    public Integer getVoucherCount() {
        return this.voucherCount;
    }

    public Long getFileSize() {
        return this.fileSize;
    }

    public String getFilePath() {
        return this.filePath;
    }

    public String getXbrlPath() {
        return this.xbrlPath;
    }

    public Long getArchivistId() {
        return this.archivistId;
    }

    public String getArchivistName() {
        return this.archivistName;
    }

    public Long getReviewerId() {
        return this.reviewerId;
    }

    public String getReviewerName() {
        return this.reviewerName;
    }

    public LocalDateTime getReviewTime() {
        return this.reviewTime;
    }

    public String getRemark() {
        return this.remark;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public List<Long> getVoucherIds() {
        return this.voucherIds;
    }

    public List<VoucherArchiveItemDTO> getItems() {
        return this.items;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setArchiveNo(final String archiveNo) {
        this.archiveNo = archiveNo;
    }

    public void setArchiveName(final String archiveName) {
        this.archiveName = archiveName;
    }

    public void setFiscalYear(final String fiscalYear) {
        this.fiscalYear = fiscalYear;
    }

    public void setPeriodFrom(final String periodFrom) {
        this.periodFrom = periodFrom;
    }

    public void setPeriodTo(final String periodTo) {
        this.periodTo = periodTo;
    }

    public void setArchiveDate(final LocalDate archiveDate) {
        this.archiveDate = archiveDate;
    }

    public void setArchiveType(final Integer archiveType) {
        this.archiveType = archiveType;
    }

    public void setStatus(final Integer status) {
        this.status = status;
    }

    public void setVoucherCount(final Integer voucherCount) {
        this.voucherCount = voucherCount;
    }

    public void setFileSize(final Long fileSize) {
        this.fileSize = fileSize;
    }

    public void setFilePath(final String filePath) {
        this.filePath = filePath;
    }

    public void setXbrlPath(final String xbrlPath) {
        this.xbrlPath = xbrlPath;
    }

    public void setArchivistId(final Long archivistId) {
        this.archivistId = archivistId;
    }

    public void setArchivistName(final String archivistName) {
        this.archivistName = archivistName;
    }

    public void setReviewerId(final Long reviewerId) {
        this.reviewerId = reviewerId;
    }

    public void setReviewerName(final String reviewerName) {
        this.reviewerName = reviewerName;
    }

    public void setReviewTime(final LocalDateTime reviewTime) {
        this.reviewTime = reviewTime;
    }

    public void setRemark(final String remark) {
        this.remark = remark;
    }

    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setVoucherIds(final List<Long> voucherIds) {
        this.voucherIds = voucherIds;
    }

    public void setItems(final List<VoucherArchiveItemDTO> items) {
        this.items = items;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof VoucherArchiveDTO)) return false;
        final VoucherArchiveDTO other = (VoucherArchiveDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$archiveType = this.getArchiveType();
        final java.lang.Object other$archiveType = other.getArchiveType();
        if (this$archiveType == null ? other$archiveType != null : !this$archiveType.equals(other$archiveType)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$voucherCount = this.getVoucherCount();
        final java.lang.Object other$voucherCount = other.getVoucherCount();
        if (this$voucherCount == null ? other$voucherCount != null : !this$voucherCount.equals(other$voucherCount)) return false;
        final java.lang.Object this$fileSize = this.getFileSize();
        final java.lang.Object other$fileSize = other.getFileSize();
        if (this$fileSize == null ? other$fileSize != null : !this$fileSize.equals(other$fileSize)) return false;
        final java.lang.Object this$archivistId = this.getArchivistId();
        final java.lang.Object other$archivistId = other.getArchivistId();
        if (this$archivistId == null ? other$archivistId != null : !this$archivistId.equals(other$archivistId)) return false;
        final java.lang.Object this$reviewerId = this.getReviewerId();
        final java.lang.Object other$reviewerId = other.getReviewerId();
        if (this$reviewerId == null ? other$reviewerId != null : !this$reviewerId.equals(other$reviewerId)) return false;
        final java.lang.Object this$archiveNo = this.getArchiveNo();
        final java.lang.Object other$archiveNo = other.getArchiveNo();
        if (this$archiveNo == null ? other$archiveNo != null : !this$archiveNo.equals(other$archiveNo)) return false;
        final java.lang.Object this$archiveName = this.getArchiveName();
        final java.lang.Object other$archiveName = other.getArchiveName();
        if (this$archiveName == null ? other$archiveName != null : !this$archiveName.equals(other$archiveName)) return false;
        final java.lang.Object this$fiscalYear = this.getFiscalYear();
        final java.lang.Object other$fiscalYear = other.getFiscalYear();
        if (this$fiscalYear == null ? other$fiscalYear != null : !this$fiscalYear.equals(other$fiscalYear)) return false;
        final java.lang.Object this$periodFrom = this.getPeriodFrom();
        final java.lang.Object other$periodFrom = other.getPeriodFrom();
        if (this$periodFrom == null ? other$periodFrom != null : !this$periodFrom.equals(other$periodFrom)) return false;
        final java.lang.Object this$periodTo = this.getPeriodTo();
        final java.lang.Object other$periodTo = other.getPeriodTo();
        if (this$periodTo == null ? other$periodTo != null : !this$periodTo.equals(other$periodTo)) return false;
        final java.lang.Object this$archiveDate = this.getArchiveDate();
        final java.lang.Object other$archiveDate = other.getArchiveDate();
        if (this$archiveDate == null ? other$archiveDate != null : !this$archiveDate.equals(other$archiveDate)) return false;
        final java.lang.Object this$filePath = this.getFilePath();
        final java.lang.Object other$filePath = other.getFilePath();
        if (this$filePath == null ? other$filePath != null : !this$filePath.equals(other$filePath)) return false;
        final java.lang.Object this$xbrlPath = this.getXbrlPath();
        final java.lang.Object other$xbrlPath = other.getXbrlPath();
        if (this$xbrlPath == null ? other$xbrlPath != null : !this$xbrlPath.equals(other$xbrlPath)) return false;
        final java.lang.Object this$archivistName = this.getArchivistName();
        final java.lang.Object other$archivistName = other.getArchivistName();
        if (this$archivistName == null ? other$archivistName != null : !this$archivistName.equals(other$archivistName)) return false;
        final java.lang.Object this$reviewerName = this.getReviewerName();
        final java.lang.Object other$reviewerName = other.getReviewerName();
        if (this$reviewerName == null ? other$reviewerName != null : !this$reviewerName.equals(other$reviewerName)) return false;
        final java.lang.Object this$reviewTime = this.getReviewTime();
        final java.lang.Object other$reviewTime = other.getReviewTime();
        if (this$reviewTime == null ? other$reviewTime != null : !this$reviewTime.equals(other$reviewTime)) return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        final java.lang.Object this$voucherIds = this.getVoucherIds();
        final java.lang.Object other$voucherIds = other.getVoucherIds();
        if (this$voucherIds == null ? other$voucherIds != null : !this$voucherIds.equals(other$voucherIds)) return false;
        final java.lang.Object this$items = this.getItems();
        final java.lang.Object other$items = other.getItems();
        if (this$items == null ? other$items != null : !this$items.equals(other$items)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof VoucherArchiveDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $archiveType = this.getArchiveType();
        result = result * PRIME + ($archiveType == null ? 43 : $archiveType.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $voucherCount = this.getVoucherCount();
        result = result * PRIME + ($voucherCount == null ? 43 : $voucherCount.hashCode());
        final java.lang.Object $fileSize = this.getFileSize();
        result = result * PRIME + ($fileSize == null ? 43 : $fileSize.hashCode());
        final java.lang.Object $archivistId = this.getArchivistId();
        result = result * PRIME + ($archivistId == null ? 43 : $archivistId.hashCode());
        final java.lang.Object $reviewerId = this.getReviewerId();
        result = result * PRIME + ($reviewerId == null ? 43 : $reviewerId.hashCode());
        final java.lang.Object $archiveNo = this.getArchiveNo();
        result = result * PRIME + ($archiveNo == null ? 43 : $archiveNo.hashCode());
        final java.lang.Object $archiveName = this.getArchiveName();
        result = result * PRIME + ($archiveName == null ? 43 : $archiveName.hashCode());
        final java.lang.Object $fiscalYear = this.getFiscalYear();
        result = result * PRIME + ($fiscalYear == null ? 43 : $fiscalYear.hashCode());
        final java.lang.Object $periodFrom = this.getPeriodFrom();
        result = result * PRIME + ($periodFrom == null ? 43 : $periodFrom.hashCode());
        final java.lang.Object $periodTo = this.getPeriodTo();
        result = result * PRIME + ($periodTo == null ? 43 : $periodTo.hashCode());
        final java.lang.Object $archiveDate = this.getArchiveDate();
        result = result * PRIME + ($archiveDate == null ? 43 : $archiveDate.hashCode());
        final java.lang.Object $filePath = this.getFilePath();
        result = result * PRIME + ($filePath == null ? 43 : $filePath.hashCode());
        final java.lang.Object $xbrlPath = this.getXbrlPath();
        result = result * PRIME + ($xbrlPath == null ? 43 : $xbrlPath.hashCode());
        final java.lang.Object $archivistName = this.getArchivistName();
        result = result * PRIME + ($archivistName == null ? 43 : $archivistName.hashCode());
        final java.lang.Object $reviewerName = this.getReviewerName();
        result = result * PRIME + ($reviewerName == null ? 43 : $reviewerName.hashCode());
        final java.lang.Object $reviewTime = this.getReviewTime();
        result = result * PRIME + ($reviewTime == null ? 43 : $reviewTime.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        final java.lang.Object $voucherIds = this.getVoucherIds();
        result = result * PRIME + ($voucherIds == null ? 43 : $voucherIds.hashCode());
        final java.lang.Object $items = this.getItems();
        result = result * PRIME + ($items == null ? 43 : $items.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "VoucherArchiveDTO(id=" + this.getId() + ", archiveNo=" + this.getArchiveNo() + ", archiveName=" + this.getArchiveName() + ", fiscalYear=" + this.getFiscalYear() + ", periodFrom=" + this.getPeriodFrom() + ", periodTo=" + this.getPeriodTo() + ", archiveDate=" + this.getArchiveDate() + ", archiveType=" + this.getArchiveType() + ", status=" + this.getStatus() + ", voucherCount=" + this.getVoucherCount() + ", fileSize=" + this.getFileSize() + ", filePath=" + this.getFilePath() + ", xbrlPath=" + this.getXbrlPath() + ", archivistId=" + this.getArchivistId() + ", archivistName=" + this.getArchivistName() + ", reviewerId=" + this.getReviewerId() + ", reviewerName=" + this.getReviewerName() + ", reviewTime=" + this.getReviewTime() + ", remark=" + this.getRemark() + ", createdAt=" + this.getCreatedAt() + ", voucherIds=" + this.getVoucherIds() + ", items=" + this.getItems() + ")";
    }
}
