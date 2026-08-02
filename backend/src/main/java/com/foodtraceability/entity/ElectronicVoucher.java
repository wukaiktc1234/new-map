package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 电子凭证实体类
 * 用于存储符合财政部标准的电子凭证信息
 * 依据：财会〔2025〕9号《关于推广应用电子凭证会计数据标准的通知》
 */
@TableName("electronic_voucher")
@Schema(description = "电子凭证实体")
public class ElectronicVoucher {
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;
    @TableField("tenant_id")
    @Schema(description = "租户ID")
    private Long tenantId;
    @TableField("voucher_type")
    @Schema(description = "凭证类型：invoice/ticket/bank_receipt/tax_receipt等")
    private String voucherType;
    @TableField("voucher_no")
    @Schema(description = "凭证编号")
    private String voucherNo;
    @TableField("source_file_path")
    @Schema(description = "源文件存储路径")
    private String sourceFilePath;
    @TableField("source_file_hash")
    @Schema(description = "文件哈希值（SHA256）")
    private String sourceFileHash;
    @TableField("source_file_size")
    @Schema(description = "源文件大小(字节)")
    private Long sourceFileSize;
    @TableField("source_file_type")
    @Schema(description = "源文件类型：XML/OFD/PDF")
    private String sourceFileType;
    @TableField("xml_content")
    @Schema(description = "XML原文内容")
    private String xmlContent;
    @TableField("parsed_data")
    @Schema(description = "解析后的结构化数据")
    private String parsedData;
    @TableField("total_amount")
    @Schema(description = "总金额")
    private BigDecimal totalAmount;
    @TableField("currency")
    @Schema(description = "币种")
    private String currency;
    @TableField("issue_date")
    @Schema(description = "开票/发生日期")
    private LocalDate issueDate;
    @TableField("signature_status")
    @Schema(description = "验签状态：0-未验签，1-验签通过，2-验签失败")
    private Integer signatureStatus;
    @TableField("verify_status")
    @Schema(description = "验真状态：0-未验真，1-验真通过，2-验真失败")
    private Integer verifyStatus;
    @TableField("verify_time")
    @Schema(description = "验真时间")
    private LocalDateTime verifyTime;
    @TableField("verify_message")
    @Schema(description = "验真消息")
    private String verifyMessage;
    @TableField("business_id")
    @Schema(description = "关联业务ID")
    private Long businessId;
    @TableField("business_type")
    @Schema(description = "关联业务类型")
    private String businessType;
    @TableField("finance_voucher_id")
    @Schema(description = "关联记账凭证ID")
    private Long financeVoucherId;
    @TableField("archive_id")
    @Schema(description = "关联档案ID")
    private Long archiveId;
    @TableField("status")
    @Schema(description = "状态：0-待处理，1-已入账，2-已归档")
    private Integer status;
    @Version
    @TableField("version")
    @Schema(description = "乐观锁版本号")
    private Integer version;
    @TableField("create_time")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @TableField("update_time")
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
    @TableField("created_by")
    @Schema(description = "创建人ID")
    private Long createdBy;
    @TableField("updated_by")
    @Schema(description = "更新人ID")
    private Long updatedBy;
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除：0-未删除，1-已删除")
    private Integer deleted;

    public ElectronicVoucher() {
    }

    public Long getId() {
        return this.id;
    }

    public Long getTenantId() {
        return this.tenantId;
    }

    public String getVoucherType() {
        return this.voucherType;
    }

    public String getVoucherNo() {
        return this.voucherNo;
    }

    public String getSourceFilePath() {
        return this.sourceFilePath;
    }

    public String getSourceFileHash() {
        return this.sourceFileHash;
    }

    public Long getSourceFileSize() {
        return this.sourceFileSize;
    }

    public String getSourceFileType() {
        return this.sourceFileType;
    }

    public String getXmlContent() {
        return this.xmlContent;
    }

    public String getParsedData() {
        return this.parsedData;
    }

    public BigDecimal getTotalAmount() {
        return this.totalAmount;
    }

    public String getCurrency() {
        return this.currency;
    }

    public LocalDate getIssueDate() {
        return this.issueDate;
    }

    public Integer getSignatureStatus() {
        return this.signatureStatus;
    }

    public Integer getVerifyStatus() {
        return this.verifyStatus;
    }

    public LocalDateTime getVerifyTime() {
        return this.verifyTime;
    }

    public String getVerifyMessage() {
        return this.verifyMessage;
    }

    public Long getBusinessId() {
        return this.businessId;
    }

    public String getBusinessType() {
        return this.businessType;
    }

    public Long getFinanceVoucherId() {
        return this.financeVoucherId;
    }

