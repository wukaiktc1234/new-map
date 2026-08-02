package com.foodtraceability.dto;

import java.math.BigDecimal;

/**
 * 应收账款统计DTO
 * 用于封装应收账款的统计结果
 */
public class ReceivableStatisticsDTO {
    /**
     * 总应收账款金额
     */
    private BigDecimal totalAmount;
    /**
     * 已收回金额
     */
    private BigDecimal totalReceivedAmount;
    /**
     * 剩余未收回金额
     */
    private BigDecimal totalRemainingAmount;
    /**
     * 逾期应收账款金额
     */
    private BigDecimal overdueAmount;
    /**
     * 逾期应收账款笔数
     */
    private Integer overdueCount;
    /**
     * 平均逾期天数
     */
    private Integer avgOverdueDays;
    /**
     * 应收账款周转率
     */
    private BigDecimal turnoverRate;
    /**
     * 本期新增应收账款金额
     */
    private BigDecimal currentPeriodNewAmount;
    /**
     * 本期收回应收账款金额
     */
    private BigDecimal currentPeriodReceivedAmount;

    public ReceivableStatisticsDTO() {
    }

    /**
     * 总应收账款金额
     */
    public BigDecimal getTotalAmount() {
        return this.totalAmount;
    }

    /**
     * 已收回金额
     */
    public BigDecimal getTotalReceivedAmount() {
        return this.totalReceivedAmount;
    }

    /**
     * 剩余未收回金额
     */
    public BigDecimal getTotalRemainingAmount() {
        return this.totalRemainingAmount;
    }

    /**
     * 逾期应收账款金额
     */
    public BigDecimal getOverdueAmount() {
        return this.overdueAmount;
    }

    /**
     * 逾期应收账款笔数
     */
    public Integer getOverdueCount() {
        return this.overdueCount;
    }

    /**
     * 平均逾期天数
     */
    public Integer getAvgOverdueDays() {
        return this.avgOverdueDays;
    }

    /**
     * 应收账款周转率
     */
    public BigDecimal getTurnoverRate() {
        return this.turnoverRate;
    }

    /**
     * 本期新增应收账款金额
     */
    public BigDecimal getCurrentPeriodNewAmount() {
        return this.currentPeriodNewAmount;
    }

    /**
     * 本期收回应收账款金额
     */
    public BigDecimal getCurrentPeriodReceivedAmount() {
        return this.currentPeriodReceivedAmount;
    }

