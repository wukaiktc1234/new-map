package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("other_inbound")
public class OtherInbound {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("inbound_no")
    private String inboundNo;
    
    @TableField("inbound_type")
    private String inboundType;
    
    @TableField("product_id")
    private Long productId;
    
    @TableField("product_name")
    private String productName;
    
    @TableField("quantity")
    private Integer quantity;
    
    @TableField("unit")
    private String unit;
    
    @TableField("warehouse_id")
    private Long warehouseId;
    
    @TableField("warehouse_name")
    private String warehouseName;
    
    @TableField("inbound_time")
    private LocalDateTime inboundTime;
    
    @TableField("operator_id")
    private Long operatorId;
    
    @TableField("operator_name")
    private String operatorName;
    
    @TableField("source_id")
    private Long sourceId;
    
    @TableField("return_source")
    private String returnSource;
    
    @TableField("remark")
    private String remark;
    
    @TableField("status")
    private Integer status;
    
    @TableField("deleted")
    private Integer deleted;
    
    @TableField("create_time")
    private LocalDateTime createdAt;
    
    @TableField("update_time")
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInboundNo() {
        return inboundNo;
    }

    public void setInboundNo(String inboundNo) {
        this.inboundNo = inboundNo;
    }

    public String getInboundType() {
        return inboundType;
    }

    public void setInboundType(String inboundType) {
        this.inboundType = inboundType;
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

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public LocalDateTime getInboundTime() {
        return inboundTime;
    }

    public void setInboundTime(LocalDateTime inboundTime) {
        this.inboundTime = inboundTime;
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

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public String getReturnSource() {
        return returnSource;
    }

    public void setReturnSource(String returnSource) {
        this.returnSource = returnSource;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
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
}
