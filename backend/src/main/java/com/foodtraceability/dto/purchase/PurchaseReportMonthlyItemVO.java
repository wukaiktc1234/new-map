package com.foodtraceability.dto.purchase;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 采购报表月度趋势项视图对象
 *
 * <p>对应前端类型 PurchaseReportMonthlyItem：
 * <ul>
 *   <li>month → month（YYYY-MM 字符串）</li>
 *   <li>purchaseAmount → purchaseAmount（分→元）</li>
 *   <li>orderCount → orderCount</li>
 *   <li>settledAmount → settledAmount（分→元）</li>
 *   <li>supplierCount → supplierCount</li>
 * </ul>
 * </p>
 */
@Schema(description = "采购报表月度趋势项")
public class PurchaseReportMonthlyItemVO {

    /** 月份（YYYY-MM 格式） */
    @Schema(description = "月份（YYYY-MM）", example = "2026-06")
    private String month;

    /** 采购金额（分） */
    @Schema(description = "采购金额（分）", example = "5230000")
    private Long purchaseAmount;

    /** 订单数 */
    @Schema(description = "订单数", example = "24")
    private Integer orderCount;

    /** 已结算金额（分） */
    @Schema(description = "已结算金额（分）", example = "2720000")
    private Long settledAmount;

    /** 供应商数 */
    @Schema(description = "供应商数", example = "7")
    private Integer supplierCount;

    // ==================== Getter & Setter ====================

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public Long getPurchaseAmount() {
        return purchaseAmount;
    }

    public void setPurchaseAmount(Long purchaseAmount) {
        this.purchaseAmount = purchaseAmount;
    }

    public Integer getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Integer orderCount) {
        this.orderCount = orderCount;
    }

    public Long getSettledAmount() {
        return settledAmount;
    }

    public void setSettledAmount(Long settledAmount) {
        this.settledAmount = settledAmount;
    }

    public Integer getSupplierCount() {
        return supplierCount;
    }

    public void setSupplierCount(Integer supplierCount) {
        this.supplierCount = supplierCount;
    }
}
