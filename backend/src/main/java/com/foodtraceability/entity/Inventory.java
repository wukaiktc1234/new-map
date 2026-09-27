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
 * 统一库存账实体（P1-LOCATION-MODEL-001 M3-M4）
 * 门店与仓库共用一张账表，按 (location_id, material_id) 唯一。
 * ID 空间规则见 docs/design/location-organization-separation-design-002.md §7：
 * location_id 是唯一外键；store_id 是 STORE 型 location 的别名，须经 location_id_map 换算。
 */
@TableName("inventory")
public class Inventory {

    /** 库存ID（主键，自增） */
    @TableId(type = IdType.AUTO)
    private Long inventoryId;

    /** 位置ID（STORE/CENTRAL/DEPOT 统一维度，唯一键之一） */
    @TableField("location_id")
    private Long locationId;

    /** 物料ID（唯一键之二） */
    @TableField("material_id")
    private Long materialId;

    /** 物料名称 */
    @TableField("material_name")
    private String materialName;

    /** 物料编码 */
    @TableField("material_code")
    private String materialCode;

    /** 物料分类ID */
    @TableField("material_category_id")
    private Long materialCategoryId;

    /** 物料分类名称 */
    @TableField("material_category_name")
    private String materialCategoryName;

    /** 规格型号 */
    @TableField("specification")
    private String specification;

    /** 当前库存数量（吸收 T1 current_stock 与 T2 current_stock 口径） */
    @TableField("quantity")
    private BigDecimal quantity;

    /** 单位 */
    @TableField("unit")
    private String unit;

    /** 锁定数量（订单预留） */
    @TableField("locked_quantity")
    private BigDecimal lockedQuantity;

    /** 当前批次号（可空，不进唯一键，D-3 单行化） */
    @TableField("batch_no")
    private String batchNo;

    /** 生产日期 */
    @TableField("production_date")
    private LocalDate productionDate;

    /** 有效期至 */
    @TableField("expiry_date")
    private LocalDate expiryDate;

    /** 门店安全库存线（吸收 T1 safety_stock） */
    @TableField("safety_stock")
    private BigDecimal safetyStock;

    /** 预警阈值（吸收 T2 min_safe_qty，低库存判断用） */
    @TableField("min_safe_qty")
    private BigDecimal minSafeQty;

    /** 最大库存量（吸收 T1 max_stock 与 T2 max_stock_qty） */
    @TableField("max_stock")
    private BigDecimal maxStock;

    /** 库存状态（1:正常 2:预警 3:过期 4:冻结） */
    @TableField("status")
    private Integer status;

    /** 单位成本（分） */
    @TableField("unit_cost")
    private Long unitCost;

    /** 总成本（分） */
    @TableField("total_cost")
    private Long totalCost;

    /** 创建时间 */
    @TableField("create_time")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField("update_time")
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    /** 乐观锁版本号 */
    @Version
    @TableField("version")
    private Integer version;

    // ==================== Transient 兼容性字段（非数据库映射） ====================

    /** 门店ID（STORE 型 location 别名；禁止直接当查询条件，须经 location_id_map —— 宪法 §三.3） */
    @Deprecated
    private transient Long storeId;

    /** 仓库ID（已并入 location_id；仅供未收编调用方编译过渡，S4 收编后移除） */
    @Deprecated
    private transient Long warehouseId;

    /** 产品名称（冗余显示，兼容前端展示） */
    private transient String productName;

    /** 库存类型（1:原料 2:半成品 3:成品 4:包装材料） */
    private transient Integer inventoryType;

    public Long getInventoryId() { return inventoryId; }
    public void setInventoryId(Long inventoryId) { this.inventoryId = inventoryId; }

    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }

    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }

    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }

    public String getMaterialCode() { return materialCode; }
    public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }

    public Long getMaterialCategoryId() { return materialCategoryId; }
    public void setMaterialCategoryId(Long materialCategoryId) { this.materialCategoryId = materialCategoryId; }

    public String getMaterialCategoryName() { return materialCategoryName; }
    public void setMaterialCategoryName(String materialCategoryName) { this.materialCategoryName = materialCategoryName; }

    public String getSpecification() { return specification; }
    public void setSpecification(String specification) { this.specification = specification; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getLockedQuantity() { return lockedQuantity; }
    public void setLockedQuantity(BigDecimal lockedQuantity) { this.lockedQuantity = lockedQuantity; }

    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }

    public LocalDate getProductionDate() { return productionDate; }
    public void setProductionDate(LocalDate productionDate) { this.productionDate = productionDate; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public BigDecimal getSafetyStock() { return safetyStock; }
    public void setSafetyStock(BigDecimal safetyStock) { this.safetyStock = safetyStock; }

    public BigDecimal getMinSafeQty() { return minSafeQty; }
    public void setMinSafeQty(BigDecimal minSafeQty) { this.minSafeQty = minSafeQty; }

    public BigDecimal getMaxStock() { return maxStock; }
    public void setMaxStock(BigDecimal maxStock) { this.maxStock = maxStock; }
    /** 兼容：原 T2 字段别名（max_stock_qty → max_stock） */
    public BigDecimal getMaxStockQty() { return maxStock; }
    public void setMaxStockQty(BigDecimal maxStockQty) { this.maxStock = maxStockQty; }
    /** 兼容：原 int 口径安全线读取 */
    public int getSafetyStockInt() { return safetyStock != null ? safetyStock.intValue() : 0; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Long getUnitCost() { return unitCost; }
    public void setUnitCost(Long unitCost) { this.unitCost = unitCost; }

    public Long getTotalCost() { return totalCost; }
    public void setTotalCost(Long totalCost) { this.totalCost = totalCost; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    // ==================== 兼容性方法（保留既有调用方编译面） ====================

    /** 兼容：T1 口径"当前库存" */
    public BigDecimal getCurrentStock() { return quantity; }
    public void setCurrentStock(BigDecimal currentStock) { this.quantity = currentStock; }

    /** 兼容：product_id 时代别名（= materialId） */
    public Long getProductId() { return materialId; }
    public void setProductId(Long productId) { this.materialId = productId; }


    /** 兼容：ID 别名 */
    public Long getId() { return inventoryId; }
    public void setId(Long id) { this.inventoryId = id; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    @Deprecated
    public Long getStoreId() { return storeId; }
    @Deprecated
    public void setStoreId(Long storeId) { this.storeId = storeId; }

    @Deprecated
    public Long getWarehouseId() { return warehouseId; }
    @Deprecated
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }

    public Integer getInventoryType() { return inventoryType; }
    public void setInventoryType(Integer inventoryType) { this.inventoryType = inventoryType; }

    public String getStoreName() { return "默认仓库"; }

    public void setCreatedAt(java.util.Date date) {
        this.createTime = date != null
            ? date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
            : null;
    }

    public java.util.Date getCreatedAt() {
        return createTime != null
            ? java.util.Date.from(createTime.atZone(java.time.ZoneId.systemDefault()).toInstant())
            : null;
    }

    public void setUpdatedAt(java.util.Date date) {
        this.updateTime = date != null
            ? date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
            : null;
    }

    public java.util.Date getUpdatedAt() {
        return updateTime != null
            ? java.util.Date.from(updateTime.atZone(java.time.ZoneId.systemDefault()).toInstant())
            : null;
    }
}
