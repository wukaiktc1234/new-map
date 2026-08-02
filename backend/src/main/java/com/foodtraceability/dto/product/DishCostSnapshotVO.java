package com.foodtraceability.dto.product;

import java.io.Serializable;
import java.util.List;

/**
 * 菜品成本快照 VO
 * 描述菜品在某一时刻的成本构成和毛利信息
 */
public class DishCostSnapshotVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 菜品ID */
    private String dishId;
    /** 菜品名称 */
    private String dishName;
    /** 分类ID */
    private String categoryId;
    /** 分类名称 */
    private String categoryName;
    /** 售价（分） */
    private Long salePrice;
    /** 总成本（分） */
    private Long totalCost;
    /** 毛利（分） */
    private Long profit;
    /** 毛利率(%) */
    private Double marginRate;
    /** 成本变化百分比(%) */
    private Double costChangePercent;
    /** 状态：normal/warning/danger */
    private String status;
    /** 最后更新时间（ISO 8601） */
    private String lastUpdated;
    /** BOM 明细成本列表 */
    private List<BomItemCost> bomItems;

    /**
     * BOM 项成本明细
     */
    public static class BomItemCost implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 物料ID */
        private String materialId;
        /** 物料名称 */
        private String materialName;
        /** 用量 */
        private Double quantity;
        /** 单位 */
        private String unit;
        /** 单价（分） */
        private Long unitPrice;
        /** 小计成本（分） */
        private Long subtotal;
        /** 成本占比(%) */
        private Double costPercent;

        public String getMaterialId() { return materialId; }
        public void setMaterialId(String materialId) { this.materialId = materialId; }
        public String getMaterialName() { return materialName; }
        public void setMaterialName(String materialName) { this.materialName = materialName; }
        public Double getQuantity() { return quantity; }
        public void setQuantity(Double quantity) { this.quantity = quantity; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        public Long getUnitPrice() { return unitPrice; }
        public void setUnitPrice(Long unitPrice) { this.unitPrice = unitPrice; }
        public Long getSubtotal() { return subtotal; }
        public void setSubtotal(Long subtotal) { this.subtotal = subtotal; }
        public Double getCostPercent() { return costPercent; }
        public void setCostPercent(Double costPercent) { this.costPercent = costPercent; }
    }

    public String getDishId() { return dishId; }
    public void setDishId(String dishId) { this.dishId = dishId; }
    public String getDishName() { return dishName; }
    public void setDishName(String dishName) { this.dishName = dishName; }
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public Long getSalePrice() { return salePrice; }
    public void setSalePrice(Long salePrice) { this.salePrice = salePrice; }
    public Long getTotalCost() { return totalCost; }
    public void setTotalCost(Long totalCost) { this.totalCost = totalCost; }
    public Long getProfit() { return profit; }
    public void setProfit(Long profit) { this.profit = profit; }
    public Double getMarginRate() { return marginRate; }
    public void setMarginRate(Double marginRate) { this.marginRate = marginRate; }
    public Double getCostChangePercent() { return costChangePercent; }
    public void setCostChangePercent(Double costChangePercent) { this.costChangePercent = costChangePercent; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }
    public List<BomItemCost> getBomItems() { return bomItems; }
    public void setBomItems(List<BomItemCost> bomItems) { this.bomItems = bomItems; }
}
