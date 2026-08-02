package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 库存预警实体类
 * 用于存储库存预警记录（低库存、积压等）
 */
@TableName("inventory_warning")
public class InventoryWarning {
    /**
     * 主键ID（自增）
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 库存ID
     */
    @TableField("inventory_id")
    private Long inventoryId;

    /**
     * 产品/物料ID
     */
    @TableField("product_id")
    private Long productId;

    /**
     * 产品名称（冗余字段）
     */
    @TableField("product_name")
    private String productName;

    /**
     * 仓库ID
     */
    @TableField("warehouse_id")
    private Long warehouseId;

    /**
     * 仓库名称（冗余字段）
     */
    @TableField("warehouse_name")
    private String warehouseName;

    /**
     * 当前库存数量
     */
    @TableField("current_stock")
    private Integer currentStock;

    /**
     * 安全库存数量
     */
    @TableField("safe_stock")
    private Integer safeStock;

    /**
     * 预警类型（1:库存不足 2:库存积压）
     */
    @TableField("warning_type")
    private Integer warningType;

    /**
     * 预警级别（1:提示 2:警告 3:严重）
     */
    @TableField("warning_level")
    private Integer warningLevel;

    /**
     * 预警状态（0:未处理 1:处理中 2:已解决）
     */
    @TableField("status")
    private Integer status;

    /**
     * 处理人
     */
    @TableField("handler")
    private String handler;

    /**
     * 处理时间
     */
    @TableField("handle_time")
    private LocalDateTime handleTime;

    /**
     * 处理备注
     */
    @TableField("handle_remark")
    private String handleRemark;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;

    /**
     * 最后更新人
     */
    @TableField("update_by")
    private String updateBy;

    /**
     * 逻辑删除标记（0:未删除 1:已删除）
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    public InventoryWarning() {
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getInventoryId() {
        return this.inventoryId;
    }

    public void setInventoryId(Long inventoryId) {
        this.inventoryId = inventoryId;
    }

    public Long getProductId() {
        return this.productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return this.productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Long getWarehouseId() {
        return this.warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getWarehouseName() {
        return this.warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public Integer getCurrentStock() {
        return this.currentStock;
    }

    public void setCurrentStock(Integer currentStock) {
        this.currentStock = currentStock;
    }

    public Integer getSafeStock() {
        return this.safeStock;
    }

    public void setSafeStock(Integer safeStock) {
        this.safeStock = safeStock;
    }

    public Integer getWarningType() {
        return this.warningType;
    }

    public void setWarningType(Integer warningType) {
        this.warningType = warningType;
    }

    public Integer getWarningLevel() {
        return this.warningLevel;
    }

    public void setWarningLevel(Integer warningLevel) {
        this.warningLevel = warningLevel;
    }

    public Integer getStatus() {
        return this.status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getHandler() {
        return this.handler;
    }

    public void setHandler(String handler) {
        this.handler = handler;
    }

    public LocalDateTime getHandleTime() {
        return this.handleTime;
    }

    public void setHandleTime(LocalDateTime handleTime) {
        this.handleTime = handleTime;
    }

    public String getHandleRemark() {
        return this.handleRemark;
    }

    public void setHandleRemark(String handleRemark) {
        this.handleRemark = handleRemark;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getUpdateBy() {
        return this.updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Integer getDeleted() {
        return this.deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    /**
     * 兼容性方法：旧版字段名为 createdAt（java.util.Date）
     */
    public java.util.Date getCreatedAt() {
        return this.createTime != null
            ? java.util.Date.from(this.createTime.atZone(java.time.ZoneId.systemDefault()).toInstant())
            : null;
    }

    /**
     * 兼容性方法：旧版字段名为 createdAt（java.util.Date）
     */
    public void setCreatedAt(java.util.Date createdAt) {
        this.createTime = createdAt != null
            ? createdAt.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
            : null;
    }

    /**
     * 兼容性方法：旧版字段名为 updatedAt（java.util.Date）
     */
    public java.util.Date getUpdatedAt() {
        return this.updateTime != null
            ? java.util.Date.from(this.updateTime.atZone(java.time.ZoneId.systemDefault()).toInstant())
            : null;
    }

    /**
     * 兼容性方法：旧版字段名为 updatedAt（java.util.Date）
     */
    public void setUpdatedAt(java.util.Date updatedAt) {
        this.updateTime = updatedAt != null
            ? updatedAt.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
            : null;
    }
}
