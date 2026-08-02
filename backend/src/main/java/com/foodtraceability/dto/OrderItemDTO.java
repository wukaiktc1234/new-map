package com.foodtraceability.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * POS订单项DTO
 */
@Schema(description = "POS订单项DTO")
public class OrderItemDTO {
    @Schema(description = "项ID")
    @JsonAlias({"productId", "foodId"})
    @NotBlank(message = "菜品ID不能为空")
    private String id;
    @Schema(description = "名称")
    @JsonAlias({"productName", "foodName"})
    @NotBlank(message = "菜品名称不能为空")
    @Size(max = 100, message = "菜品名称长度不能超过100个字符")
    private String name;
    @Schema(description = "数量")
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量必须大于0")
    @Max(value = 9999, message = "数量不能超过9999")
    private Integer quantity;
    @Schema(description = "价格")
    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格必须大于0")
    @DecimalMax(value = "99999.00", message = "价格不能超过99999.00")
    private BigDecimal price;
    @Schema(description = "菜品类型")
    private String dishType;
    @Schema(description = "子项列表")
    @Valid
    private List<OrderItemDTO> items;

    public OrderItemDTO() {
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public Integer getQuantity() {
        return this.quantity;
    }

    public BigDecimal getPrice() {
        return this.price;
    }

    public String getDishType() {
        return this.dishType;
    }

    public List<OrderItemDTO> getItems() {
        return this.items;
    }

    @JsonAlias({"productId", "foodId"})
    public void setId(final String id) {
        this.id = id;
    }

    @JsonAlias({"productName", "foodName"})
    public void setName(final String name) {
        this.name = name;
    }

    public void setQuantity(final Integer quantity) {
        this.quantity = quantity;
    }

    public void setPrice(final BigDecimal price) {
        this.price = price;
    }

    public void setDishType(final String dishType) {
        this.dishType = dishType;
    }

    public void setItems(final List<OrderItemDTO> items) {
        this.items = items;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof OrderItemDTO)) return false;
        final OrderItemDTO other = (OrderItemDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$quantity = this.getQuantity();
        final java.lang.Object other$quantity = other.getQuantity();
        if (this$quantity == null ? other$quantity != null : !this$quantity.equals(other$quantity)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$name = this.getName();
        final java.lang.Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final java.lang.Object this$price = this.getPrice();
        final java.lang.Object other$price = other.getPrice();
        if (this$price == null ? other$price != null : !this$price.equals(other$price)) return false;
        final java.lang.Object this$dishType = this.getDishType();
        final java.lang.Object other$dishType = other.getDishType();
        if (this$dishType == null ? other$dishType != null : !this$dishType.equals(other$dishType)) return false;
        final java.lang.Object this$items = this.getItems();
        final java.lang.Object other$items = other.getItems();
        if (this$items == null ? other$items != null : !this$items.equals(other$items)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof OrderItemDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $quantity = this.getQuantity();
        result = result * PRIME + ($quantity == null ? 43 : $quantity.hashCode());
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final java.lang.Object $price = this.getPrice();
        result = result * PRIME + ($price == null ? 43 : $price.hashCode());
        final java.lang.Object $dishType = this.getDishType();
        result = result * PRIME + ($dishType == null ? 43 : $dishType.hashCode());
        final java.lang.Object $items = this.getItems();
        result = result * PRIME + ($items == null ? 43 : $items.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "OrderItemDTO(id=" + this.getId() + ", name=" + this.getName() + ", quantity=" + this.getQuantity() + ", price=" + this.getPrice() + ", dishType=" + this.getDishType() + ", items=" + this.getItems() + ")";
    }
}
