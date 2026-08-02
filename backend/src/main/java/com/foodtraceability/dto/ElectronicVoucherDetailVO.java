package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 电子凭证详情VO
 */
@Schema(description = "电子凭证详情VO")
public class ElectronicVoucherDetailVO {
    @Schema(description = "主键ID")
    private Long id;
    @Schema(description = "凭证类型")
    private String voucherType;
    @Schema(description = "凭证类型名称")
    private String voucherTypeName;
    @Schema(description = "凭证编号")
    private String voucherNo;
    @Schema(description = "源文件路径")
    private String sourceFilePath;
    @Schema(description = "文件哈希值")
    private String sourceFileHash;
    @Schema(description = "文件大小(字节)")
    private Long sourceFileSize;
    @Schema(description = "源文件类型")
    private String sourceFileType;
    @Schema(description = "总金额")
    private BigDecimal totalAmount;
    @Schema(description = "币种")
    private String currency;
    @Schema(description = "开票/发生日期")
    private LocalDate issueDate;
    @Schema(description = "验签状态")
    private Integer signatureStatus;
    @Schema(description = "验签状态名称")
    private String signatureStatusName;
    @Schema(description = "验真状态")
    private Integer verifyStatus;
    @Schema(description = "验真状态名称")
    private String verifyStatusName;
    @Schema(description = "验真时间")
    private String verifyTime;
    @Schema(description = "验真消息")
    private String verifyMessage;
    @Schema(description = "状态")
    private Integer status;
    @Schema(description = "状态名称")
    private String statusName;
    @Schema(description = "关联业务类型")
    private String businessType;
    @Schema(description = "关联业务ID")
    private Long businessId;
    @Schema(description = "关联记账凭证ID")
    private Long financeVoucherId;
    @Schema(description = "发票详情")
    private ElectronicInvoiceVO invoice;
    @Schema(description = "验签记录列表")
    private List<VoucherSignatureLogVO> signatureLogs;
    @Schema(description = "创建时间")
    private String createdAt;
    @Schema(description = "创建人姓名")
    private String createdByName;

    public ElectronicVoucherDetailVO() {
    }

    public Long getId() {
        return this.id;
    }

    public String getVoucherType() {
        return this.voucherType;
    }

