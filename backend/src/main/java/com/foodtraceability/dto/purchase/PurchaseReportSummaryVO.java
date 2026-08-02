package com.foodtraceability.dto.purchase;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * 采购报表汇总视图对象
 *
 * <p>聚合 purchase_orders 表的统计指标，所有金额字段以"分"为单位（Long），
 * 由前端 DataConverter 转换为"元"（number）。</p>
 *
 * <p>字段对应前端类型 PurchaseReportSummary：
 * <ul>
 *   <li>totalPurchaseAmount → totalPurchaseAmount（分→元）</li>
 *   <li>totalOrderCount → totalOrderCount</li>
 *   <li>totalSettledAmount → totalSettledAmount（分→元）</li>
 *   <li>totalUnsettledAmount → totalUnsettledAmount（分→元）</li>
 *   <li>avgOrderAmount → avgOrderAmount（分→元）</li>
 *   <li>supplierCount → supplierCount</li>
 *   <li>pendingOrders → 前端未使用，保留以备扩展</li>
 *   <li>onTimeDeliveryRate → onTimeDeliveryRate（百分比）</li>
 *   <li>qualifiedRate → qualifiedRate（百分比）</li>
 * </ul>
 * </p>
 */
@Schema(description = "采购报表汇总视图对象")
public class PurchaseReportSummaryVO {

    /** 总采购金额（分） */
    @Schema(description = "总采购金额（分）", example = "15680000")
    private Long totalPurchaseAmount;

    /** 订单总数 */
    @Schema(description = "订单总数", example = "87")
    private Integer totalOrderCount;

    /** 已结算金额（分） */
    @Schema(description = "已结算金额（分）", example = "12850000")
    private Long totalSettledAmount;

    /** 未结算金额（分） */
    @Schema(description = "未结算金额（分）", example = "2830000")
    private Long totalUnsettledAmount;

    /** 平均订单金额（分） */
    @Schema(description = "平均订单金额（分）", example = "180200")
    private Long avgOrderAmount;

    /** 供应商数 */
    @Schema(description = "供应商数", example = "7")
    private Integer supplierCount;

    /** 待处理订单数（订单状态为 0草稿/1待审核 的订单） */
    @Schema(description = "待处理订单数", example = "5")
    private Integer pendingOrders;

    /** 准时交付率（百分比，0~100） */
    @Schema(description = "准时交付率（百分比）", example = "92.50")
    private BigDecimal onTimeDeliveryRate;

    /** 质检合格率（百分比，0~100） */
    @Schema(description = "质检合格率（百分比）", example = "96.80")
    private BigDecimal qualifiedRate;

    // ==================== Getter & Setter ====================

    public Long getTotalPurchaseAmount() {
        return totalPurchaseAmount;
    }

    public void setTotalPurchaseAmount(Long totalPurchaseAmount) {
        this.totalPurchaseAmount = totalPurchaseAmount;
    }

    public Integer getTotalOrderCount() {
        return totalOrderCount;
    }

    public void setTotalOrderCount(Integer totalOrderCount) {
        this.totalOrderCount = totalOrderCount;
    }

    public Long getTotalSettledAmount() {
        return totalSettledAmount;
    }

    public void setTotalSettledAmount(Long totalSettledAmount) {
        this.totalSettledAmount = totalSettledAmount;
    }

    public Long getTotalUnsettledAmount() {
        return totalUnsettledAmount;
    }

    public void setTotalUnsettledAmount(Long totalUnsettledAmount) {
        this.totalUnsettledAmount = totalUnsettledAmount;
    }

    public Long getAvgOrderAmount() {
        return avgOrderAmount;
    }

    public void setAvgOrderAmount(Long avgOrderAmount) {
        this.avgOrderAmount = avgOrderAmount;
    }

    public Integer getSupplierCount() {
        return supplierCount;
    }

    public void setSupplierCount(Integer supplierCount) {
        this.supplierCount = supplierCount;
    }

    public Integer getPendingOrders() {
        return pendingOrders;
    }

    public void setPendingOrders(Integer pendingOrders) {
        this.pendingOrders = pendingOrders;
    }

    public BigDecimal getOnTimeDeliveryRate() {
        return onTimeDeliveryRate;
    }

    public void setOnTimeDeliveryRate(BigDecimal onTimeDeliveryRate) {
        this.onTimeDeliveryRate = onTimeDeliveryRate;
    }

    public BigDecimal getQualifiedRate() {
        return qualifiedRate;
    }

    public void setQualifiedRate(BigDecimal qualifiedRate) {
        this.qualifiedRate = qualifiedRate;
    }
}
