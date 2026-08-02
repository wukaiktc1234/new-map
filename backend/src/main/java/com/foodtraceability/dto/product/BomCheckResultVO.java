package com.foodtraceability.dto.product;

import java.io.Serializable;
import java.util.List;

/**
 * BOM 库存联动检查结果 VO
 * 描述单个菜品 BOM 配方所需物料的库存检查结果
 */
public class BomCheckResultVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 菜品ID */
    private String dishId;
    /** 菜品名称 */
    private String dishName;
    /** 是否可制作 */
    private Boolean canMake;
    /** 总需求数量 */
    private Integer totalRequired;
    /** 缺料项列表 */
    private List<InsufficientItem> insufficientItems;
    /** 替代菜品列表 */
    private List<AlternativeDishVO> alternativeDishes;
    /** 检查时间（ISO 8601） */
    private String checkedAt;

    /**
     * 缺料项明细
     */
    public static class InsufficientItem implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 物料ID */
        private String materialId;
        /** 物料名称 */
        private String materialName;
        /** 需求数量 */
        private Double requiredQty;
        /** 可用库存数量 */
        private Double availableQty;
        /** 缺口数量 */
        private Double shortageQty;
        /** 缺口百分比 */
        private Double shortagePercent;
        /** 单位 */
        private String unit;
        /** 紧急程度：low/medium/high */
        private String urgency;

        public String getMaterialId() { return materialId; }
        public void setMaterialId(String materialId) { this.materialId = materialId; }
        public String getMaterialName() { return materialName; }
        public void setMaterialName(String materialName) { this.materialName = materialName; }
        public Double getRequiredQty() { return requiredQty; }
        public void setRequiredQty(Double requiredQty) { this.requiredQty = requiredQty; }
        public Double getAvailableQty() { return availableQty; }
        public void setAvailableQty(Double availableQty) { this.availableQty = availableQty; }
        public Double getShortageQty() { return shortageQty; }
        public void setShortageQty(Double shortageQty) { this.shortageQty = shortageQty; }
        public Double getShortagePercent() { return shortagePercent; }
        public void setShortagePercent(Double shortagePercent) { this.shortagePercent = shortagePercent; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
        public String getUrgency() { return urgency; }
        public void setUrgency(String urgency) { this.urgency = urgency; }
    }

    public String getDishId() { return dishId; }
    public void setDishId(String dishId) { this.dishId = dishId; }
    public String getDishName() { return dishName; }
    public void setDishName(String dishName) { this.dishName = dishName; }
    public Boolean getCanMake() { return canMake; }
    public void setCanMake(Boolean canMake) { this.canMake = canMake; }
    public Integer getTotalRequired() { return totalRequired; }
    public void setTotalRequired(Integer totalRequired) { this.totalRequired = totalRequired; }
    public List<InsufficientItem> getInsufficientItems() { return insufficientItems; }
    public void setInsufficientItems(List<InsufficientItem> insufficientItems) { this.insufficientItems = insufficientItems; }
    public List<AlternativeDishVO> getAlternativeDishes() { return alternativeDishes; }
    public void setAlternativeDishes(List<AlternativeDishVO> alternativeDishes) { this.alternativeDishes = alternativeDishes; }
    public String getCheckedAt() { return checkedAt; }
    public void setCheckedAt(String checkedAt) { this.checkedAt = checkedAt; }
}
