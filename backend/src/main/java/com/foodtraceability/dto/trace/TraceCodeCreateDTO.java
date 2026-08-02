package com.foodtraceability.dto.trace;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 追溯码创建DTO
 */
@Schema(description = "追溯码创建请求")
public class TraceCodeCreateDTO {

    /** 追溯类型：1菜品追溯码 2原料批次追溯码 3物流追溯码 4检验报告码 */
    @NotNull(message = "追溯类型不能为空")
    @Min(value = 1, message = "追溯类型无效")
    @Max(value = 4, message = "追溯类型无效")
    @Schema(description = "追溯类型", example = "1")
    private Integer traceType;

    /** 目标类型：1成品菜品 2半成品 3原材料 4包装材料 */
    @NotNull(message = "目标类型不能为空")
    @Min(value = 1, message = "目标类型无效")
    @Max(value = 4, message = "目标类型无效")
    @Schema(description = "目标类型", example = "1")
    private Integer targetType;

    /** 关联的目标ID */
    @Schema(description = "目标ID")
    private Long targetId;

    /** 目标名称 */
    @NotBlank(message = "目标名称不能为空")
    @Size(max = 200, message = "目标名称长度不能超过200个字符")
    @Schema(description = "目标名称", example = "红烧牛肉面")
    private String targetName;

    /** 批次号 */
    @Size(max = 64, message = "批次号长度不能超过64个字符")
    @Schema(description = "批次号", example = "BATCH20260420")
    private String batchNo;

    /** 生产/加工日期 */
    @Schema(description = "生产日期")
    private LocalDate productionDate;

    /** 有效期至 */
    @Schema(description = "有效期至")
    private LocalDate expiryDate;

    /** 供应商ID */
    @Schema(description = "供应商ID")
    private Long supplierId;

    /** 供应商名称 */
    @Size(max = 100, message = "供应商名称长度不能超过100个字符")
    @Schema(description = "供应商名称", example = "绿色农场")
    private String supplierName;

    /** 入库单ID */
    @Schema(description = "入库单ID")
    private Long purchaseStockinId;

    /** 仓库ID */
    @Schema(description = "仓库ID")
    private Long warehouseId;

    /** 当前位置 */
    @Size(max = 200, message = "当前位置长度不能超过200个字符")
    @Schema(description = "当前位置", example = "中央厨房冷库A区")
    private String currentLocation;

    /** 额外信息（如检测报告编号） */
    @Size(max = 500, message = "额外信息长度不能超过500个字符")
    @Schema(description = "额外信息")
    private String extraInfo;

    public Integer getTraceType() { return traceType; }
    public void setTraceType(Integer traceType) { this.traceType = traceType; }
    public Integer getTargetType() { return targetType; }
    public void setTargetType(Integer targetType) { this.targetType = targetType; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public LocalDate getProductionDate() { return productionDate; }
    public void setProductionDate(LocalDate productionDate) { this.productionDate = productionDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public Long getPurchaseStockinId() { return purchaseStockinId; }
    public void setPurchaseStockinId(Long purchaseStockinId) { this.purchaseStockinId = purchaseStockinId; }
    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    public String getCurrentLocation() { return currentLocation; }
    public void setCurrentLocation(String currentLocation) { this.currentLocation = currentLocation; }
    public String getExtraInfo() { return extraInfo; }
    public void setExtraInfo(String extraInfo) { this.extraInfo = extraInfo; }
}
