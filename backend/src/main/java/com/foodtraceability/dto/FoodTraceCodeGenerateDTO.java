package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

/**
 * 食品追溯码生成DTO
 */
@Schema(description = "食品追溯码生成DTO")
public class FoodTraceCodeGenerateDTO {
    @Schema(description = "订单ID")
    private String orderId;
    @Schema(description = "订单编号")
    private String orderNumber;
    @Schema(description = "订单类型：0-堂食, 1-外卖, 2-自提")
    private Integer orderType;
    @Schema(description = "菜品ID")
    private String dishId;
    @Schema(description = "菜品名称")
    private String dishName;
    @Schema(description = "菜品价格")
    private BigDecimal dishPrice;
    @Schema(description = "数量")
    private Integer quantity;
    @Schema(description = "后厨订单ID")
    private Long kitchenOrderId;
    @Schema(description = "制作人员ID")
    private Long chefId;
    @Schema(description = "制作人员姓名")
    private String chefName;
    @Schema(description = "门店ID")
    private Long storeId;
    @Schema(description = "门店名称")
    private String storeName;
    @Schema(description = "桌号（堂食时使用）")
    private String tableNumber;
    @Schema(description = "使用的原料追溯码列表")
    private List<String> materialTraceCodes;
    @Schema(description = "原料成本")
    private BigDecimal materialCost;
    @Schema(description = "人工成本")
    private BigDecimal laborCost;
    @Schema(description = "备注")
    private String remark;

    public FoodTraceCodeGenerateDTO() {
    }

    public String getOrderId() {
        return this.orderId;
    }

    public String getOrderNumber() {
        return this.orderNumber;
    }

    public Integer getOrderType() {
        return this.orderType;
    }

    public String getDishId() {
        return this.dishId;
    }

    public String getDishName() {
        return this.dishName;
    }

    public BigDecimal getDishPrice() {
        return this.dishPrice;
    }

    public Integer getQuantity() {
        return this.quantity;
    }

    public Long getKitchenOrderId() {
        return this.kitchenOrderId;
    }

    public Long getChefId() {
        return this.chefId;
    }

    public String getChefName() {
        return this.chefName;
    }

    public Long getStoreId() {
        return this.storeId;
    }

    public String getStoreName() {
        return this.storeName;
    }

    public String getTableNumber() {
        return this.tableNumber;
    }

    public List<String> getMaterialTraceCodes() {
        return this.materialTraceCodes;
    }

    public BigDecimal getMaterialCost() {
        return this.materialCost;
    }

    public BigDecimal getLaborCost() {
        return this.laborCost;
    }

    public String getRemark() {
        return this.remark;
    }

    public void setOrderId(final String orderId) {
        this.orderId = orderId;
    }

