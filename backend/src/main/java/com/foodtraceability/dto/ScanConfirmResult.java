package com.foodtraceability.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 扫码确认入库结果DTO
 */
public class ScanConfirmResult {
    private boolean success;
    private String message;
    private String traceCode;
    private String materialName;
    private BigDecimal expectedQuantity;
    private BigDecimal scannedQuantity;
    private int totalLabelCount;
    private int scannedLabelCount;
    private boolean needManualIntervention;
    private String alertType;
    private List<String> errors = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    public static ScanConfirmResult success(String message) {
        ScanConfirmResult result = new ScanConfirmResult();
        result.setSuccess(true);
        result.setMessage(message);
        return result;
    }

    public static ScanConfirmResult failure(String message) {
        ScanConfirmResult result = new ScanConfirmResult();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }

    public static ScanConfirmResult needIntervention(String message, String alertType) {
        ScanConfirmResult result = new ScanConfirmResult();
        result.setSuccess(false);
        result.setMessage(message);
        result.setNeedManualIntervention(true);
        result.setAlertType(alertType);
        return result;
    }

    public void addError(String error) {
        this.errors.add(error);
    }

    public void addWarning(String warning) {
        this.warnings.add(warning);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public ScanConfirmResult() {
    }

    public boolean isSuccess() {
        return this.success;
    }

    public String getMessage() {
        return this.message;
    }

    public String getTraceCode() {
        return this.traceCode;
    }

    public String getMaterialName() {
        return this.materialName;
    }

    public BigDecimal getExpectedQuantity() {
        return this.expectedQuantity;
    }

    public BigDecimal getScannedQuantity() {
        return this.scannedQuantity;
    }

    public int getTotalLabelCount() {
        return this.totalLabelCount;
    }

    public int getScannedLabelCount() {
        return this.scannedLabelCount;
    }

    public boolean isNeedManualIntervention() {
        return this.needManualIntervention;
    }

    public String getAlertType() {
        return this.alertType;
    }

    public List<String> getErrors() {
        return this.errors;
    }

    public List<String> getWarnings() {
        return this.warnings;
    }

    public void setSuccess(final boolean success) {
        this.success = success;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public void setTraceCode(final String traceCode) {
        this.traceCode = traceCode;
    }

    public void setMaterialName(final String materialName) {
        this.materialName = materialName;
    }

    public void setExpectedQuantity(final BigDecimal expectedQuantity) {
        this.expectedQuantity = expectedQuantity;
    }

    public void setScannedQuantity(final BigDecimal scannedQuantity) {
        this.scannedQuantity = scannedQuantity;
    }

    public void setTotalLabelCount(final int totalLabelCount) {
        this.totalLabelCount = totalLabelCount;
    }

    public void setScannedLabelCount(final int scannedLabelCount) {
        this.scannedLabelCount = scannedLabelCount;
    }

    public void setNeedManualIntervention(final boolean needManualIntervention) {
        this.needManualIntervention = needManualIntervention;
    }

    public void setAlertType(final String alertType) {
        this.alertType = alertType;
    }

    public void setErrors(final List<String> errors) {
        this.errors = errors;
    }

    public void setWarnings(final List<String> warnings) {
        this.warnings = warnings;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof ScanConfirmResult)) return false;
        final ScanConfirmResult other = (ScanConfirmResult) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.isSuccess() != other.isSuccess()) return false;
        if (this.getTotalLabelCount() != other.getTotalLabelCount()) return false;
        if (this.getScannedLabelCount() != other.getScannedLabelCount()) return false;
        if (this.isNeedManualIntervention() != other.isNeedManualIntervention()) return false;
        final java.lang.Object this$message = this.getMessage();
        final java.lang.Object other$message = other.getMessage();
        if (this$message == null ? other$message != null : !this$message.equals(other$message)) return false;
        final java.lang.Object this$traceCode = this.getTraceCode();
        final java.lang.Object other$traceCode = other.getTraceCode();
        if (this$traceCode == null ? other$traceCode != null : !this$traceCode.equals(other$traceCode)) return false;
        final java.lang.Object this$materialName = this.getMaterialName();
        final java.lang.Object other$materialName = other.getMaterialName();
        if (this$materialName == null ? other$materialName != null : !this$materialName.equals(other$materialName)) return false;
        final java.lang.Object this$expectedQuantity = this.getExpectedQuantity();
        final java.lang.Object other$expectedQuantity = other.getExpectedQuantity();
        if (this$expectedQuantity == null ? other$expectedQuantity != null : !this$expectedQuantity.equals(other$expectedQuantity)) return false;
        final java.lang.Object this$scannedQuantity = this.getScannedQuantity();
        final java.lang.Object other$scannedQuantity = other.getScannedQuantity();
        if (this$scannedQuantity == null ? other$scannedQuantity != null : !this$scannedQuantity.equals(other$scannedQuantity)) return false;
        final java.lang.Object this$alertType = this.getAlertType();
        final java.lang.Object other$alertType = other.getAlertType();
        if (this$alertType == null ? other$alertType != null : !this$alertType.equals(other$alertType)) return false;
        final java.lang.Object this$errors = this.getErrors();
        final java.lang.Object other$errors = other.getErrors();
        if (this$errors == null ? other$errors != null : !this$errors.equals(other$errors)) return false;
        final java.lang.Object this$warnings = this.getWarnings();
        final java.lang.Object other$warnings = other.getWarnings();
        if (this$warnings == null ? other$warnings != null : !this$warnings.equals(other$warnings)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ScanConfirmResult;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isSuccess() ? 79 : 97);
        result = result * PRIME + this.getTotalLabelCount();
        result = result * PRIME + this.getScannedLabelCount();
        result = result * PRIME + (this.isNeedManualIntervention() ? 79 : 97);
        final java.lang.Object $message = this.getMessage();
        result = result * PRIME + ($message == null ? 43 : $message.hashCode());
        final java.lang.Object $traceCode = this.getTraceCode();
        result = result * PRIME + ($traceCode == null ? 43 : $traceCode.hashCode());
        final java.lang.Object $materialName = this.getMaterialName();
        result = result * PRIME + ($materialName == null ? 43 : $materialName.hashCode());
        final java.lang.Object $expectedQuantity = this.getExpectedQuantity();
        result = result * PRIME + ($expectedQuantity == null ? 43 : $expectedQuantity.hashCode());
        final java.lang.Object $scannedQuantity = this.getScannedQuantity();
        result = result * PRIME + ($scannedQuantity == null ? 43 : $scannedQuantity.hashCode());
        final java.lang.Object $alertType = this.getAlertType();
        result = result * PRIME + ($alertType == null ? 43 : $alertType.hashCode());
        final java.lang.Object $errors = this.getErrors();
        result = result * PRIME + ($errors == null ? 43 : $errors.hashCode());
        final java.lang.Object $warnings = this.getWarnings();
        result = result * PRIME + ($warnings == null ? 43 : $warnings.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "ScanConfirmResult(success=" + this.isSuccess() + ", message=" + this.getMessage() + ", traceCode=" + this.getTraceCode() + ", materialName=" + this.getMaterialName() + ", expectedQuantity=" + this.getExpectedQuantity() + ", scannedQuantity=" + this.getScannedQuantity() + ", totalLabelCount=" + this.getTotalLabelCount() + ", scannedLabelCount=" + this.getScannedLabelCount() + ", needManualIntervention=" + this.isNeedManualIntervention() + ", alertType=" + this.getAlertType() + ", errors=" + this.getErrors() + ", warnings=" + this.getWarnings() + ")";
    }
}
