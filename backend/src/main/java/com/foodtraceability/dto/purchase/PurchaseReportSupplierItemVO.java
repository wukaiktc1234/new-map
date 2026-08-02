package com.foodtraceability.dto.purchase;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * 采购报表供应商维度项视图对象
 *
 * <p>对应前端类型 PurchaseReportSupplierItem：
 * <ul>
 *   <li>supplierId → supplierId（Long→string）</li>
 *   <li>supplierName → supplierName</li>
 *   <li>totalAmount → totalAmount（分→元）</li>
 *   <li>orderCount → orderCount</li>
 *   <li>onTimeRate → onTimeRate（百分比，0~100）</li>
 *   <li>qualifiedRate → qualifiedRate（百分比，0~100）</li>
 * </ul>
 * </p>
 */
@Schema(description = "采购报表供应商维度项")
public class PurchaseReportSupplierItemVO {

    /** 供应商ID */
    @Schema(description = "供应商ID", example = "1")
    private Long supplierId;

    /** 供应商名称 */
    @Schema(description = "供应商名称", example = "绿源蔬菜批发")
    private String supplierName;

    /** 采购总额（分） */
    @Schema(description = "采购总额（分）", example = "3200000")
    private Long totalAmount;

    /** 订单数 */
    @Schema(description = "订单数", example = "22")
    private Integer orderCount;

    /** 准时率（百分比，0~100） */
    @Schema(description = "准时率（百分比）", example = "98.00")
    private BigDecimal onTimeRate;

    /** 合格率（百分比，0~100） */
    @Schema(description = "合格率（百分比）", example = "99.00")
    private BigDecimal qualifiedRate;

    // ==================== Getter & Setter ====================

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Integer getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Integer orderCount) {
        this.orderCount = orderCount;
    }

    public BigDecimal getOnTimeRate() {
        return onTimeRate;
    }

    public void setOnTimeRate(BigDecimal onTimeRate) {
        this.onTimeRate = onTimeRate;
    }

    public BigDecimal getQualifiedRate() {
        return qualifiedRate;
    }

    public void setQualifiedRate(BigDecimal qualifiedRate) {
        this.qualifiedRate = qualifiedRate;
    }
}
