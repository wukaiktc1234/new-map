package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 按菜品聚合的订单视图DTO
 * 方便厨师看到"宫保鸡丁还需要做3份"这种视图
 */
@Schema(description = "按菜品聚合的订单视图DTO")
public class FoodGroupedOrderDTO {
    @Schema(description = "菜品ID")
    private String dishId;

    @Schema(description = "菜品名称")
    private String dishName;

    @Schema(description = "待制作总数量")
    private Integer totalQuantity;

    @Schema(description = "涉及订单数")
    private Integer orderCount;

    @Schema(description = "关联的后厨订单ID列表")
    private List<String> kitchenOrderIds;

    @Schema(description = "最高优先级")
    private Integer maxPriority;

    @Schema(description = "最早下单时间对应的等待分钟数")
    private Long minWaitMinutes;

    public FoodGroupedOrderDTO() {
    }

    public String getDishId() {
        return dishId;
    }

    public void setDishId(String dishId) {
        this.dishId = dishId;
    }

    public String getDishName() {
        return dishName;
    }

    public void setDishName(String dishName) {
        this.dishName = dishName;
    }

    public Integer getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public Integer getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Integer orderCount) {
        this.orderCount = orderCount;
    }

    public List<String> getKitchenOrderIds() {
        return kitchenOrderIds;
    }

    public void setKitchenOrderIds(List<String> kitchenOrderIds) {
        this.kitchenOrderIds = kitchenOrderIds;
    }

    public Integer getMaxPriority() {
        return maxPriority;
    }

    public void setMaxPriority(Integer maxPriority) {
        this.maxPriority = maxPriority;
    }

    public Long getMinWaitMinutes() {
        return minWaitMinutes;
    }

    public void setMinWaitMinutes(Long minWaitMinutes) {
        this.minWaitMinutes = minWaitMinutes;
    }
}
