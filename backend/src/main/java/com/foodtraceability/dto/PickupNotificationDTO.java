package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 取餐通知DTO
 * 用于厨房通知前端取餐的数据传输
 */
@Schema(description = "取餐通知DTO")
public class PickupNotificationDTO {

    @Schema(description = "订单号")
    private String orderNumber;

    @Schema(description = "桌号")
    private String tableNumber;

    @Schema(description = "订单类型")
    private Integer orderType;

    @Schema(description = "菜品总数")
    private Integer totalDishes;

    public PickupNotificationDTO() {
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getTableNumber() {
        return tableNumber;
    }

    public void setTableNumber(String tableNumber) {
        this.tableNumber = tableNumber;
    }

    public Integer getOrderType() {
        return orderType;
    }

    public void setOrderType(Integer orderType) {
        this.orderType = orderType;
    }

    public Integer getTotalDishes() {
        return totalDishes;
    }

    public void setTotalDishes(Integer totalDishes) {
        this.totalDishes = totalDishes;
    }
}
