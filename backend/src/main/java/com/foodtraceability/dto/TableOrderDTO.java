package com.foodtraceability.dto;

import java.math.BigDecimal;
import java.util.List;

public class TableOrderDTO {
    private Long tableId;
    private String tableNumber;
    private List<TableOrderItem> items;
    private BigDecimal totalAmount;


    public static class TableOrderItem {
        private String id;
        private String name;
        private BigDecimal price;
        private Integer quantity;

        public TableOrderItem() {
        }

        public String getId() {
            return this.id;
        }

        public String getName() {
            return this.name;
        }

        public BigDecimal getPrice() {
            return this.price;
        }

        public Integer getQuantity() {
            return this.quantity;
        }

        public void setId(final String id) {
            this.id = id;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public void setPrice(final BigDecimal price) {
            this.price = price;
        }

        public void setQuantity(final Integer quantity) {
            this.quantity = quantity;
        }

        @java.lang.Override
        public boolean equals(final java.lang.Object o) {
            if (o == this) return true;
            if (!(o instanceof TableOrderDTO.TableOrderItem)) return false;
            final TableOrderDTO.TableOrderItem other = (TableOrderDTO.TableOrderItem) o;
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
            return true;
        }

        protected boolean canEqual(final java.lang.Object other) {
            return other instanceof TableOrderDTO.TableOrderItem;
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
            return result;
        }

        @java.lang.Override
        public java.lang.String toString() {
            return "TableOrderDTO.TableOrderItem(id=" + this.getId() + ", name=" + this.getName() + ", price=" + this.getPrice() + ", quantity=" + this.getQuantity() + ")";
        }
    }

    public TableOrderDTO() {
    }

    public Long getTableId() {
        return this.tableId;
    }

    public String getTableNumber() {
        return this.tableNumber;
    }

    public List<TableOrderItem> getItems() {
        return this.items;
    }

    public BigDecimal getTotalAmount() {
        return this.totalAmount;
    }

    public void setTableId(final Long tableId) {
        this.tableId = tableId;
    }

    public void setTableNumber(final String tableNumber) {
        this.tableNumber = tableNumber;
    }

    public void setItems(final List<TableOrderItem> items) {
        this.items = items;
    }

    public void setTotalAmount(final BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof TableOrderDTO)) return false;
        final TableOrderDTO other = (TableOrderDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$tableId = this.getTableId();
        final java.lang.Object other$tableId = other.getTableId();
        if (this$tableId == null ? other$tableId != null : !this$tableId.equals(other$tableId)) return false;
        final java.lang.Object this$tableNumber = this.getTableNumber();
        final java.lang.Object other$tableNumber = other.getTableNumber();
        if (this$tableNumber == null ? other$tableNumber != null : !this$tableNumber.equals(other$tableNumber)) return false;
        final java.lang.Object this$items = this.getItems();
        final java.lang.Object other$items = other.getItems();
        if (this$items == null ? other$items != null : !this$items.equals(other$items)) return false;
        final java.lang.Object this$totalAmount = this.getTotalAmount();
        final java.lang.Object other$totalAmount = other.getTotalAmount();
        if (this$totalAmount == null ? other$totalAmount != null : !this$totalAmount.equals(other$totalAmount)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof TableOrderDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $tableId = this.getTableId();
        result = result * PRIME + ($tableId == null ? 43 : $tableId.hashCode());
        final java.lang.Object $tableNumber = this.getTableNumber();
        result = result * PRIME + ($tableNumber == null ? 43 : $tableNumber.hashCode());
        final java.lang.Object $items = this.getItems();
        result = result * PRIME + ($items == null ? 43 : $items.hashCode());
        final java.lang.Object $totalAmount = this.getTotalAmount();
        result = result * PRIME + ($totalAmount == null ? 43 : $totalAmount.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "TableOrderDTO(tableId=" + this.getTableId() + ", tableNumber=" + this.getTableNumber() + ", items=" + this.getItems() + ", totalAmount=" + this.getTotalAmount() + ")";
    }
}
