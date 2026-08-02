package com.foodtraceability.dto.product;

import java.io.Serializable;
import java.util.List;

/**
 * BOM 批量检查请求 DTO
 * 用于批量检查多个菜品的 BOM 库存情况
 */
public class BatchCheckRequestDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 待检查菜品列表 */
    private List<CheckItem> items;

    /**
     * 检查项
     */
    public static class CheckItem implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 菜品ID */
        private String dishId;
        /** 数量 */
        private Integer quantity;

        public String getDishId() { return dishId; }
        public void setDishId(String dishId) { this.dishId = dishId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }

    public List<CheckItem> getItems() { return items; }
    public void setItems(List<CheckItem> items) { this.items = items; }
}
