package com.foodtraceability.dto.purchase;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * 采购报表分类维度项视图对象
 *
 * <p>对应前端类型 PurchaseReportCategoryItem：
 * <ul>
 *   <li>categoryName → categoryName</li>
 *   <li>totalAmount → totalAmount（分→元）</li>
 *   <li>totalQuantity → totalQuantity</li>
 *   <li>orderCount → orderCount</li>
 * </ul>
 * </p>
 */
@Schema(description = "采购报表分类维度项")
public class PurchaseReportCategoryItemVO {

    /** 品类名称 */
    @Schema(description = "品类名称", example = "蔬菜类")
    private String categoryName;

    /** 采购总额（分） */
    @Schema(description = "采购总额（分）", example = "3200000")
    private Long totalAmount;

    /** 总数量 */
    @Schema(description = "总数量", example = "4800.000")
    private BigDecimal totalQuantity;

    /** 订单数 */
    @Schema(description = "订单数", example = "22")
    private Integer orderCount;

    // ==================== Getter & Setter ====================

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(BigDecimal totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public Integer getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Integer orderCount) {
        this.orderCount = orderCount;
    }
}
