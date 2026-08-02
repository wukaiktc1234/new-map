package com.foodtraceability.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 采购入库进度DTO
 */
public class StockinProgressDTO {
    private Long stockinId;
    private String stockinNo;
    private String status;
    private int totalLabelCount;
    private int scannedLabelCount;
    private int pendingLabelCount;
    private int errorLabelCount;
    private BigDecimal totalQuantity;
    private BigDecimal scannedQuantity;
    private BigDecimal pendingQuantity;
    private List<LabelProgressItem> labels = new ArrayList<>();
    private boolean canComplete;
    private boolean hasErrors;
    private String lastScanTime;
    private String lastScanOperator;


    public static class LabelProgressItem {
        private String traceCodeId;
        private String traceCode;
        private String materialName;
        private BigDecimal quantity;
        private String status;
        private String scanTime;
        private String scanOperator;
        private boolean hasError;
        private String errorMessage;

        public LabelProgressItem() {
        }

        public String getTraceCodeId() {
            return this.traceCodeId;
        }

        public String getTraceCode() {
            return this.traceCode;
        }

        public String getMaterialName() {
            return this.materialName;
        }

        public BigDecimal getQuantity() {
            return this.quantity;
        }

        public String getStatus() {
            return this.status;
        }

        public String getScanTime() {
            return this.scanTime;
        }

        public String getScanOperator() {
            return this.scanOperator;
        }

        public boolean isHasError() {
            return this.hasError;
        }

        public String getErrorMessage() {
            return this.errorMessage;
        }

        public void setTraceCodeId(final String traceCodeId) {
            this.traceCodeId = traceCodeId;
        }

        public void setTraceCode(final String traceCode) {
            this.traceCode = traceCode;
        }

        public void setMaterialName(final String materialName) {
            this.materialName = materialName;
        }

        public void setQuantity(final BigDecimal quantity) {
            this.quantity = quantity;
        }

        public void setStatus(final String status) {
            this.status = status;
        }

        public void setScanTime(final String scanTime) {
            this.scanTime = scanTime;
        }

        public void setScanOperator(final String scanOperator) {
            this.scanOperator = scanOperator;
        }

        public void setHasError(final boolean hasError) {
            this.hasError = hasError;
        }

        public void setErrorMessage(final String errorMessage) {
            this.errorMessage = errorMessage;
        }

        @java.lang.Override
        public boolean equals(final java.lang.Object o) {
            if (o == this) return true;
            if (!(o instanceof StockinProgressDTO.LabelProgressItem)) return false;
            final StockinProgressDTO.LabelProgressItem other = (StockinProgressDTO.LabelProgressItem) o;
            if (!other.canEqual((java.lang.Object) this)) return false;
            if (this.isHasError() != other.isHasError()) return false;
            final java.lang.Object this$traceCodeId = this.getTraceCodeId();
            final java.lang.Object other$traceCodeId = other.getTraceCodeId();
            if (this$traceCodeId == null ? other$traceCodeId != null : !this$traceCodeId.equals(other$traceCodeId)) return false;
            final java.lang.Object this$traceCode = this.getTraceCode();
            final java.lang.Object other$traceCode = other.getTraceCode();
            if (this$traceCode == null ? other$traceCode != null : !this$traceCode.equals(other$traceCode)) return false;
            final java.lang.Object this$materialName = this.getMaterialName();
            final java.lang.Object other$materialName = other.getMaterialName();
            if (this$materialName == null ? other$materialName != null : !this$materialName.equals(other$materialName)) return false;
            final java.lang.Object this$quantity = this.getQuantity();
            final java.lang.Object other$quantity = other.getQuantity();
            if (this$quantity == null ? other$quantity != null : !this$quantity.equals(other$quantity)) return false;
            final java.lang.Object this$status = this.getStatus();
            final java.lang.Object other$status = other.getStatus();
            if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
            final java.lang.Object this$scanTime = this.getScanTime();
            final java.lang.Object other$scanTime = other.getScanTime();
            if (this$scanTime == null ? other$scanTime != null : !this$scanTime.equals(other$scanTime)) return false;
            final java.lang.Object this$scanOperator = this.getScanOperator();
            final java.lang.Object other$scanOperator = other.getScanOperator();
            if (this$scanOperator == null ? other$scanOperator != null : !this$scanOperator.equals(other$scanOperator)) return false;
            final java.lang.Object this$errorMessage = this.getErrorMessage();
            final java.lang.Object other$errorMessage = other.getErrorMessage();
            if (this$errorMessage == null ? other$errorMessage != null : !this$errorMessage.equals(other$errorMessage)) return false;
            return true;
        }

        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof StockinProgressDTO.LabelProgressItem;
        }

