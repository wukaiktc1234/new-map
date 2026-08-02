package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 库存主数据实体类
 * 用于存储物料库存信息，包括数量、批次、成本等
 */
@TableName("inventory")
public class Inventory {
    /**
     * 库存ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long inventoryId;

    /**
     * 物料ID
     */
    @TableField("material_id")
    private Long materialId;

    /**
     * 物料名称
     */
    @TableField("material_name")
    private String materialName;

    /**
     * 物料编码
     */
    @TableField("material_code")
    private String materialCode;

    /**
     * 物料分类ID
     */
    @TableField("material_category_id")
    private Long materialCategoryId;

    /**
     * 物料分类名称
     */
    @TableField("material_category_name")
    private String materialCategoryName;

    /**
     * 规格型号
     */
    @TableField("specification")
    private String specification;

    /**
     * 单位
     */
    @TableField("unit")
    private String unit;

    /**
     * 所属仓库ID
     */
    @TableField("warehouse_id")
    private Long warehouseId;

    /**
     * 所属库位ID
     */
    @TableField("location_id")
    private Long locationId;

    /**
     * 当前库存数量（数据库列 current_stock）
     */
    @TableField("current_stock")
    private BigDecimal quantity;

    /**
     * 锁定数量（用于订单预留）
     */
    @TableField("locked_quantity")
    private BigDecimal lockedQuantity;

    /**
     * 当前批次号
     */
    @TableField("batch_no")
    private String batchNo;

    /**
     * 生产日期
     */
    @TableField("production_date")
    private LocalDate productionDate;

    /**
     * 有效期至
     */
    @TableField("expiry_date")
    private LocalDate expiryDate;

    /**
     * 单位成本（分）
     */
    @TableField("unit_cost")
    private Long unitCost;

    /**
     * 总成本（分）
     */
    @TableField("total_cost")
    private Long totalCost;

    /**
     * 安全库存量
     */
    @TableField("min_safe_qty")
    private BigDecimal minSafeQty;

    /**
     * 最大库存量
     */
    @TableField("max_stock_qty")
    private BigDecimal maxStockQty;

    /**
     * 库存状态（1:正常 2:预警 3:过期 4:冻结）
     */
    @TableField("status")
    private Integer status;

    /**
     * 创建时间（数据库列 created_at）
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间（数据库列 updated_at）
     */
    @TableField("update_time")
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记（0:未删除 1:已删除）
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    /**
     * 乐观锁版本号
     * MyBatis-Plus 自动在 UPDATE 时添加 WHERE version = ? 条件，
     * 并将 version 递增。若并发更新导致版本不匹配，updateById 返回 0。
     */
    @Version
    @TableField("version")
    private Integer version;

    // ==================== Transient 兼容性字段（非数据库映射） ====================

    /** 产品名称（冗余显示，兼容前端展示） */
    private transient String productName;

    /** 门店ID（多门店场景，冗余存储） */
    private transient Long storeId;

    /** 库存类型（1:原料 2:半成品 3:成品 4:包装材料） */
    private transient Integer inventoryType;

    public Long getInventoryId() {
        return inventoryId;
    }

    public void setInventoryId(Long inventoryId) {
        this.inventoryId = inventoryId;
    }

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public String getMaterialName() {
        return materialName;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }

    public String getMaterialCode() {
        return materialCode;
    }

    public void setMaterialCode(String materialCode) {
        this.materialCode = materialCode;
    }

    public Long getMaterialCategoryId() {
        return materialCategoryId;
    }

    public void setMaterialCategoryId(Long materialCategoryId) {
        this.materialCategoryId = materialCategoryId;
    }

    public String getMaterialCategoryName() {
        return materialCategoryName;
    }

    public void setMaterialCategoryName(String materialCategoryName) {
        this.materialCategoryName = materialCategoryName;
    }

    public String getSpecification() {
        return specification;
    }

    public void setSpecification(String specification) {
        this.specification = specification;
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

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getLockedQuantity() {
        return lockedQuantity;
    }

    public void setLockedQuantity(BigDecimal lockedQuantity) {
        this.lockedQuantity = lockedQuantity;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public LocalDate getProductionDate() {
        return productionDate;
    }

    public void setProductionDate(LocalDate productionDate) {
        this.productionDate = productionDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Long getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(Long unitCost) {
        this.unitCost = unitCost;
    }

    public Long getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(Long totalCost) {
        this.totalCost = totalCost;
    }

    public BigDecimal getMinSafeQty() {
        return minSafeQty;
    }

    public void setMinSafeQty(BigDecimal minSafeQty) {
        this.minSafeQty = minSafeQty;
    }

    public BigDecimal getMaxStockQty() {
        return maxStockQty;
    }

    public void setMaxStockQty(BigDecimal maxStockQty) {
        this.maxStockQty = maxStockQty;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    // ==================== 兼容性方法 ====================

    public BigDecimal getCurrentStock() {
        return quantity;
    }

    public void setCurrentStock(BigDecimal currentStock) {
        this.quantity = currentStock;
    }

    // ==================== 兼容性方法 ====================

    public Long getProductId() {
        return materialId;
    }

    public void setProductId(Long productId) {
        this.materialId = productId;
    }

    public void setSafetyStock(BigDecimal safetyStock) {
        this.minSafeQty = safetyStock;
    }

    public int getSafetyStock() {
        return minSafeQty != null ? minSafeQty.intValue() : 0;
    }

    public String getStoreName() {
        return "默认仓库";
    }

    // ==================== ID 兼容性方法 ====================

    /**
     * 兼容性方法：获取ID（映射到 inventoryId）
     */
    public Long getId() {
        return inventoryId;
    }

    /**
     * 兼容性方法：设置ID（映射到 inventoryId）
     */
    public void setId(Long id) {
        this.inventoryId = id;
    }

    // ==================== 产品名称兼容性 ====================

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    // ==================== 门店ID兼容性 ====================

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    // ==================== 库存类型兼容性 ====================

    public Integer getInventoryType() {
        return inventoryType;
    }

    public void setInventoryType(Integer inventoryType) {
        this.inventoryType = inventoryType;
    }

    // ==================== Date 类型兼容性方法 ====================

    /**
     * 兼容性方法：设置创建时间（java.util.Date -> LocalDateTime 转换）
     * 用于与使用旧版 Date 类型的代码兼容
     */
    public void setCreatedAt(java.util.Date date) {
        this.createTime = date != null
            ? date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
            : null;
    }

    /**
     * 兼容性方法：获取创建时间（别名方法）
     */
    public java.util.Date getCreatedAt() {
        return createTime != null
            ? java.util.Date.from(createTime.atZone(java.time.ZoneId.systemDefault()).toInstant())
            : null;
    }

    /**
     * 兼容性方法：设置更新时间（java.util.Date -> LocalDateTime 转换）
     * 用于与使用旧版 Date 类型的代码兼容
     */
    public void setUpdatedAt(java.util.Date date) {
        this.updateTime = date != null
            ? date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
            : null;
    }

    /**
     * 兼容性方法：获取更新时间（别名方法）
     */
    public java.util.Date getUpdatedAt() {
        return updateTime != null
            ? java.util.Date.from(updateTime.atZone(java.time.ZoneId.systemDefault()).toInstant())
            : null;
    }
}
