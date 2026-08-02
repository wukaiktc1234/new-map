package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 库存预警创建DTO
 */
@Schema(description = "库存预警创建DTO")
public class InventoryWarningCreateDTO {

    @Schema(description = "库存ID")
    private Long inventoryId;

    @NotNull(message = "产品ID不能为空")
    @Schema(description = "产品ID", example = "1", required = true)
    private Long productId;

    @Schema(description = "产品名称", example = "食用油")
    @Size(max = 100, message = "产品名称长度不能超过100个字符")
    private String productName;

    @NotNull(message = "仓库ID不能为空")
    @Schema(description = "仓库ID", example = "1", required = true)
    private Long warehouseId;

    @Schema(description = "仓库名称", example = "主仓库")
    @Size(max = 100, message = "仓库名称长度不能超过100个字符")
    private String warehouseName;

    @Schema(description = "当前库存数量")
    @Min(value = 0, message = "当前库存数量不能为负数")
    private Integer currentStock;

    @Schema(description = "安全库存数量")
    @Min(value = 0, message = "安全库存数量不能为负数")
    private Integer safeStock;

    @NotNull(message = "预警类型不能为空")
    @Min(value = 1, message = "预警类型无效")
    @Max(value = 2, message = "预警类型无效")
    @Schema(description = "预警类型（1：库存不足，2：库存积压）", example = "1", required = true)
    private Integer warningType;

    @NotNull(message = "预警级别不能为空")
    @Min(value = 1, message = "预警级别无效")
    @Max(value = 3, message = "预警级别无效")
    @Schema(description = "预警级别（1：提示，2：警告，3：严重）", example = "2", required = true)
    private Integer warningLevel;

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

    public Integer getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(Integer currentStock) {
        this.currentStock = currentStock;
    }

    public Integer getSafeStock() {
        return safeStock;
    }

    public void setSafeStock(Integer safeStock) {
        this.safeStock = safeStock;
    }

    public Integer getWarningType() {
        return warningType;
    }

    public void setWarningType(Integer warningType) {
        this.warningType = warningType;
    }

    public Integer getWarningLevel() {
        return warningLevel;
    }

    public void setWarningLevel(Integer warningLevel) {
        this.warningLevel = warningLevel;
    }
}