    /**
     * 总应收账款金额
     */
    public void setTotalAmount(final BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    /**
     * 已收回金额
     */
    public void setTotalReceivedAmount(final BigDecimal totalReceivedAmount) {
        this.totalReceivedAmount = totalReceivedAmount;
    }

    /**
     * 剩余未收回金额
     */
    public void setTotalRemainingAmount(final BigDecimal totalRemainingAmount) {
        this.totalRemainingAmount = totalRemainingAmount;
    }

    /**
     * 逾期应收账款金额
     */
    public void setOverdueAmount(final BigDecimal overdueAmount) {
        this.overdueAmount = overdueAmount;
    }

    /**
     * 逾期应收账款笔数
     */
    public void setOverdueCount(final Integer overdueCount) {
        this.overdueCount = overdueCount;
    }

    /**
     * 平均逾期天数
     */
    public void setAvgOverdueDays(final Integer avgOverdueDays) {
        this.avgOverdueDays = avgOverdueDays;
    }

    /**
     * 应收账款周转率
     */
    public void setTurnoverRate(final BigDecimal turnoverRate) {
        this.turnoverRate = turnoverRate;
    }

    /**
     * 本期新增应收账款金额
     */
    public void setCurrentPeriodNewAmount(final BigDecimal currentPeriodNewAmount) {
        this.currentPeriodNewAmount = currentPeriodNewAmount;
    }

    /**
     * 本期收回应收账款金额
     */
    public void setCurrentPeriodReceivedAmount(final BigDecimal currentPeriodReceivedAmount) {
        this.currentPeriodReceivedAmount = currentPeriodReceivedAmount;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof ReceivableStatisticsDTO)) return false;
        final ReceivableStatisticsDTO other = (ReceivableStatisticsDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$overdueCount = this.getOverdueCount();
        final java.lang.Object other$overdueCount = other.getOverdueCount();
        if (this$overdueCount == null ? other$overdueCount != null : !this$overdueCount.equals(other$overdueCount)) return false;
        final java.lang.Object this$avgOverdueDays = this.getAvgOverdueDays();
        final java.lang.Object other$avgOverdueDays = other.getAvgOverdueDays();
        if (this$avgOverdueDays == null ? other$avgOverdueDays != null : !this$avgOverdueDays.equals(other$avgOverdueDays)) return false;
        final java.lang.Object this$totalAmount = this.getTotalAmount();
        final java.lang.Object other$totalAmount = other.getTotalAmount();
        if (this$totalAmount == null ? other$totalAmount != null : !this$totalAmount.equals(other$totalAmount)) return false;
        final java.lang.Object this$totalReceivedAmount = this.getTotalReceivedAmount();
        final java.lang.Object other$totalReceivedAmount = other.getTotalReceivedAmount();
        if (this$totalReceivedAmount == null ? other$totalReceivedAmount != null : !this$totalReceivedAmount.equals(other$totalReceivedAmount)) return false;
        final java.lang.Object this$totalRemainingAmount = this.getTotalRemainingAmount();
        final java.lang.Object other$totalRemainingAmount = other.getTotalRemainingAmount();
        if (this$totalRemainingAmount == null ? other$totalRemainingAmount != null : !this$totalRemainingAmount.equals(other$totalRemainingAmount)) return false;
        final java.lang.Object this$overdueAmount = this.getOverdueAmount();
        final java.lang.Object other$overdueAmount = other.getOverdueAmount();
        if (this$overdueAmount == null ? other$overdueAmount != null : !this$overdueAmount.equals(other$overdueAmount)) return false;
        final java.lang.Object this$turnoverRate = this.getTurnoverRate();
        final java.lang.Object other$turnoverRate = other.getTurnoverRate();
        if (this$turnoverRate == null ? other$turnoverRate != null : !this$turnoverRate.equals(other$turnoverRate)) return false;
        final java.lang.Object this$currentPeriodNewAmount = this.getCurrentPeriodNewAmount();
        final java.lang.Object other$currentPeriodNewAmount = other.getCurrentPeriodNewAmount();
        if (this$currentPeriodNewAmount == null ? other$currentPeriodNewAmount != null : !this$currentPeriodNewAmount.equals(other$currentPeriodNewAmount)) return false;
        final java.lang.Object this$currentPeriodReceivedAmount = this.getCurrentPeriodReceivedAmount();
        final java.lang.Object other$currentPeriodReceivedAmount = other.getCurrentPeriodReceivedAmount();
        if (this$currentPeriodReceivedAmount == null ? other$currentPeriodReceivedAmount != null : !this$currentPeriodReceivedAmount.equals(other$currentPeriodReceivedAmount)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof ReceivableStatisticsDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $overdueCount = this.getOverdueCount();
        result = result * PRIME + ($overdueCount == null ? 43 : $overdueCount.hashCode());
        final java.lang.Object $avgOverdueDays = this.getAvgOverdueDays();
        result = result * PRIME + ($avgOverdueDays == null ? 43 : $avgOverdueDays.hashCode());
        final java.lang.Object $totalAmount = this.getTotalAmount();
        result = result * PRIME + ($totalAmount == null ? 43 : $totalAmount.hashCode());
        final java.lang.Object $totalReceivedAmount = this.getTotalReceivedAmount();
        result = result * PRIME + ($totalReceivedAmount == null ? 43 : $totalReceivedAmount.hashCode());
        final java.lang.Object $totalRemainingAmount = this.getTotalRemainingAmount();
        result = result * PRIME + ($totalRemainingAmount == null ? 43 : $totalRemainingAmount.hashCode());
        final java.lang.Object $overdueAmount = this.getOverdueAmount();
        result = result * PRIME + ($overdueAmount == null ? 43 : $overdueAmount.hashCode());
        final java.lang.Object $turnoverRate = this.getTurnoverRate();
        result = result * PRIME + ($turnoverRate == null ? 43 : $turnoverRate.hashCode());
        final java.lang.Object $currentPeriodNewAmount = this.getCurrentPeriodNewAmount();
        result = result * PRIME + ($currentPeriodNewAmount == null ? 43 : $currentPeriodNewAmount.hashCode());
        final java.lang.Object $currentPeriodReceivedAmount = this.getCurrentPeriodReceivedAmount();
        result = result * PRIME + ($currentPeriodReceivedAmount == null ? 43 : $currentPeriodReceivedAmount.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "ReceivableStatisticsDTO(totalAmount=" + this.getTotalAmount() + ", totalReceivedAmount=" + this.getTotalReceivedAmount() + ", totalRemainingAmount=" + this.getTotalRemainingAmount() + ", overdueAmount=" + this.getOverdueAmount() + ", overdueCount=" + this.getOverdueCount() + ", avgOverdueDays=" + this.getAvgOverdueDays() + ", turnoverRate=" + this.getTurnoverRate() + ", currentPeriodNewAmount=" + this.getCurrentPeriodNewAmount() + ", currentPeriodReceivedAmount=" + this.getCurrentPeriodReceivedAmount() + ")";
    }
}
