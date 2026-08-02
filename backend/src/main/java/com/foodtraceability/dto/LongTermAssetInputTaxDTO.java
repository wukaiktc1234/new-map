package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 长期资产进项税额抵扣DTO
 *
 * 依据：财政部 税务总局公告2026年第15号《长期资产进项税额抵扣暂行办法》
 * 实施日期：2026年1月1日
 */
@Schema(description = "长期资产进项税额抵扣信息")
public class LongTermAssetInputTaxDTO {
    @Schema(description = "资产ID")
    private Long id;
    @Schema(description = "资产编号")
    private String assetNo;
    @Schema(description = "资产名称")
    private String assetName;
    @Schema(description = "资产类别：1-固定资产，2-无形资产，3-不动产")
    private Integer assetType;
    @Schema(description = "资产原值（不含税）")
    private BigDecimal originalValue;
    @Schema(description = "进项税额总额")
    private BigDecimal totalInputTax;
    @Schema(description = "是否混合用途：0-否，1-是")
    private Integer mixedUse;
    @Schema(description = "应税项目使用比例(%)")
    private BigDecimal taxableUseRatio;
    @Schema(description = "不得抵扣项目使用比例(%)")
    private BigDecimal nonDeductibleUseRatio;
    @Schema(description = "资产状态：1-正常使用，2-已处置")
    private Integer status;
    @Schema(description = "折旧/摊销年限")
    private Integer depreciationYears;
    @Schema(description = "年折旧/摊销额")
    private BigDecimal annualDepreciation;
    @Schema(description = "不得抵扣年折旧额")
    private BigDecimal nonDeductibleAnnualDepreciation;
    @Schema(description = "资产入账日期")
    private LocalDate accountingDate;
    @Schema(description = "开始调整年度")
    private Integer adjustmentStartYear;
    @Schema(description = "已调整年度数")
    private Integer adjustedYears;
    @Schema(description = "累计转出进项税额")
    private BigDecimal cumulativeTransferredTax;
    @Schema(description = "剩余可抵扣进项税额")
    private BigDecimal remainingDeductibleTax;
    @Schema(description = "是否大额资产（原值>500万元）：0-否，1-是")
    private Integer largeAsset;
    @Schema(description = "电子凭证ID")
    private Long electronicVoucherId;
    @Schema(description = "发票号码")
    private String invoiceNo;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
    @Schema(description = "年度调整记录列表")
    private List<AnnualAdjustmentRecord> adjustmentRecords;


    @Schema(description = "年度调整记录")
    public static class AnnualAdjustmentRecord {
        @Schema(description = "记录ID")
        private Long id;
        @Schema(description = "资产ID")
        private Long assetId;
        @Schema(description = "调整年度")
        private Integer adjustmentYear;
        @Schema(description = "不得抵扣年折旧额")
        private BigDecimal nonDeductibleDepreciation;
        @Schema(description = "年度应转出进项税额")
        private BigDecimal transferredTax;
        @Schema(description = "调整日期")
        private LocalDate adjustmentDate;
        @Schema(description = "会计凭证ID")
        private Long financeVoucherId;
        @Schema(description = "状态：0-待调整，1-已调整")
        private Integer status;
        @Schema(description = "备注")
        private String remark;

        public AnnualAdjustmentRecord() {
        }

        public Long getId() {
            return this.id;
        }

        public Long getAssetId() {
            return this.assetId;
        }

        public Integer getAdjustmentYear() {
            return this.adjustmentYear;
        }

        public BigDecimal getNonDeductibleDepreciation() {
            return this.nonDeductibleDepreciation;
        }

        public BigDecimal getTransferredTax() {
            return this.transferredTax;
        }

        public LocalDate getAdjustmentDate() {
            return this.adjustmentDate;
        }

        public Long getFinanceVoucherId() {
            return this.financeVoucherId;
        }

        public Integer getStatus() {
            return this.status;
        }

        public String getRemark() {
            return this.remark;
        }

        public void setId(final Long id) {
            this.id = id;
        }

        public void setAssetId(final Long assetId) {
            this.assetId = assetId;
        }

        public void setAdjustmentYear(final Integer adjustmentYear) {
            this.adjustmentYear = adjustmentYear;
        }

        public void setNonDeductibleDepreciation(final BigDecimal nonDeductibleDepreciation) {
            this.nonDeductibleDepreciation = nonDeductibleDepreciation;
        }

