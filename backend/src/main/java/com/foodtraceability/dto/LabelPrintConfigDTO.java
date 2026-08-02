package com.foodtraceability.dto;

/**
 * 标签打印配置DTO
 * 支持剥离模式（即打即贴）配置
 */
public class LabelPrintConfigDTO {
    private Long printerId;
    private String printerType;
    private String printMode;
    private boolean peelModeEnabled;
    private boolean cutAfterPrint;
    private int copies;
    private int printSpeed;
    private int printDensity;
    private int labelWidth;
    private int labelHeight;
    private int gapSize;
    private boolean autoTear;
    private int tearOffPosition;
    private boolean waitForPeel;
    private int peelDelay;
    private boolean reprintOnJam;
    private boolean beepOnComplete;

    public static LabelPrintConfigDTO defaultConfig() {
        LabelPrintConfigDTO config = new LabelPrintConfigDTO();
        config.setPrintMode("STANDARD");
        config.setPeelModeEnabled(false);
        config.setCutAfterPrint(false);
        config.setCopies(1);
        config.setPrintSpeed(3);
        config.setPrintDensity(10);
        config.setLabelWidth(40);
        config.setLabelHeight(24);
        config.setGapSize(2);
        config.setAutoTear(true);
        config.setTearOffPosition(0);
        config.setWaitForPeel(true);
        config.setPeelDelay(500);
        config.setReprintOnJam(true);
        config.setBeepOnComplete(true);
        return config;
    }

    public static LabelPrintConfigDTO peelModeConfig() {
        LabelPrintConfigDTO config = defaultConfig();
        config.setPrintMode("PEEL");
        config.setPeelModeEnabled(true);
        config.setAutoTear(false);
        config.setWaitForPeel(true);
        config.setPeelDelay(500);
        return config;
    }

    public LabelPrintConfigDTO() {
    }

    public Long getPrinterId() {
        return this.printerId;
    }

    public String getPrinterType() {
        return this.printerType;
    }

    public String getPrintMode() {
        return this.printMode;
    }

    public boolean isPeelModeEnabled() {
        return this.peelModeEnabled;
    }

    public boolean isCutAfterPrint() {
        return this.cutAfterPrint;
    }

    public int getCopies() {
        return this.copies;
    }

    public int getPrintSpeed() {
        return this.printSpeed;
    }

    public int getPrintDensity() {
        return this.printDensity;
    }

    public int getLabelWidth() {
        return this.labelWidth;
    }

    public int getLabelHeight() {
        return this.labelHeight;
    }

    public int getGapSize() {
        return this.gapSize;
    }

    public boolean isAutoTear() {
        return this.autoTear;
    }

    public int getTearOffPosition() {
        return this.tearOffPosition;
    }

    public boolean isWaitForPeel() {
        return this.waitForPeel;
    }

    public int getPeelDelay() {
        return this.peelDelay;
    }

    public boolean isReprintOnJam() {
        return this.reprintOnJam;
    }

    public boolean isBeepOnComplete() {
        return this.beepOnComplete;
    }

    public void setPrinterId(final Long printerId) {
        this.printerId = printerId;
    }

    public void setPrinterType(final String printerType) {
        this.printerType = printerType;
    }

    public void setPrintMode(final String printMode) {
        this.printMode = printMode;
    }

    public void setPeelModeEnabled(final boolean peelModeEnabled) {
        this.peelModeEnabled = peelModeEnabled;
    }

    public void setCutAfterPrint(final boolean cutAfterPrint) {
        this.cutAfterPrint = cutAfterPrint;
    }

    public void setCopies(final int copies) {
        this.copies = copies;
    }

    public void setPrintSpeed(final int printSpeed) {
        this.printSpeed = printSpeed;
    }

    public void setPrintDensity(final int printDensity) {
        this.printDensity = printDensity;
    }

    public void setLabelWidth(final int labelWidth) {
        this.labelWidth = labelWidth;
    }

    public void setLabelHeight(final int labelHeight) {
        this.labelHeight = labelHeight;
    }

    public void setGapSize(final int gapSize) {
        this.gapSize = gapSize;
    }

    public void setAutoTear(final boolean autoTear) {
        this.autoTear = autoTear;
    }

    public void setTearOffPosition(final int tearOffPosition) {
        this.tearOffPosition = tearOffPosition;
    }

    public void setWaitForPeel(final boolean waitForPeel) {
        this.waitForPeel = waitForPeel;
    }

