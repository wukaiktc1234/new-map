package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 带等待时间的后厨订单DTO
 * 用于智能排序查询结果
 */
@Schema(description = "带等待时间的后厨订单DTO")
public class KitchenOrderWithWaitTimeDTO {
    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "后厨订单ID")
    private String kitchenOrderId;

    @Schema(description = "订单ID")
    private String orderId;

    @Schema(description = "订单编号")
    private String orderNumber;

    @Schema(description = "订单类型：0-堂食, 1-外卖, 2-自提")
    private Integer orderType;

    @Schema(description = "桌号")
    private String tableNumber;

    @Schema(description = "菜品总数")
    private Integer totalDishes;

    @Schema(description = "优先级：0-普通, 1-加急, 2-特急")
    private Integer priority;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "等待时间(分钟)")
    private Long waitMinutes;

    @Schema(description = "厨师姓名")
    private String chefName;

    @Schema(description = "门店名称")
    private String storeName;

    public KitchenOrderWithWaitTimeDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKitchenOrderId() {
        return kitchenOrderId;
    }

    public void setKitchenOrderId(String kitchenOrderId) {
        this.kitchenOrderId = kitchenOrderId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Integer getOrderType() {
        return orderType;
    }

    public void setOrderType(Integer orderType) {
        this.orderType = orderType;
    }

    public String getTableNumber() {
        return tableNumber;
    }

    public void setTableNumber(String tableNumber) {
        this.tableNumber = tableNumber;
    }

    public Integer getTotalDishes() {
        return totalDishes;
    }

    public void setTotalDishes(Integer totalDishes) {
        this.totalDishes = totalDishes;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public Long getWaitMinutes() {
        return waitMinutes;
    }

    public void setWaitMinutes(Long waitMinutes) {
        this.waitMinutes = waitMinutes;
    }

    public String getChefName() {
        return chefName;
    }

    public void setChefName(String chefName) {
        this.chefName = chefName;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }
}