        public void setTransferredTax(final BigDecimal transferredTax) {
            this.transferredTax = transferredTax;
        }

        public void setAdjustmentDate(final LocalDate adjustmentDate) {
            this.adjustmentDate = adjustmentDate;
        }

        public void setFinanceVoucherId(final Long financeVoucherId) {
            this.financeVoucherId = financeVoucherId;
        }

        public void setStatus(final Integer status) {
            this.status = status;
        }

        public void setRemark(final String remark) {
            this.remark = remark;
        }

        @java.lang.Override
        public boolean equals(final java.lang.Object o) {
            if (o == this) return true;
            if (!(o instanceof LongTermAssetInputTaxDTO.AnnualAdjustmentRecord)) return false;
            final LongTermAssetInputTaxDTO.AnnualAdjustmentRecord other = (LongTermAssetInputTaxDTO.AnnualAdjustmentRecord) o;
            if (!other.canEqual((java.lang.Object) this)) return false;
            final java.lang.Object this$id = this.getId();
            final java.lang.Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            final java.lang.Object this$assetId = this.getAssetId();
            final java.lang.Object other$assetId = other.getAssetId();
            if (this$assetId == null ? other$assetId != null : !this$assetId.equals(other$assetId)) return false;
            final java.lang.Object this$adjustmentYear = this.getAdjustmentYear();
            final java.lang.Object other$adjustmentYear = other.getAdjustmentYear();
            if (this$adjustmentYear == null ? other$adjustmentYear != null : !this$adjustmentYear.equals(other$adjustmentYear)) return false;
            final java.lang.Object this$financeVoucherId = this.getFinanceVoucherId();
            final java.lang.Object other$financeVoucherId = other.getFinanceVoucherId();
            if (this$financeVoucherId == null ? other$financeVoucherId != null : !this$financeVoucherId.equals(other$financeVoucherId)) return false;
            final java.lang.Object this$status = this.getStatus();
            final java.lang.Object other$status = other.getStatus();
            if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
            final java.lang.Object this$nonDeductibleDepreciation = this.getNonDeductibleDepreciation();
            final java.lang.Object other$nonDeductibleDepreciation = other.getNonDeductibleDepreciation();
            if (this$nonDeductibleDepreciation == null ? other$nonDeductibleDepreciation != null : !this$nonDeductibleDepreciation.equals(other$nonDeductibleDepreciation)) return false;
            final java.lang.Object this$transferredTax = this.getTransferredTax();
            final java.lang.Object other$transferredTax = other.getTransferredTax();
            if (this$transferredTax == null ? other$transferredTax != null : !this$transferredTax.equals(other$transferredTax)) return false;
            final java.lang.Object this$adjustmentDate = this.getAdjustmentDate();
            final java.lang.Object other$adjustmentDate = other.getAdjustmentDate();
            if (this$adjustmentDate == null ? other$adjustmentDate != null : !this$adjustmentDate.equals(other$adjustmentDate)) return false;
            final java.lang.Object this$remark = this.getRemark();
            final java.lang.Object other$remark = other.getRemark();
            if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) return false;
            return true;
        }

        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof LongTermAssetInputTaxDTO.AnnualAdjustmentRecord;
        }

        @java.lang.Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final java.lang.Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            final java.lang.Object $assetId = this.getAssetId();
            result = result * PRIME + ($assetId == null ? 43 : $assetId.hashCode());
            final java.lang.Object $adjustmentYear = this.getAdjustmentYear();
            result = result * PRIME + ($adjustmentYear == null ? 43 : $adjustmentYear.hashCode());
            final java.lang.Object $financeVoucherId = this.getFinanceVoucherId();
            result = result * PRIME + ($financeVoucherId == null ? 43 : $financeVoucherId.hashCode());
            final java.lang.Object $status = this.getStatus();
            result = result * PRIME + ($status == null ? 43 : $status.hashCode());
            final java.lang.Object $nonDeductibleDepreciation = this.getNonDeductibleDepreciation();
            result = result * PRIME + ($nonDeductibleDepreciation == null ? 43 : $nonDeductibleDepreciation.hashCode());
            final java.lang.Object $transferredTax = this.getTransferredTax();
            result = result * PRIME + ($transferredTax == null ? 43 : $transferredTax.hashCode());
            final java.lang.Object $adjustmentDate = this.getAdjustmentDate();
            result = result * PRIME + ($adjustmentDate == null ? 43 : $adjustmentDate.hashCode());
            final java.lang.Object $remark = this.getRemark();
            result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
            return result;
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "LongTermAssetInputTaxDTO.AnnualAdjustmentRecord(id=" + this.getId() + ", assetId=" + this.getAssetId() + ", adjustmentYear=" + this.getAdjustmentYear() + ", nonDeductibleDepreciation=" + this.getNonDeductibleDepreciation() + ", transferredTax=" + this.getTransferredTax() + ", adjustmentDate=" + this.getAdjustmentDate() + ", financeVoucherId=" + this.getFinanceVoucherId() + ", status=" + this.getStatus() + ", remark=" + this.getRemark() + ")";
        }
    }


    @Schema(description = "年度调整结果")
    public static class AnnualAdjustmentResult {
        @Schema(description = "是否成功")
        private Boolean success;
        @Schema(description = "资产ID")
        private Long assetId;
        @Schema(description = "调整年度")
        private Integer adjustmentYear;
        @Schema(description = "转出进项税额")
        private BigDecimal transferredTax;
        @Schema(description = "生成的会计凭证ID")
        private Long financeVoucherId;
        @Schema(description = "错误信息")
        private String errorMessage;

        public AnnualAdjustmentResult() {
        }

        public Boolean getSuccess() {
            return this.success;
        }

        public Long getAssetId() {
            return this.assetId;
        }

        public Integer getAdjustmentYear() {
            return this.adjustmentYear;
        }

        public BigDecimal getTransferredTax() {
            return this.transferredTax;
        }

        public Long getFinanceVoucherId() {
            return this.financeVoucherId;
        }

        public String getErrorMessage() {
            return this.errorMessage;
        }

        public void setSuccess(final Boolean success) {
            this.success = success;
        }

        public void setAssetId(final Long assetId) {
            this.assetId = assetId;
        }

        public void setAdjustmentYear(final Integer adjustmentYear) {
            this.adjustmentYear = adjustmentYear;
        }

        public void setTransferredTax(final BigDecimal transferredTax) {
            this.transferredTax = transferredTax;
        }

        public void setFinanceVoucherId(final Long financeVoucherId) {
            this.financeVoucherId = financeVoucherId;
        }

        public void setErrorMessage(final String errorMessage) {
            this.errorMessage = errorMessage;
        }

        @java.lang.Override
        public boolean equals(final java.lang.Object o) {
            if (o == this) return true;
            if (!(o instanceof LongTermAssetInputTaxDTO.AnnualAdjustmentResult)) return false;
            final LongTermAssetInputTaxDTO.AnnualAdjustmentResult other = (LongTermAssetInputTaxDTO.AnnualAdjustmentResult) o;
            if (!other.canEqual((java.lang.Object) this)) return false;
            final java.lang.Object this$success = this.getSuccess();
            final java.lang.Object other$success = other.getSuccess();
            if (this$success == null ? other$success != null : !this$success.equals(other$success)) return false;
            final java.lang.Object this$assetId = this.getAssetId();
            final java.lang.Object other$assetId = other.getAssetId();
            if (this$assetId == null ? other$assetId != null : !this$assetId.equals(other$assetId)) return false;
            final java.lang.Object this$adjustmentYear = this.getAdjustmentYear();
            final java.lang.Object other$adjustmentYear = other.getAdjustmentYear();
            if (this$adjustmentYear == null ? other$adjustmentYear != null : !this$adjustmentYear.equals(other$adjustmentYear)) return false;
            final java.lang.Object this$financeVoucherId = this.getFinanceVoucherId();
            final java.lang.Object other$financeVoucherId = other.getFinanceVoucherId();
            if (this$financeVoucherId == null ? other$financeVoucherId != null : !this$financeVoucherId.equals(other$financeVoucherId)) return false;
            final java.lang.Object this$transferredTax = this.getTransferredTax();
            final java.lang.Object other$transferredTax = other.getTransferredTax();
            if (this$transferredTax == null ? other$transferredTax != null : !this$transferredTax.equals(other$transferredTax)) return false;
            final java.lang.Object this$errorMessage = this.getErrorMessage();
            final java.lang.Object other$errorMessage = other.getErrorMessage();
            if (this$errorMessage == null ? other$errorMessage != null : !this$errorMessage.equals(other$errorMessage)) return false;
            return true;
        }

        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof LongTermAssetInputTaxDTO.AnnualAdjustmentResult;
        }

        @java.lang.Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final java.lang.Object $success = this.getSuccess();
            result = result * PRIME + ($success == null ? 43 : $success.hashCode());
            final java.lang.Object $assetId = this.getAssetId();
            result = result * PRIME + ($assetId == null ? 43 : $assetId.hashCode());
            final java.lang.Object $adjustmentYear = this.getAdjustmentYear();
            result = result * PRIME + ($adjustmentYear == null ? 43 : $adjustmentYear.hashCode());
            final java.lang.Object $financeVoucherId = this.getFinanceVoucherId();
            result = result * PRIME + ($financeVoucherId == null ? 43 : $financeVoucherId.hashCode());
            final java.lang.Object $transferredTax = this.getTransferredTax();
            result = result * PRIME + ($transferredTax == null ? 43 : $transferredTax.hashCode());
            final java.lang.Object $errorMessage = this.getErrorMessage();
            result = result * PRIME + ($errorMessage == null ? 43 : $errorMessage.hashCode());
            return result;
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "LongTermAssetInputTaxDTO.AnnualAdjustmentResult(success=" + this.getSuccess() + ", assetId=" + this.getAssetId() + ", adjustmentYear=" + this.getAdjustmentYear() + ", transferredTax=" + this.getTransferredTax() + ", financeVoucherId=" + this.getFinanceVoucherId() + ", errorMessage=" + this.getErrorMessage() + ")";
        }
    }

    public LongTermAssetInputTaxDTO() {
    }

    public Long getId() {
        return this.id;
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

    public BigDecimal getOriginalValue() {
        return this.originalValue;
    }

    public BigDecimal getTotalInputTax() {
        return this.totalInputTax;
    }

    public Integer getMixedUse() {
        return this.mixedUse;
    }

    public BigDecimal getTaxableUseRatio() {
        return this.taxableUseRatio;
    }

    public BigDecimal getNonDeductibleUseRatio() {
        return this.nonDeductibleUseRatio;
    }

    public Integer getStatus() {
        return this.status;
    }

    public Integer getDepreciationYears() {
        return this.depreciationYears;
    }

    public BigDecimal getAnnualDepreciation() {
        return this.annualDepreciation;
    }

    public BigDecimal getNonDeductibleAnnualDepreciation() {
        return this.nonDeductibleAnnualDepreciation;
    }

    public LocalDate getAccountingDate() {
        return this.accountingDate;
    }

    public Integer getAdjustmentStartYear() {
        return this.adjustmentStartYear;
    }

    public Integer getAdjustedYears() {
        return this.adjustedYears;
    }

    public BigDecimal getCumulativeTransferredTax() {
        return this.cumulativeTransferredTax;
    }

    public BigDecimal getRemainingDeductibleTax() {
        return this.remainingDeductibleTax;
    }

    public Integer getLargeAsset() {
        return this.largeAsset;
    }

    public Long getElectronicVoucherId() {
        return this.electronicVoucherId;
    }

    public String getInvoiceNo() {
        return this.invoiceNo;
    }

    public String getRemark() {
        return this.remark;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public List<AnnualAdjustmentRecord> getAdjustmentRecords() {
        return this.adjustmentRecords;
    }

    public void setId(final Long id) {
        this.id = id;
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

    public void setOriginalValue(final BigDecimal originalValue) {
        this.originalValue = originalValue;
    }

    public void setTotalInputTax(final BigDecimal totalInputTax) {
        this.totalInputTax = totalInputTax;
    }

    public void setMixedUse(final Integer mixedUse) {
        this.mixedUse = mixedUse;
    }

    public void setTaxableUseRatio(final BigDecimal taxableUseRatio) {
        this.taxableUseRatio = taxableUseRatio;
    }

    public void setNonDeductibleUseRatio(final BigDecimal nonDeductibleUseRatio) {
        this.nonDeductibleUseRatio = nonDeductibleUseRatio;
    }

    public void setStatus(final Integer status) {
        this.status = status;
    }

    public void setDepreciationYears(final Integer depreciationYears) {
        this.depreciationYears = depreciationYears;
    }

    public void setAnnualDepreciation(final BigDecimal annualDepreciation) {
        this.annualDepreciation = annualDepreciation;
    }

    public void setNonDeductibleAnnualDepreciation(final BigDecimal nonDeductibleAnnualDepreciation) {
        this.nonDeductibleAnnualDepreciation = nonDeductibleAnnualDepreciation;
    }

    public void setAccountingDate(final LocalDate accountingDate) {
        this.accountingDate = accountingDate;
    }

    public void setAdjustmentStartYear(final Integer adjustmentStartYear) {
        this.adjustmentStartYear = adjustmentStartYear;
    }

    public void setAdjustedYears(final Integer adjustedYears) {
        this.adjustedYears = adjustedYears;
    }

    public void setCumulativeTransferredTax(final BigDecimal cumulativeTransferredTax) {
        this.cumulativeTransferredTax = cumulativeTransferredTax;
    }

    public void setRemainingDeductibleTax(final BigDecimal remainingDeductibleTax) {
        this.remainingDeductibleTax = remainingDeductibleTax;
    }

    public void setLargeAsset(final Integer largeAsset) {
        this.largeAsset = largeAsset;
    }

    public void setElectronicVoucherId(final Long electronicVoucherId) {
        this.electronicVoucherId = electronicVoucherId;
    }

    public void setInvoiceNo(final String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public void setRemark(final String remark) {
        this.remark = remark;
    }

    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(final LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setAdjustmentRecords(final List<AnnualAdjustmentRecord> adjustmentRecords) {
        this.adjustmentRecords = adjustmentRecords;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof LongTermAssetInputTaxDTO)) return false;
        final LongTermAssetInputTaxDTO other = (LongTermAssetInputTaxDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$assetType = this.getAssetType();
        final java.lang.Object other$assetType = other.getAssetType();
        if (this$assetType == null ? other$assetType != null : !this$assetType.equals(other$assetType)) return false;
        final java.lang.Object this$mixedUse = this.getMixedUse();
        final java.lang.Object other$mixedUse = other.getMixedUse();
        if (this$mixedUse == null ? other$mixedUse != null : !this$mixedUse.equals(other$mixedUse)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$depreciationYears = this.getDepreciationYears();
        final java.lang.Object other$depreciationYears = other.getDepreciationYears();
        if (this$depreciationYears == null ? other$depreciationYears != null : !this$depreciationYears.equals(other$depreciationYears)) return false;
        final java.lang.Object this$adjustmentStartYear = this.getAdjustmentStartYear();
        final java.lang.Object other$adjustmentStartYear = other.getAdjustmentStartYear();
        if (this$adjustmentStartYear == null ? other$adjustmentStartYear != null : !this$adjustmentStartYear.equals(other$adjustmentStartYear)) return false;
        final java.lang.Object this$adjustedYears = this.getAdjustedYears();
        final java.lang.Object other$adjustedYears = other.getAdjustedYears();
        if (this$adjustedYears == null ? other$adjustedYears != null : !this$adjustedYears.equals(other$adjustedYears)) return false;
        final java.lang.Object this$largeAsset = this.getLargeAsset();
        final java.lang.Object other$largeAsset = other.getLargeAsset();
        if (this$largeAsset == null ? other$largeAsset != null : !this$largeAsset.equals(other$largeAsset)) return false;
        final java.lang.Object this$electronicVoucherId = this.getElectronicVoucherId();
        final java.lang.Object other$electronicVoucherId = other.getElectronicVoucherId();
        if (this$electronicVoucherId == null ? other$electronicVoucherId != null : !this$electronicVoucherId.equals(other$electronicVoucherId)) return false;
        final java.lang.Object this$assetNo = this.getAssetNo();
        final java.lang.Object other$assetNo = other.getAssetNo();
        if (this$assetNo == null ? other$assetNo != null : !this$assetNo.equals(other$assetNo)) return false;
        final java.lang.Object this$assetName = this.getAssetName();
        final java.lang.Object other$assetName = other.getAssetName();
        if (this$assetName == null ? other$assetName != null : !this$assetName.equals(other$assetName)) return false;
        final java.lang.Object this$originalValue = this.getOriginalValue();
        final java.lang.Object other$originalValue = other.getOriginalValue();
        if (this$originalValue == null ? other$originalValue != null : !this$originalValue.equals(other$originalValue)) return false;
        final java.lang.Object this$totalInputTax = this.getTotalInputTax();
        final java.lang.Object other$totalInputTax = other.getTotalInputTax();
        if (this$totalInputTax == null ? other$totalInputTax != null : !this$totalInputTax.equals(other$totalInputTax)) return false;
        final java.lang.Object this$taxableUseRatio = this.getTaxableUseRatio();
        final java.lang.Object other$taxableUseRatio = other.getTaxableUseRatio();
        if (this$taxableUseRatio == null ? other$taxableUseRatio != null : !this$taxableUseRatio.equals(other$taxableUseRatio)) return false;
        final java.lang.Object this$nonDeductibleUseRatio = this.getNonDeductibleUseRatio();
        final java.lang.Object other$nonDeductibleUseRatio = other.getNonDeductibleUseRatio();
        if (this$nonDeductibleUseRatio == null ? other$nonDeductibleUseRatio != null : !this$nonDeductibleUseRatio.equals(other$nonDeductibleUseRatio)) return false;
        final java.lang.Object this$annualDepreciation = this.getAnnualDepreciation();
        final java.lang.Object other$annualDepreciation = other.getAnnualDepreciation();
        if (this$annualDepreciation == null ? other$annualDepreciation != null : !this$annualDepreciation.equals(other$annualDepreciation)) return false;
        final java.lang.Object this$nonDeductibleAnnualDepreciation = this.getNonDeductibleAnnualDepreciation();
        final java.lang.Object other$nonDeductibleAnnualDepreciation = other.getNonDeductibleAnnualDepreciation();
        if (this$nonDeductibleAnnualDepreciation == null ? other$nonDeductibleAnnualDepreciation != null : !this$nonDeductibleAnnualDepreciation.equals(other$nonDeductibleAnnualDepreciation)) return false;
        final java.lang.Object this$accountingDate = this.getAccountingDate();
        final java.lang.Object other$accountingDate = other.getAccountingDate();
        if (this$accountingDate == null ? other$accountingDate != null : !this$accountingDate.equals(other$accountingDate)) return false;
        final java.lang.Object this$cumulativeTransferredTax = this.getCumulativeTransferredTax();
        final java.lang.Object other$cumulativeTransferredTax = other.getCumulativeTransferredTax();
        if (this$cumulativeTransferredTax == null ? other$cumulativeTransferredTax != null : !this$cumulativeTransferredTax.equals(other$cumulativeTransferredTax)) return false;
        final java.lang.Object this$remainingDeductibleTax = this.getRemainingDeductibleTax();
        final java.lang.Object other$remainingDeductibleTax = other.getRemainingDeductibleTax();
        if (this$remainingDeductibleTax == null ? other$remainingDeductibleTax != null : !this$remainingDeductibleTax.equals(other$remainingDeductibleTax)) return false;
        final java.lang.Object this$invoiceNo = this.getInvoiceNo();
        final java.lang.Object other$invoiceNo = other.getInvoiceNo();
        if (this$invoiceNo == null ? other$invoiceNo != null : !this$invoiceNo.equals(other$invoiceNo)) return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        final java.lang.Object this$updatedAt = this.getUpdatedAt();
        final java.lang.Object other$updatedAt = other.getUpdatedAt();
        if (this$updatedAt == null ? other$updatedAt != null : !this$updatedAt.equals(other$updatedAt)) return false;
        final java.lang.Object this$adjustmentRecords = this.getAdjustmentRecords();
        final java.lang.Object other$adjustmentRecords = other.getAdjustmentRecords();
        if (this$adjustmentRecords == null ? other$adjustmentRecords != null : !this$adjustmentRecords.equals(other$adjustmentRecords)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof LongTermAssetInputTaxDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $assetType = this.getAssetType();
        result = result * PRIME + ($assetType == null ? 43 : $assetType.hashCode());
        final java.lang.Object $mixedUse = this.getMixedUse();
        result = result * PRIME + ($mixedUse == null ? 43 : $mixedUse.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $depreciationYears = this.getDepreciationYears();
        result = result * PRIME + ($depreciationYears == null ? 43 : $depreciationYears.hashCode());
        final java.lang.Object $adjustmentStartYear = this.getAdjustmentStartYear();
        result = result * PRIME + ($adjustmentStartYear == null ? 43 : $adjustmentStartYear.hashCode());
        final java.lang.Object $adjustedYears = this.getAdjustedYears();
        result = result * PRIME + ($adjustedYears == null ? 43 : $adjustedYears.hashCode());
        final java.lang.Object $largeAsset = this.getLargeAsset();
        result = result * PRIME + ($largeAsset == null ? 43 : $largeAsset.hashCode());
        final java.lang.Object $electronicVoucherId = this.getElectronicVoucherId();
        result = result * PRIME + ($electronicVoucherId == null ? 43 : $electronicVoucherId.hashCode());
        final java.lang.Object $assetNo = this.getAssetNo();
        result = result * PRIME + ($assetNo == null ? 43 : $assetNo.hashCode());
        final java.lang.Object $assetName = this.getAssetName();
        result = result * PRIME + ($assetName == null ? 43 : $assetName.hashCode());
        final java.lang.Object $originalValue = this.getOriginalValue();
        result = result * PRIME + ($originalValue == null ? 43 : $originalValue.hashCode());
        final java.lang.Object $totalInputTax = this.getTotalInputTax();
        result = result * PRIME + ($totalInputTax == null ? 43 : $totalInputTax.hashCode());
        final java.lang.Object $taxableUseRatio = this.getTaxableUseRatio();
        result = result * PRIME + ($taxableUseRatio == null ? 43 : $taxableUseRatio.hashCode());
        final java.lang.Object $nonDeductibleUseRatio = this.getNonDeductibleUseRatio();
        result = result * PRIME + ($nonDeductibleUseRatio == null ? 43 : $nonDeductibleUseRatio.hashCode());
        final java.lang.Object $annualDepreciation = this.getAnnualDepreciation();
        result = result * PRIME + ($annualDepreciation == null ? 43 : $annualDepreciation.hashCode());
        final java.lang.Object $nonDeductibleAnnualDepreciation = this.getNonDeductibleAnnualDepreciation();
        result = result * PRIME + ($nonDeductibleAnnualDepreciation == null ? 43 : $nonDeductibleAnnualDepreciation.hashCode());
        final java.lang.Object $accountingDate = this.getAccountingDate();
        result = result * PRIME + ($accountingDate == null ? 43 : $accountingDate.hashCode());
        final java.lang.Object $cumulativeTransferredTax = this.getCumulativeTransferredTax();
        result = result * PRIME + ($cumulativeTransferredTax == null ? 43 : $cumulativeTransferredTax.hashCode());
        final java.lang.Object $remainingDeductibleTax = this.getRemainingDeductibleTax();
        result = result * PRIME + ($remainingDeductibleTax == null ? 43 : $remainingDeductibleTax.hashCode());
        final java.lang.Object $invoiceNo = this.getInvoiceNo();
        result = result * PRIME + ($invoiceNo == null ? 43 : $invoiceNo.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        final java.lang.Object $updatedAt = this.getUpdatedAt();
        result = result * PRIME + ($updatedAt == null ? 43 : $updatedAt.hashCode());
        final java.lang.Object $adjustmentRecords = this.getAdjustmentRecords();
        result = result * PRIME + ($adjustmentRecords == null ? 43 : $adjustmentRecords.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "LongTermAssetInputTaxDTO(id=" + this.getId() + ", assetNo=" + this.getAssetNo() + ", assetName=" + this.getAssetName() + ", assetType=" + this.getAssetType() + ", originalValue=" + this.getOriginalValue() + ", totalInputTax=" + this.getTotalInputTax() + ", mixedUse=" + this.getMixedUse() + ", taxableUseRatio=" + this.getTaxableUseRatio() + ", nonDeductibleUseRatio=" + this.getNonDeductibleUseRatio() + ", status=" + this.getStatus() + ", depreciationYears=" + this.getDepreciationYears() + ", annualDepreciation=" + this.getAnnualDepreciation() + ", nonDeductibleAnnualDepreciation=" + this.getNonDeductibleAnnualDepreciation() + ", accountingDate=" + this.getAccountingDate() + ", adjustmentStartYear=" + this.getAdjustmentStartYear() + ", adjustedYears=" + this.getAdjustedYears() + ", cumulativeTransferredTax=" + this.getCumulativeTransferredTax() + ", remainingDeductibleTax=" + this.getRemainingDeductibleTax() + ", largeAsset=" + this.getLargeAsset() + ", electronicVoucherId=" + this.getElectronicVoucherId() + ", invoiceNo=" + this.getInvoiceNo() + ", remark=" + this.getRemark() + ", createdAt=" + this.getCreatedAt() + ", updatedAt=" + this.getUpdatedAt() + ", adjustmentRecords=" + this.getAdjustmentRecords() + ")";
    }
}