    public void setOrderNumber(final String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public void setOrderType(final Integer orderType) {
        this.orderType = orderType;
    }

    public void setDishId(final String dishId) {
        this.dishId = dishId;
    }

    public void setDishName(final String dishName) {
        this.dishName = dishName;
    }

    public void setDishPrice(final BigDecimal dishPrice) {
        this.dishPrice = dishPrice;
    }

    public void setQuantity(final Integer quantity) {
        this.quantity = quantity;
    }

    public void setKitchenOrderId(final Long kitchenOrderId) {
        this.kitchenOrderId = kitchenOrderId;
    }

    public void setChefId(final Long chefId) {
        this.chefId = chefId;
    }

    public void setChefName(final String chefName) {
        this.chefName = chefName;
    }

    public void setStoreId(final Long storeId) {
        this.storeId = storeId;
    }

    public void setStoreName(final String storeName) {
        this.storeName = storeName;
    }

    public void setTableNumber(final String tableNumber) {
        this.tableNumber = tableNumber;
    }

    public void setMaterialTraceCodes(final List<String> materialTraceCodes) {
        this.materialTraceCodes = materialTraceCodes;
    }

    public void setMaterialCost(final BigDecimal materialCost) {
        this.materialCost = materialCost;
    }

    public void setLaborCost(final BigDecimal laborCost) {
        this.laborCost = laborCost;
    }

    public void setRemark(final String remark) {
        this.remark = remark;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof FoodTraceCodeGenerateDTO)) return false;
        final FoodTraceCodeGenerateDTO other = (FoodTraceCodeGenerateDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$orderType = this.getOrderType();
        final java.lang.Object other$orderType = other.getOrderType();
        if (this$orderType == null ? other$orderType != null : !this$orderType.equals(other$orderType)) return false;
        final java.lang.Object this$quantity = this.getQuantity();
        final java.lang.Object other$quantity = other.getQuantity();
        if (this$quantity == null ? other$quantity != null : !this$quantity.equals(other$quantity)) return false;
        final java.lang.Object this$kitchenOrderId = this.getKitchenOrderId();
        final java.lang.Object other$kitchenOrderId = other.getKitchenOrderId();
        if (this$kitchenOrderId == null ? other$kitchenOrderId != null : !this$kitchenOrderId.equals(other$kitchenOrderId)) return false;
        final java.lang.Object this$chefId = this.getChefId();
        final java.lang.Object other$chefId = other.getChefId();
        if (this$chefId == null ? other$chefId != null : !this$chefId.equals(other$chefId)) return false;
        final java.lang.Object this$storeId = this.getStoreId();
        final java.lang.Object other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !this$storeId.equals(other$storeId)) return false;
        final java.lang.Object this$orderId = this.getOrderId();
        final java.lang.Object other$orderId = other.getOrderId();
        if (this$orderId == null ? other$orderId != null : !this$orderId.equals(other$orderId)) return false;
        final java.lang.Object this$orderNumber = this.getOrderNumber();
        final java.lang.Object other$orderNumber = other.getOrderNumber();
        if (this$orderNumber == null ? other$orderNumber != null : !this$orderNumber.equals(other$orderNumber)) return false;
        final java.lang.Object this$dishId = this.getDishId();
        final java.lang.Object other$dishId = other.getDishId();
        if (this$dishId == null ? other$dishId != null : !this$dishId.equals(other$dishId)) return false;
        final java.lang.Object this$dishName = this.getDishName();
        final java.lang.Object other$dishName = other.getDishName();
        if (this$dishName == null ? other$dishName != null : !this$dishName.equals(other$dishName)) return false;
        final java.lang.Object this$dishPrice = this.getDishPrice();
        final java.lang.Object other$dishPrice = other.getDishPrice();
        if (this$dishPrice == null ? other$dishPrice != null : !this$dishPrice.equals(other$dishPrice)) return false;
        final java.lang.Object this$chefName = this.getChefName();
        final java.lang.Object other$chefName = other.getChefName();
        if (this$chefName == null ? other$chefName != null : !this$chefName.equals(other$chefName)) return false;
        final java.lang.Object this$storeName = this.getStoreName();
        final java.lang.Object other$storeName = other.getStoreName();
        if (this$storeName == null ? other$storeName != null : !this$storeName.equals(other$storeName)) return false;
        final java.lang.Object this$tableNumber = this.getTableNumber();
        final java.lang.Object other$tableNumber = other.getTableNumber();
        if (this$tableNumber == null ? other$tableNumber != null : !this$tableNumber.equals(other$tableNumber)) return false;
        final java.lang.Object this$materialTraceCodes = this.getMaterialTraceCodes();
        final java.lang.Object other$materialTraceCodes = other.getMaterialTraceCodes();
        if (this$materialTraceCodes == null ? other$materialTraceCodes != null : !this$materialTraceCodes.equals(other$materialTraceCodes)) return false;
        final java.lang.Object this$materialCost = this.getMaterialCost();
        final java.lang.Object other$materialCost = other.getMaterialCost();
        if (this$materialCost == null ? other$materialCost != null : !this$materialCost.equals(other$materialCost)) return false;
        final java.lang.Object this$laborCost = this.getLaborCost();
        final java.lang.Object other$laborCost = other.getLaborCost();
        if (this$laborCost == null ? other$laborCost != null : !this$laborCost.equals(other$laborCost)) return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof FoodTraceCodeGenerateDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $orderType = this.getOrderType();
        result = result * PRIME + ($orderType == null ? 43 : $orderType.hashCode());
        final java.lang.Object $quantity = this.getQuantity();
        result = result * PRIME + ($quantity == null ? 43 : $quantity.hashCode());
        final java.lang.Object $kitchenOrderId = this.getKitchenOrderId();
        result = result * PRIME + ($kitchenOrderId == null ? 43 : $kitchenOrderId.hashCode());
        final java.lang.Object $chefId = this.getChefId();
        result = result * PRIME + ($chefId == null ? 43 : $chefId.hashCode());
        final java.lang.Object $storeId = this.getStoreId();
        result = result * PRIME + ($storeId == null ? 43 : $storeId.hashCode());
        final java.lang.Object $orderId = this.getOrderId();
        result = result * PRIME + ($orderId == null ? 43 : $orderId.hashCode());
        final java.lang.Object $orderNumber = this.getOrderNumber();
        result = result * PRIME + ($orderNumber == null ? 43 : $orderNumber.hashCode());
        final java.lang.Object $dishId = this.getDishId();
        result = result * PRIME + ($dishId == null ? 43 : $dishId.hashCode());
        final java.lang.Object $dishName = this.getDishName();
        result = result * PRIME + ($dishName == null ? 43 : $dishName.hashCode());
        final java.lang.Object $dishPrice = this.getDishPrice();
        result = result * PRIME + ($dishPrice == null ? 43 : $dishPrice.hashCode());
        final java.lang.Object $chefName = this.getChefName();
        result = result * PRIME + ($chefName == null ? 43 : $chefName.hashCode());
        final java.lang.Object $storeName = this.getStoreName();
        result = result * PRIME + ($storeName == null ? 43 : $storeName.hashCode());
        final java.lang.Object $tableNumber = this.getTableNumber();
        result = result * PRIME + ($tableNumber == null ? 43 : $tableNumber.hashCode());
        final java.lang.Object $materialTraceCodes = this.getMaterialTraceCodes();
        result = result * PRIME + ($materialTraceCodes == null ? 43 : $materialTraceCodes.hashCode());
        final java.lang.Object $materialCost = this.getMaterialCost();
        result = result * PRIME + ($materialCost == null ? 43 : $materialCost.hashCode());
        final java.lang.Object $laborCost = this.getLaborCost();
        result = result * PRIME + ($laborCost == null ? 43 : $laborCost.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "FoodTraceCodeGenerateDTO(orderId=" + this.getOrderId() + ", orderNumber=" + this.getOrderNumber() + ", orderType=" + this.getOrderType() + ", dishId=" + this.getDishId() + ", dishName=" + this.getDishName() + ", dishPrice=" + this.getDishPrice() + ", quantity=" + this.getQuantity() + ", kitchenOrderId=" + this.getKitchenOrderId() + ", chefId=" + this.getChefId() + ", chefName=" + this.getChefName() + ", storeId=" + this.getStoreId() + ", storeName=" + this.getStoreName() + ", tableNumber=" + this.getTableNumber() + ", materialTraceCodes=" + this.getMaterialTraceCodes() + ", materialCost=" + this.getMaterialCost() + ", laborCost=" + this.getLaborCost() + ", remark=" + this.getRemark() + ")";
    }
}