    public String getVoucherTypeName() {
        return this.voucherTypeName;
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

    public String getSignatureStatusName() {
        return this.signatureStatusName;
    }

    public Integer getVerifyStatus() {
        return this.verifyStatus;
    }

    public String getVerifyStatusName() {
        return this.verifyStatusName;
    }

    public String getVerifyTime() {
        return this.verifyTime;
    }

    public String getVerifyMessage() {
        return this.verifyMessage;
    }

    public Integer getStatus() {
        return this.status;
    }

    public String getStatusName() {
        return this.statusName;
    }

    public String getBusinessType() {
        return this.businessType;
    }

    public Long getBusinessId() {
        return this.businessId;
    }

    public Long getFinanceVoucherId() {
        return this.financeVoucherId;
    }

    public ElectronicInvoiceVO getInvoice() {
        return this.invoice;
    }

    public List<VoucherSignatureLogVO> getSignatureLogs() {
        return this.signatureLogs;
    }

    public String getCreatedAt() {
        return this.createdAt;
    }

    public String getCreatedByName() {
        return this.createdByName;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setVoucherType(final String voucherType) {
        this.voucherType = voucherType;
    }

    public void setVoucherTypeName(final String voucherTypeName) {
        this.voucherTypeName = voucherTypeName;
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

    public void setSignatureStatusName(final String signatureStatusName) {
        this.signatureStatusName = signatureStatusName;
    }

    public void setVerifyStatus(final Integer verifyStatus) {
        this.verifyStatus = verifyStatus;
    }

    public void setVerifyStatusName(final String verifyStatusName) {
        this.verifyStatusName = verifyStatusName;
    }

    public void setVerifyTime(final String verifyTime) {
        this.verifyTime = verifyTime;
    }

    public void setVerifyMessage(final String verifyMessage) {
        this.verifyMessage = verifyMessage;
    }

    public void setStatus(final Integer status) {
        this.status = status;
    }

    public void setStatusName(final String statusName) {
        this.statusName = statusName;
    }

    public void setBusinessType(final String businessType) {
        this.businessType = businessType;
    }

    public void setBusinessId(final Long businessId) {
        this.businessId = businessId;
    }

    public void setFinanceVoucherId(final Long financeVoucherId) {
        this.financeVoucherId = financeVoucherId;
    }

    public void setInvoice(final ElectronicInvoiceVO invoice) {
        this.invoice = invoice;
    }

    public void setSignatureLogs(final List<VoucherSignatureLogVO> signatureLogs) {
        this.signatureLogs = signatureLogs;
    }

    public void setCreatedAt(final String createdAt) {
        this.createdAt = createdAt;
    }

    public void setCreatedByName(final String createdByName) {
        this.createdByName = createdByName;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof ElectronicVoucherDetailVO)) return false;
        final ElectronicVoucherDetailVO other = (ElectronicVoucherDetailVO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$sourceFileSize = this.getSourceFileSize();
        final java.lang.Object other$sourceFileSize = other.getSourceFileSize();
        if (this$sourceFileSize == null ? other$sourceFileSize != null : !this$sourceFileSize.equals(other$sourceFileSize)) return false;
        final java.lang.Object this$signatureStatus = this.getSignatureStatus();
        final java.lang.Object other$signatureStatus = other.getSignatureStatus();
        if (this$signatureStatus == null ? other$signatureStatus != null : !this$signatureStatus.equals(other$signatureStatus)) return false;
        final java.lang.Object this$verifyStatus = this.getVerifyStatus();
        final java.lang.Object other$verifyStatus = other.getVerifyStatus();
        if (this$verifyStatus == null ? other$verifyStatus != null : !this$verifyStatus.equals(other$verifyStatus)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$businessId = this.getBusinessId();
        final java.lang.Object other$businessId = other.getBusinessId();
        if (this$businessId == null ? other$businessId != null : !this$businessId.equals(other$businessId)) return false;
        final java.lang.Object this$financeVoucherId = this.getFinanceVoucherId();
        final java.lang.Object other$financeVoucherId = other.getFinanceVoucherId();
        if (this$financeVoucherId == null ? other$financeVoucherId != null : !this$financeVoucherId.equals(other$financeVoucherId)) return false;
        final java.lang.Object this$voucherType = this.getVoucherType();
        final java.lang.Object other$voucherType = other.getVoucherType();
        if (this$voucherType == null ? other$voucherType != null : !this$voucherType.equals(other$voucherType)) return false;
        final java.lang.Object this$voucherTypeName = this.getVoucherTypeName();
        final java.lang.Object other$voucherTypeName = other.getVoucherTypeName();
        if (this$voucherTypeName == null ? other$voucherTypeName != null : !this$voucherTypeName.equals(other$voucherTypeName)) return false;
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
        final java.lang.Object this$totalAmount = this.getTotalAmount();
        final java.lang.Object other$totalAmount = other.getTotalAmount();
        if (this$totalAmount == null ? other$totalAmount != null : !this$totalAmount.equals(other$totalAmount)) return false;
        final java.lang.Object this$currency = this.getCurrency();
        final java.lang.Object other$currency = other.getCurrency();
        if (this$currency == null ? other$currency != null : !this$currency.equals(other$currency)) return false;
        final java.lang.Object this$issueDate = this.getIssueDate();
        final java.lang.Object other$issueDate = other.getIssueDate();
        if (this$issueDate == null ? other$issueDate != null : !this$issueDate.equals(other$issueDate)) return false;
        final java.lang.Object this$signatureStatusName = this.getSignatureStatusName();
        final java.lang.Object other$signatureStatusName = other.getSignatureStatusName();
        if (this$signatureStatusName == null ? other$signatureStatusName != null : !this$signatureStatusName.equals(other$signatureStatusName)) return false;
        final java.lang.Object this$verifyStatusName = this.getVerifyStatusName();
        final java.lang.Object other$verifyStatusName = other.getVerifyStatusName();
        if (this$verifyStatusName == null ? other$verifyStatusName != null : !this$verifyStatusName.equals(other$verifyStatusName)) return false;
        final java.lang.Object this$verifyTime = this.getVerifyTime();
        final java.lang.Object other$verifyTime = other.getVerifyTime();
        if (this$verifyTime == null ? other$verifyTime != null : !this$verifyTime.equals(other$verifyTime)) return false;
        final java.lang.Object this$verifyMessage = this.getVerifyMessage();
        final java.lang.Object other$verifyMessage = other.getVerifyMessage();
        if (this$verifyMessage == null ? other$verifyMessage != null : !this$verifyMessage.equals(other$verifyMessage)) return false;
        final java.lang.Object this$statusName = this.getStatusName();
        final java.lang.Object other$statusName = other.getStatusName();
        if (this$statusName == null ? other$statusName != null : !this$statusName.equals(other$statusName)) return false;
        final java.lang.Object this$businessType = this.getBusinessType();
        final java.lang.Object other$businessType = other.getBusinessType();
        if (this$businessType == null ? other$businessType != null : !this$businessType.equals(other$businessType)) return false;
        final java.lang.Object this$invoice = this.getInvoice();
        final java.lang.Object other$invoice = other.getInvoice();
        if (this$invoice == null ? other$invoice != null : !this$invoice.equals(other$invoice)) return false;
        final java.lang.Object this$signatureLogs = this.getSignatureLogs();
        final java.lang.Object other$signatureLogs = other.getSignatureLogs();
        if (this$signatureLogs == null ? other$signatureLogs != null : !this$signatureLogs.equals(other$signatureLogs)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        final java.lang.Object this$createdByName = this.getCreatedByName();
        final java.lang.Object other$createdByName = other.getCreatedByName();
        if (this$createdByName == null ? other$createdByName != null : !this$createdByName.equals(other$createdByName)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ElectronicVoucherDetailVO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $sourceFileSize = this.getSourceFileSize();
        result = result * PRIME + ($sourceFileSize == null ? 43 : $sourceFileSize.hashCode());
        final java.lang.Object $signatureStatus = this.getSignatureStatus();
        result = result * PRIME + ($signatureStatus == null ? 43 : $signatureStatus.hashCode());
        final java.lang.Object $verifyStatus = this.getVerifyStatus();
        result = result * PRIME + ($verifyStatus == null ? 43 : $verifyStatus.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $businessId = this.getBusinessId();
        result = result * PRIME + ($businessId == null ? 43 : $businessId.hashCode());
        final java.lang.Object $financeVoucherId = this.getFinanceVoucherId();
        result = result * PRIME + ($financeVoucherId == null ? 43 : $financeVoucherId.hashCode());
        final java.lang.Object $voucherType = this.getVoucherType();
        result = result * PRIME + ($voucherType == null ? 43 : $voucherType.hashCode());
        final java.lang.Object $voucherTypeName = this.getVoucherTypeName();
        result = result * PRIME + ($voucherTypeName == null ? 43 : $voucherTypeName.hashCode());
        final java.lang.Object $voucherNo = this.getVoucherNo();
        result = result * PRIME + ($voucherNo == null ? 43 : $voucherNo.hashCode());
        final java.lang.Object $sourceFilePath = this.getSourceFilePath();
        result = result * PRIME + ($sourceFilePath == null ? 43 : $sourceFilePath.hashCode());
        final java.lang.Object $sourceFileHash = this.getSourceFileHash();
        result = result * PRIME + ($sourceFileHash == null ? 43 : $sourceFileHash.hashCode());
        final java.lang.Object $sourceFileType = this.getSourceFileType();
        result = result * PRIME + ($sourceFileType == null ? 43 : $sourceFileType.hashCode());
        final java.lang.Object $totalAmount = this.getTotalAmount();
        result = result * PRIME + ($totalAmount == null ? 43 : $totalAmount.hashCode());
        final java.lang.Object $currency = this.getCurrency();
        result = result * PRIME + ($currency == null ? 43 : $currency.hashCode());
        final java.lang.Object $issueDate = this.getIssueDate();
        result = result * PRIME + ($issueDate == null ? 43 : $issueDate.hashCode());
        final java.lang.Object $signatureStatusName = this.getSignatureStatusName();
        result = result * PRIME + ($signatureStatusName == null ? 43 : $signatureStatusName.hashCode());
        final java.lang.Object $verifyStatusName = this.getVerifyStatusName();
        result = result * PRIME + ($verifyStatusName == null ? 43 : $verifyStatusName.hashCode());
        final java.lang.Object $verifyTime = this.getVerifyTime();
        result = result * PRIME + ($verifyTime == null ? 43 : $verifyTime.hashCode());
        final java.lang.Object $verifyMessage = this.getVerifyMessage();
        result = result * PRIME + ($verifyMessage == null ? 43 : $verifyMessage.hashCode());
        final java.lang.Object $statusName = this.getStatusName();
        result = result * PRIME + ($statusName == null ? 43 : $statusName.hashCode());
        final java.lang.Object $businessType = this.getBusinessType();
        result = result * PRIME + ($businessType == null ? 43 : $businessType.hashCode());
        final java.lang.Object $invoice = this.getInvoice();
        result = result * PRIME + ($invoice == null ? 43 : $invoice.hashCode());
        final java.lang.Object $signatureLogs = this.getSignatureLogs();
        result = result * PRIME + ($signatureLogs == null ? 43 : $signatureLogs.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        final java.lang.Object $createdByName = this.getCreatedByName();
        result = result * PRIME + ($createdByName == null ? 43 : $createdByName.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "ElectronicVoucherDetailVO(id=" + this.getId() + ", voucherType=" + this.getVoucherType() + ", voucherTypeName=" + this.getVoucherTypeName() + ", voucherNo=" + this.getVoucherNo() + ", sourceFilePath=" + this.getSourceFilePath() + ", sourceFileHash=" + this.getSourceFileHash() + ", sourceFileSize=" + this.getSourceFileSize() + ", sourceFileType=" + this.getSourceFileType() + ", totalAmount=" + this.getTotalAmount() + ", currency=" + this.getCurrency() + ", issueDate=" + this.getIssueDate() + ", signatureStatus=" + this.getSignatureStatus() + ", signatureStatusName=" + this.getSignatureStatusName() + ", verifyStatus=" + this.getVerifyStatus() + ", verifyStatusName=" + this.getVerifyStatusName() + ", verifyTime=" + this.getVerifyTime() + ", verifyMessage=" + this.getVerifyMessage() + ", status=" + this.getStatus() + ", statusName=" + this.getStatusName() + ", businessType=" + this.getBusinessType() + ", businessId=" + this.getBusinessId() + ", financeVoucherId=" + this.getFinanceVoucherId() + ", invoice=" + this.getInvoice() + ", signatureLogs=" + this.getSignatureLogs() + ", createdAt=" + this.getCreatedAt() + ", createdByName=" + this.getCreatedByName() + ")";
    }
}
