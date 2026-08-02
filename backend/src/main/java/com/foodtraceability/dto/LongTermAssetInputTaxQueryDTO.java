package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 长期资产进项税额抵扣查询DTO
 *
 * 依据：财政部 税务总局公告2026年第15号《长期资产进项税额抵扣暂行办法》
 */
@Schema(description = "长期资产进项税额抵扣查询条件")
public class LongTermAssetInputTaxQueryDTO extends PageQuery {
    @Schema(description = "资产编号")
    private String assetNo;
    @Schema(description = "资产名称")
    private String assetName;
    @Schema(description = "资产类别：1-固定资产，2-无形资产，3-不动产")
    private Integer assetType;
    @Schema(description = "是否混合用途：0-否，1-是")
    private Integer mixedUse;
    @Schema(description = "是否大额资产：0-否，1-是")
    private Integer largeAsset;
    @Schema(description = "资产状态：1-正常使用，2-已处置")
    private Integer status;
    @Schema(description = "入账日期起")
    private String accountingDateFrom;
    @Schema(description = "入账日期止")
    private String accountingDateTo;
    @Schema(description = "发票号码")
    private String invoiceNo;

    public LongTermAssetInputTaxQueryDTO() {
    }

    public String getAssetNo() {
        return this.assetNo;
    }

    public String getAssetName() {
        return this.assetName;
    }

    public Integer getAssetType() {
        return this.assetType;
    }

    public Integer getMixedUse() {
        return this.mixedUse;
    }

    public Integer getLargeAsset() {
        return this.largeAsset;
    }

    public Integer getStatus() {
        return this.status;
    }

    public String getAccountingDateFrom() {
        return this.accountingDateFrom;
    }

    public String getAccountingDateTo() {
        return this.accountingDateTo;
    }

    public String getInvoiceNo() {
        return this.invoiceNo;
    }

    public void setAssetNo(final String assetNo) {
        this.assetNo = assetNo;
    }

    public void setAssetName(final String assetName) {
        this.assetName = assetName;
    }

    public void setAssetType(final Integer assetType) {
        this.assetType = assetType;
    }

    public void setMixedUse(final Integer mixedUse) {
        this.mixedUse = mixedUse;
    }

    public void setLargeAsset(final Integer largeAsset) {
        this.largeAsset = largeAsset;
    }

    public void setStatus(final Integer status) {
        this.status = status;
    }

    public void setAccountingDateFrom(final String accountingDateFrom) {
        this.accountingDateFrom = accountingDateFrom;
    }

    public void setAccountingDateTo(final String accountingDateTo) {
        this.accountingDateTo = accountingDateTo;
    }

    public void setInvoiceNo(final String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "LongTermAssetInputTaxQueryDTO(assetNo=" + this.getAssetNo() + ", assetName=" + this.getAssetName() + ", assetType=" + this.getAssetType() + ", mixedUse=" + this.getMixedUse() + ", largeAsset=" + this.getLargeAsset() + ", status=" + this.getStatus() + ", accountingDateFrom=" + this.getAccountingDateFrom() + ", accountingDateTo=" + this.getAccountingDateTo() + ", invoiceNo=" + this.getInvoiceNo() + ")";
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof LongTermAssetInputTaxQueryDTO)) return false;
        final LongTermAssetInputTaxQueryDTO other = (LongTermAssetInputTaxQueryDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (!super.equals(o)) return false;
        final java.lang.Object this$assetType = this.getAssetType();
        final java.lang.Object other$assetType = other.getAssetType();
        if (this$assetType == null ? other$assetType != null : !this$assetType.equals(other$assetType)) return false;
        final java.lang.Object this$mixedUse = this.getMixedUse();
        final java.lang.Object other$mixedUse = other.getMixedUse();
        if (this$mixedUse == null ? other$mixedUse != null : !this$mixedUse.equals(other$mixedUse)) return false;
        final java.lang.Object this$largeAsset = this.getLargeAsset();
        final java.lang.Object other$largeAsset = other.getLargeAsset();
        if (this$largeAsset == null ? other$largeAsset != null : !this$largeAsset.equals(other$largeAsset)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$assetNo = this.getAssetNo();
        final java.lang.Object other$assetNo = other.getAssetNo();
        if (this$assetNo == null ? other$assetNo != null : !this$assetNo.equals(other$assetNo)) return false;
        final java.lang.Object this$assetName = this.getAssetName();
        final java.lang.Object other$assetName = other.getAssetName();
        if (this$assetName == null ? other$assetName != null : !this$assetName.equals(other$assetName)) return false;
        final java.lang.Object this$accountingDateFrom = this.getAccountingDateFrom();
        final java.lang.Object other$accountingDateFrom = other.getAccountingDateFrom();
        if (this$accountingDateFrom == null ? other$accountingDateFrom != null : !this$accountingDateFrom.equals(other$accountingDateFrom)) return false;
        final java.lang.Object this$accountingDateTo = this.getAccountingDateTo();
        final java.lang.Object other$accountingDateTo = other.getAccountingDateTo();
        if (this$accountingDateTo == null ? other$accountingDateTo != null : !this$accountingDateTo.equals(other$accountingDateTo)) return false;
        final java.lang.Object this$invoiceNo = this.getInvoiceNo();
        final java.lang.Object other$invoiceNo = other.getInvoiceNo();
        if (this$invoiceNo == null ? other$invoiceNo != null : !this$invoiceNo.equals(other$invoiceNo)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof LongTermAssetInputTaxQueryDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = super.hashCode();
        final java.lang.Object $assetType = this.getAssetType();
        result = result * PRIME + ($assetType == null ? 43 : $assetType.hashCode());
        final java.lang.Object $mixedUse = this.getMixedUse();
        result = result * PRIME + ($mixedUse == null ? 43 : $mixedUse.hashCode());
        final java.lang.Object $largeAsset = this.getLargeAsset();
        result = result * PRIME + ($largeAsset == null ? 43 : $largeAsset.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $assetNo = this.getAssetNo();
        result = result * PRIME + ($assetNo == null ? 43 : $assetNo.hashCode());
        final java.lang.Object $assetName = this.getAssetName();
        result = result * PRIME + ($assetName == null ? 43 : $assetName.hashCode());
        final java.lang.Object $accountingDateFrom = this.getAccountingDateFrom();
        result = result * PRIME + ($accountingDateFrom == null ? 43 : $accountingDateFrom.hashCode());
        final java.lang.Object $accountingDateTo = this.getAccountingDateTo();
        result = result * PRIME + ($accountingDateTo == null ? 43 : $accountingDateTo.hashCode());
        final java.lang.Object $invoiceNo = this.getInvoiceNo();
        result = result * PRIME + ($invoiceNo == null ? 43 : $invoiceNo.hashCode());
        return result;
    }
}