        @java.lang.Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            result = result * PRIME + (this.isHasError() ? 79 : 97);
            final java.lang.Object $traceCodeId = this.getTraceCodeId();
            result = result * PRIME + ($traceCodeId == null ? 43 : $traceCodeId.hashCode());
            final java.lang.Object $traceCode = this.getTraceCode();
            result = result * PRIME + ($traceCode == null ? 43 : $traceCode.hashCode());
            final java.lang.Object $materialName = this.getMaterialName();
            result = result * PRIME + ($materialName == null ? 43 : $materialName.hashCode());
            final java.lang.Object $quantity = this.getQuantity();
            result = result * PRIME + ($quantity == null ? 43 : $quantity.hashCode());
            final java.lang.Object $status = this.getStatus();
            result = result * PRIME + ($status == null ? 43 : $status.hashCode());
            final java.lang.Object $scanTime = this.getScanTime();
            result = result * PRIME + ($scanTime == null ? 43 : $scanTime.hashCode());
            final java.lang.Object $scanOperator = this.getScanOperator();
            result = result * PRIME + ($scanOperator == null ? 43 : $scanOperator.hashCode());
            final java.lang.Object $errorMessage = this.getErrorMessage();
            result = result * PRIME + ($errorMessage == null ? 43 : $errorMessage.hashCode());
            return result;
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "StockinProgressDTO.LabelProgressItem(traceCodeId=" + this.getTraceCodeId() + ", traceCode=" + this.getTraceCode() + ", materialName=" + this.getMaterialName() + ", quantity=" + this.getQuantity() + ", status=" + this.getStatus() + ", scanTime=" + this.getScanTime() + ", scanOperator=" + this.getScanOperator() + ", hasError=" + this.isHasError() + ", errorMessage=" + this.getErrorMessage() + ")";
        }
    }

    public double getProgressPercentage() {
        if (totalLabelCount == 0) return 0;
        return (double) scannedLabelCount / totalLabelCount * 100;
    }

    public boolean isProgressComplete() {
        return pendingLabelCount == 0 && errorLabelCount == 0;
    }

    public StockinProgressDTO() {
    }

    public Long getStockinId() {
        return this.stockinId;
    }

    public String getStockinNo() {
        return this.stockinNo;
    }

    public String getStatus() {
        return this.status;
    }

    public int getTotalLabelCount() {
        return this.totalLabelCount;
    }

    public int getScannedLabelCount() {
        return this.scannedLabelCount;
    }

    public int getPendingLabelCount() {
        return this.pendingLabelCount;
    }

    public int getErrorLabelCount() {
        return this.errorLabelCount;
    }

    public BigDecimal getTotalQuantity() {
        return this.totalQuantity;
    }

    public BigDecimal getScannedQuantity() {
        return this.scannedQuantity;
    }

    public BigDecimal getPendingQuantity() {
        return this.pendingQuantity;
    }

    public List<LabelProgressItem> getLabels() {
        return this.labels;
    }

    public boolean isCanComplete() {
        return this.canComplete;
    }

    public boolean isHasErrors() {
        return this.hasErrors;
    }

    public String getLastScanTime() {
        return this.lastScanTime;
    }

    public String getLastScanOperator() {
        return this.lastScanOperator;
    }

    public void setStockinId(final Long stockinId) {
        this.stockinId = stockinId;
    }

    public void setStockinNo(final String stockinNo) {
        this.stockinNo = stockinNo;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public void setTotalLabelCount(final int totalLabelCount) {
        this.totalLabelCount = totalLabelCount;
    }

    public void setScannedLabelCount(final int scannedLabelCount) {
        this.scannedLabelCount = scannedLabelCount;
    }

    public void setPendingLabelCount(final int pendingLabelCount) {
        this.pendingLabelCount = pendingLabelCount;
    }

    public void setErrorLabelCount(final int errorLabelCount) {
        this.errorLabelCount = errorLabelCount;
    }

    public void setTotalQuantity(final BigDecimal totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public void setScannedQuantity(final BigDecimal scannedQuantity) {
        this.scannedQuantity = scannedQuantity;
    }

    public void setPendingQuantity(final BigDecimal pendingQuantity) {
        this.pendingQuantity = pendingQuantity;
    }

    public void setLabels(final List<LabelProgressItem> labels) {
        this.labels = labels;
    }

    public void setCanComplete(final boolean canComplete) {
        this.canComplete = canComplete;
    }

    public void setHasErrors(final boolean hasErrors) {
        this.hasErrors = hasErrors;
    }

    public void setLastScanTime(final String lastScanTime) {
        this.lastScanTime = lastScanTime;
    }

    public void setLastScanOperator(final String lastScanOperator) {
        this.lastScanOperator = lastScanOperator;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof StockinProgressDTO)) return false;
        final StockinProgressDTO other = (StockinProgressDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.getTotalLabelCount() != other.getTotalLabelCount()) return false;
        if (this.getScannedLabelCount() != other.getScannedLabelCount()) return false;
        if (this.getPendingLabelCount() != other.getPendingLabelCount()) return false;
        if (this.getErrorLabelCount() != other.getErrorLabelCount()) return false;
        if (this.isCanComplete() != other.isCanComplete()) return false;
        if (this.isHasErrors() != other.isHasErrors()) return false;
        final java.lang.Object this$stockinId = this.getStockinId();
        final java.lang.Object other$stockinId = other.getStockinId();
        if (this$stockinId == null ? other$stockinId != null : !this$stockinId.equals(other$stockinId)) return false;
        final java.lang.Object this$stockinNo = this.getStockinNo();
        final java.lang.Object other$stockinNo = other.getStockinNo();
        if (this$stockinNo == null ? other$stockinNo != null : !this$stockinNo.equals(other$stockinNo)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$totalQuantity = this.getTotalQuantity();
        final java.lang.Object other$totalQuantity = other.getTotalQuantity();
        if (this$totalQuantity == null ? other$totalQuantity != null : !this$totalQuantity.equals(other$totalQuantity)) return false;
        final java.lang.Object this$scannedQuantity = this.getScannedQuantity();
        final java.lang.Object other$scannedQuantity = other.getScannedQuantity();
        if (this$scannedQuantity == null ? other$scannedQuantity != null : !this$scannedQuantity.equals(other$scannedQuantity)) return false;
        final java.lang.Object this$pendingQuantity = this.getPendingQuantity();
        final java.lang.Object other$pendingQuantity = other.getPendingQuantity();
        if (this$pendingQuantity == null ? other$pendingQuantity != null : !this$pendingQuantity.equals(other$pendingQuantity)) return false;
        final java.lang.Object this$labels = this.getLabels();
        final java.lang.Object other$labels = other.getLabels();
        if (this$labels == null ? other$labels != null : !this$labels.equals(other$labels)) return false;
        final java.lang.Object this$lastScanTime = this.getLastScanTime();
        final java.lang.Object other$lastScanTime = other.getLastScanTime();
        if (this$lastScanTime == null ? other$lastScanTime != null : !this$lastScanTime.equals(other$lastScanTime)) return false;
        final java.lang.Object this$lastScanOperator = this.getLastScanOperator();
        final java.lang.Object other$lastScanOperator = other.getLastScanOperator();
        if (this$lastScanOperator == null ? other$lastScanOperator != null : !this$lastScanOperator.equals(other$lastScanOperator)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof StockinProgressDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.getTotalLabelCount();
        result = result * PRIME + this.getScannedLabelCount();
        result = result * PRIME + this.getPendingLabelCount();
        result = result * PRIME + this.getErrorLabelCount();
        result = result * PRIME + (this.isCanComplete() ? 79 : 97);
        result = result * PRIME + (this.isHasErrors() ? 79 : 97);
        final java.lang.Object $stockinId = this.getStockinId();
        result = result * PRIME + ($stockinId == null ? 43 : $stockinId.hashCode());
        final java.lang.Object $stockinNo = this.getStockinNo();
        result = result * PRIME + ($stockinNo == null ? 43 : $stockinNo.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $totalQuantity = this.getTotalQuantity();
        result = result * PRIME + ($totalQuantity == null ? 43 : $totalQuantity.hashCode());
        final java.lang.Object $scannedQuantity = this.getScannedQuantity();
        result = result * PRIME + ($scannedQuantity == null ? 43 : $scannedQuantity.hashCode());
        final java.lang.Object $pendingQuantity = this.getPendingQuantity();
        result = result * PRIME + ($pendingQuantity == null ? 43 : $pendingQuantity.hashCode());
        final java.lang.Object $labels = this.getLabels();
        result = result * PRIME + ($labels == null ? 43 : $labels.hashCode());
        final java.lang.Object $lastScanTime = this.getLastScanTime();
        result = result * PRIME + ($lastScanTime == null ? 43 : $lastScanTime.hashCode());
        final java.lang.Object $lastScanOperator = this.getLastScanOperator();
        result = result * PRIME + ($lastScanOperator == null ? 43 : $lastScanOperator.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "StockinProgressDTO(stockinId=" + this.getStockinId() + ", stockinNo=" + this.getStockinNo() + ", status=" + this.getStatus() + ", totalLabelCount=" + this.getTotalLabelCount() + ", scannedLabelCount=" + this.getScannedLabelCount() + ", pendingLabelCount=" + this.getPendingLabelCount() + ", errorLabelCount=" + this.getErrorLabelCount() + ", totalQuantity=" + this.getTotalQuantity() + ", scannedQuantity=" + this.getScannedQuantity() + ", pendingQuantity=" + this.getPendingQuantity() + ", labels=" + this.getLabels() + ", canComplete=" + this.isCanComplete() + ", hasErrors=" + this.isHasErrors() + ", lastScanTime=" + this.getLastScanTime() + ", lastScanOperator=" + this.getLastScanOperator() + ")";
    }
}
