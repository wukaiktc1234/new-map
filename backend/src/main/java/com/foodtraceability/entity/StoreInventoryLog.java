package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("store_inventory_log")
@Schema(description = "门店库存日志实体")
public class StoreInventoryLog {

    @TableId(type = IdType.AUTO)
    @Schema(description = "日志ID")
    private Long id;

    @Schema(description = "库存记录ID")
    private Long inventoryId;

    @Schema(description = "产品ID")
    private Long productId;

    @Schema(description = "产品名称")
    private String productName;

    @Schema(description = "门店ID")
    private String storeId;

    @Schema(description = "门店名称")
    private String storeName;

    @Schema(description = "变动类型（1:入库,2:出库,3:调拨,4:盘点,5:损耗）")
    private Integer type;

    @Schema(description = "变动前库存")
    private BigDecimal beforeStock;

    @Schema(description = "变动后库存")
    private BigDecimal afterStock;

    @Schema(description = "变动数量")
    private BigDecimal changeQuantity;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "操作人姓名")
    private String operatorName;

    @Schema(description = "备注")
    private String remark;

    @TableField("create_time")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField("update_time")
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "删除标记")
    private Integer deleted;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getInventoryId() {
        return inventoryId;
    }

    public void setInventoryId(Long inventoryId) {
        this.inventoryId = inventoryId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public BigDecimal getBeforeStock() {
        return beforeStock;
    }

    public void setBeforeStock(BigDecimal beforeStock) {
        this.beforeStock = beforeStock;
    }

    public BigDecimal getAfterStock() {
        return afterStock;
    }

    public void setAfterStock(BigDecimal afterStock) {
        this.afterStock = afterStock;
    }

    public BigDecimal getChangeQuantity() {
        return changeQuantity;
    }

    public void setChangeQuantity(BigDecimal changeQuantity) {
        this.changeQuantity = changeQuantity;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