    public Long getArchiveId() {
        return this.archiveId;
    }

    public Integer getStatus() {
        return this.status;
    }

    public Integer getVersion() {
        return this.version;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public Long getCreatedBy() {
        return this.createdBy;
    }

    public Long getUpdatedBy() {
        return this.updatedBy;
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

    public void setVoucherType(final String voucherType) {
        this.voucherType = voucherType;
    }

    public void setVoucherNo(final String voucherNo) {
        this.voucherNo = voucherNo;
    }

    public void setSourceFilePath(final String sourceFilePath) {
        this.sourceFilePath = sourceFilePath;
    }

    public void setSourceFileHash(final String sourceFileHash) {
        this.sourceFileHash = sourceFileHash;
    }

    public void setSourceFileSize(final Long sourceFileSize) {
        this.sourceFileSize = sourceFileSize;
    }

    public void setSourceFileType(final String sourceFileType) {
        this.sourceFileType = sourceFileType;
    }

    public void setXmlContent(final String xmlContent) {
        this.xmlContent = xmlContent;
    }

    public void setParsedData(final String parsedData) {
        this.parsedData = parsedData;
    }

    public void setTotalAmount(final BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public void setCurrency(final String currency) {
        this.currency = currency;
    }

    public void setIssueDate(final LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public void setSignatureStatus(final Integer signatureStatus) {
        this.signatureStatus = signatureStatus;
    }

    public void setVerifyStatus(final Integer verifyStatus) {
        this.verifyStatus = verifyStatus;
    }

    public void setVerifyTime(final LocalDateTime verifyTime) {
        this.verifyTime = verifyTime;
    }

    public void setVerifyMessage(final String verifyMessage) {
        this.verifyMessage = verifyMessage;
    }

    public void setBusinessId(final Long businessId) {
        this.businessId = businessId;
    }

    public void setBusinessType(final String businessType) {
        this.businessType = businessType;
    }

    public void setFinanceVoucherId(final Long financeVoucherId) {
        this.financeVoucherId = financeVoucherId;
    }

    public void setArchiveId(final Long archiveId) {
        this.archiveId = archiveId;
    }

    public void setStatus(final Integer status) {
        this.status = status;
    }

    public void setVersion(final Integer version) {
        this.version = version;
    }

    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(final LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setCreatedBy(final Long createdBy) {
        this.createdBy = createdBy;
    }

    public void setUpdatedBy(final Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof ElectronicVoucher)) return false;
        final ElectronicVoucher other = (ElectronicVoucher) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$tenantId = this.getTenantId();
        final java.lang.Object other$tenantId = other.getTenantId();
        if (this$tenantId == null ? other$tenantId != null : !this$tenantId.equals(other$tenantId)) return false;
        final java.lang.Object this$sourceFileSize = this.getSourceFileSize();
        final java.lang.Object other$sourceFileSize = other.getSourceFileSize();
        if (this$sourceFileSize == null ? other$sourceFileSize != null : !this$sourceFileSize.equals(other$sourceFileSize)) return false;
        final java.lang.Object this$signatureStatus = this.getSignatureStatus();
        final java.lang.Object other$signatureStatus = other.getSignatureStatus();
        if (this$signatureStatus == null ? other$signatureStatus != null : !this$signatureStatus.equals(other$signatureStatus)) return false;
        final java.lang.Object this$verifyStatus = this.getVerifyStatus();
        final java.lang.Object other$verifyStatus = other.getVerifyStatus();
        if (this$verifyStatus == null ? other$verifyStatus != null : !this$verifyStatus.equals(other$verifyStatus)) return false;
        final java.lang.Object this$businessId = this.getBusinessId();
        final java.lang.Object other$businessId = other.getBusinessId();
        if (this$businessId == null ? other$businessId != null : !this$businessId.equals(other$businessId)) return false;
        final java.lang.Object this$financeVoucherId = this.getFinanceVoucherId();
        final java.lang.Object other$financeVoucherId = other.getFinanceVoucherId();
        if (this$financeVoucherId == null ? other$financeVoucherId != null : !this$financeVoucherId.equals(other$financeVoucherId)) return false;
        final java.lang.Object this$archiveId = this.getArchiveId();
        final java.lang.Object other$archiveId = other.getArchiveId();
        if (this$archiveId == null ? other$archiveId != null : !this$archiveId.equals(other$archiveId)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$version = this.getVersion();
        final java.lang.Object other$version = other.getVersion();
        if (this$version == null ? other$version != null : !this$version.equals(other$version)) return false;
        final java.lang.Object this$createdBy = this.getCreatedBy();
        final java.lang.Object other$createdBy = other.getCreatedBy();
        if (this$createdBy == null ? other$createdBy != null : !this$createdBy.equals(other$createdBy)) return false;
        final java.lang.Object this$updatedBy = this.getUpdatedBy();
        final java.lang.Object other$updatedBy = other.getUpdatedBy();
        if (this$updatedBy == null ? other$updatedBy != null : !this$updatedBy.equals(other$updatedBy)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$voucherType = this.getVoucherType();
        final java.lang.Object other$voucherType = other.getVoucherType();
        if (this$voucherType == null ? other$voucherType != null : !this$voucherType.equals(other$voucherType)) return false;
        final java.lang.Object this$voucherNo = this.getVoucherNo();
        final java.lang.Object other$voucherNo = other.getVoucherNo();
        if (this$voucherNo == null ? other$voucherNo != null : !this$voucherNo.equals(other$voucherNo)) return false;
        final java.lang.Object this$sourceFilePath = this.getSourceFilePath();
        final java.lang.Object other$sourceFilePath = other.getSourceFilePath();
        if (this$sourceFilePath == null ? other$sourceFilePath != null : !this$sourceFilePath.equals(other$sourceFilePath)) return false;
        final java.lang.Object this$sourceFileHash = this.getSourceFileHash();
        final java.lang.Object other$sourceFileHash = other.getSourceFileHash();
        if (this$sourceFileHash == null ? other$sourceFileHash != null : !this$sourceFileHash.equals(other$sourceFileHash)) return false;
        final java.lang.Object this$sourceFileType = this.getSourceFileType();
        final java.lang.Object other$sourceFileType = other.getSourceFileType();
        if (this$sourceFileType == null ? other$sourceFileType != null : !this$sourceFileType.equals(other$sourceFileType)) return false;
        final java.lang.Object this$xmlContent = this.getXmlContent();
        final java.lang.Object other$xmlContent = other.getXmlContent();
        if (this$xmlContent == null ? other$xmlContent != null : !this$xmlContent.equals(other$xmlContent)) return false;
        final java.lang.Object this$parsedData = this.getParsedData();
        final java.lang.Object other$parsedData = other.getParsedData();
        if (this$parsedData == null ? other$parsedData != null : !this$parsedData.equals(other$parsedData)) return false;
        final java.lang.Object this$totalAmount = this.getTotalAmount();
        final java.lang.Object other$totalAmount = other.getTotalAmount();
        if (this$totalAmount == null ? other$totalAmount != null : !this$totalAmount.equals(other$totalAmount)) return false;
        final java.lang.Object this$currency = this.getCurrency();
        final java.lang.Object other$currency = other.getCurrency();
        if (this$currency == null ? other$currency != null : !this$currency.equals(other$currency)) return false;
        final java.lang.Object this$issueDate = this.getIssueDate();
        final java.lang.Object other$issueDate = other.getIssueDate();
        if (this$issueDate == null ? other$issueDate != null : !this$issueDate.equals(other$issueDate)) return false;
        final java.lang.Object this$verifyTime = this.getVerifyTime();
        final java.lang.Object other$verifyTime = other.getVerifyTime();
        if (this$verifyTime == null ? other$verifyTime != null : !this$verifyTime.equals(other$verifyTime)) return false;
        final java.lang.Object this$verifyMessage = this.getVerifyMessage();
        final java.lang.Object other$verifyMessage = other.getVerifyMessage();
        if (this$verifyMessage == null ? other$verifyMessage != null : !this$verifyMessage.equals(other$verifyMessage)) return false;
        final java.lang.Object this$businessType = this.getBusinessType();
        final java.lang.Object other$businessType = other.getBusinessType();
        if (this$businessType == null ? other$businessType != null : !this$businessType.equals(other$businessType)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        final java.lang.Object this$updatedAt = this.getUpdatedAt();
        final java.lang.Object other$updatedAt = other.getUpdatedAt();
        if (this$updatedAt == null ? other$updatedAt != null : !this$updatedAt.equals(other$updatedAt)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ElectronicVoucher;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $tenantId = this.getTenantId();
        result = result * PRIME + ($tenantId == null ? 43 : $tenantId.hashCode());
        final java.lang.Object $sourceFileSize = this.getSourceFileSize();
        result = result * PRIME + ($sourceFileSize == null ? 43 : $sourceFileSize.hashCode());
        final java.lang.Object $signatureStatus = this.getSignatureStatus();
        result = result * PRIME + ($signatureStatus == null ? 43 : $signatureStatus.hashCode());
        final java.lang.Object $verifyStatus = this.getVerifyStatus();
        result = result * PRIME + ($verifyStatus == null ? 43 : $verifyStatus.hashCode());
        final java.lang.Object $businessId = this.getBusinessId();
        result = result * PRIME + ($businessId == null ? 43 : $businessId.hashCode());
        final java.lang.Object $financeVoucherId = this.getFinanceVoucherId();
        result = result * PRIME + ($financeVoucherId == null ? 43 : $financeVoucherId.hashCode());
        final java.lang.Object $archiveId = this.getArchiveId();
        result = result * PRIME + ($archiveId == null ? 43 : $archiveId.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $version = this.getVersion();
        result = result * PRIME + ($version == null ? 43 : $version.hashCode());
        final java.lang.Object $createdBy = this.getCreatedBy();
        result = result * PRIME + ($createdBy == null ? 43 : $createdBy.hashCode());
        final java.lang.Object $updatedBy = this.getUpdatedBy();
        result = result * PRIME + ($updatedBy == null ? 43 : $updatedBy.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $voucherType = this.getVoucherType();
        result = result * PRIME + ($voucherType == null ? 43 : $voucherType.hashCode());
        final java.lang.Object $voucherNo = this.getVoucherNo();
        result = result * PRIME + ($voucherNo == null ? 43 : $voucherNo.hashCode());
        final java.lang.Object $sourceFilePath = this.getSourceFilePath();
        result = result * PRIME + ($sourceFilePath == null ? 43 : $sourceFilePath.hashCode());
        final java.lang.Object $sourceFileHash = this.getSourceFileHash();
        result = result * PRIME + ($sourceFileHash == null ? 43 : $sourceFileHash.hashCode());
        final java.lang.Object $sourceFileType = this.getSourceFileType();
        result = result * PRIME + ($sourceFileType == null ? 43 : $sourceFileType.hashCode());
        final java.lang.Object $xmlContent = this.getXmlContent();
        result = result * PRIME + ($xmlContent == null ? 43 : $xmlContent.hashCode());
        final java.lang.Object $parsedData = this.getParsedData();
        result = result * PRIME + ($parsedData == null ? 43 : $parsedData.hashCode());
        final java.lang.Object $totalAmount = this.getTotalAmount();
        result = result * PRIME + ($totalAmount == null ? 43 : $totalAmount.hashCode());
        final java.lang.Object $currency = this.getCurrency();
        result = result * PRIME + ($currency == null ? 43 : $currency.hashCode());
        final java.lang.Object $issueDate = this.getIssueDate();
        result = result * PRIME + ($issueDate == null ? 43 : $issueDate.hashCode());
        final java.lang.Object $verifyTime = this.getVerifyTime();
        result = result * PRIME + ($verifyTime == null ? 43 : $verifyTime.hashCode());
        final java.lang.Object $verifyMessage = this.getVerifyMessage();
        result = result * PRIME + ($verifyMessage == null ? 43 : $verifyMessage.hashCode());
        final java.lang.Object $businessType = this.getBusinessType();
        result = result * PRIME + ($businessType == null ? 43 : $businessType.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        final java.lang.Object $updatedAt = this.getUpdatedAt();
        result = result * PRIME + ($updatedAt == null ? 43 : $updatedAt.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "ElectronicVoucher(id=" + this.getId() + ", tenantId=" + this.getTenantId() + ", voucherType=" + this.getVoucherType() + ", voucherNo=" + this.getVoucherNo() + ", sourceFilePath=" + this.getSourceFilePath() + ", sourceFileHash=" + this.getSourceFileHash() + ", sourceFileSize=" + this.getSourceFileSize() + ", sourceFileType=" + this.getSourceFileType() + ", xmlContent=" + this.getXmlContent() + ", parsedData=" + this.getParsedData() + ", totalAmount=" + this.getTotalAmount() + ", currency=" + this.getCurrency() + ", issueDate=" + this.getIssueDate() + ", signatureStatus=" + this.getSignatureStatus() + ", verifyStatus=" + this.getVerifyStatus() + ", verifyTime=" + this.getVerifyTime() + ", verifyMessage=" + this.getVerifyMessage() + ", businessId=" + this.getBusinessId() + ", businessType=" + this.getBusinessType() + ", financeVoucherId=" + this.getFinanceVoucherId() + ", archiveId=" + this.getArchiveId() + ", status=" + this.getStatus() + ", version=" + this.getVersion() + ", createdAt=" + this.getCreatedAt() + ", updatedAt=" + this.getUpdatedAt() + ", createdBy=" + this.getCreatedBy() + ", updatedBy=" + this.getUpdatedBy() + ", deleted=" + this.getDeleted() + ")";
    }
}