    public void setPeelDelay(final int peelDelay) {
        this.peelDelay = peelDelay;
    }

    public void setReprintOnJam(final boolean reprintOnJam) {
        this.reprintOnJam = reprintOnJam;
    }

    public void setBeepOnComplete(final boolean beepOnComplete) {
        this.beepOnComplete = beepOnComplete;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof LabelPrintConfigDTO)) return false;
        final LabelPrintConfigDTO other = (LabelPrintConfigDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        if (this.isPeelModeEnabled() != other.isPeelModeEnabled()) return false;
        if (this.isCutAfterPrint() != other.isCutAfterPrint()) return false;
        if (this.getCopies() != other.getCopies()) return false;
        if (this.getPrintSpeed() != other.getPrintSpeed()) return false;
        if (this.getPrintDensity() != other.getPrintDensity()) return false;
        if (this.getLabelWidth() != other.getLabelWidth()) return false;
        if (this.getLabelHeight() != other.getLabelHeight()) return false;
        if (this.getGapSize() != other.getGapSize()) return false;
        if (this.isAutoTear() != other.isAutoTear()) return false;
        if (this.getTearOffPosition() != other.getTearOffPosition()) return false;
        if (this.isWaitForPeel() != other.isWaitForPeel()) return false;
        if (this.getPeelDelay() != other.getPeelDelay()) return false;
        if (this.isReprintOnJam() != other.isReprintOnJam()) return false;
        if (this.isBeepOnComplete() != other.isBeepOnComplete()) return false;
        final java.lang.Object this$printerId = this.getPrinterId();
        final java.lang.Object other$printerId = other.getPrinterId();
        if (this$printerId == null ? other$printerId != null : !this$printerId.equals(other$printerId)) return false;
        final java.lang.Object this$printerType = this.getPrinterType();
        final java.lang.Object other$printerType = other.getPrinterType();
        if (this$printerType == null ? other$printerType != null : !this$printerType.equals(other$printerType)) return false;
        final java.lang.Object this$printMode = this.getPrintMode();
        final java.lang.Object other$printMode = other.getPrintMode();
        if (this$printMode == null ? other$printMode != null : !this$printMode.equals(other$printMode)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof LabelPrintConfigDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isPeelModeEnabled() ? 79 : 97);
        result = result * PRIME + (this.isCutAfterPrint() ? 79 : 97);
        result = result * PRIME + this.getCopies();
        result = result * PRIME + this.getPrintSpeed();
        result = result * PRIME + this.getPrintDensity();
        result = result * PRIME + this.getLabelWidth();
        result = result * PRIME + this.getLabelHeight();
        result = result * PRIME + this.getGapSize();
        result = result * PRIME + (this.isAutoTear() ? 79 : 97);
        result = result * PRIME + this.getTearOffPosition();
        result = result * PRIME + (this.isWaitForPeel() ? 79 : 97);
        result = result * PRIME + this.getPeelDelay();
        result = result * PRIME + (this.isReprintOnJam() ? 79 : 97);
        result = result * PRIME + (this.isBeepOnComplete() ? 79 : 97);
        final java.lang.Object $printerId = this.getPrinterId();
        result = result * PRIME + ($printerId == null ? 43 : $printerId.hashCode());
        final java.lang.Object $printerType = this.getPrinterType();
        result = result * PRIME + ($printerType == null ? 43 : $printerType.hashCode());
        final java.lang.Object $printMode = this.getPrintMode();
        result = result * PRIME + ($printMode == null ? 43 : $printMode.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "LabelPrintConfigDTO(printerId=" + this.getPrinterId() + ", printerType=" + this.getPrinterType() + ", printMode=" + this.getPrintMode() + ", peelModeEnabled=" + this.isPeelModeEnabled() + ", cutAfterPrint=" + this.isCutAfterPrint() + ", copies=" + this.getCopies() + ", printSpeed=" + this.getPrintSpeed() + ", printDensity=" + this.getPrintDensity() + ", labelWidth=" + this.getLabelWidth() + ", labelHeight=" + this.getLabelHeight() + ", gapSize=" + this.getGapSize() + ", autoTear=" + this.isAutoTear() + ", tearOffPosition=" + this.getTearOffPosition() + ", waitForPeel=" + this.isWaitForPeel() + ", peelDelay=" + this.getPeelDelay() + ", reprintOnJam=" + this.isReprintOnJam() + ", beepOnComplete=" + this.isBeepOnComplete() + ")";
    }
}
