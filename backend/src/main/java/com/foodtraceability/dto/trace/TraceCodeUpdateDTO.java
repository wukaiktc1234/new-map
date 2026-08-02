package com.foodtraceability.dto.trace;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * 追溯码更新DTO
 */
@Schema(description = "追溯码更新请求")
public class TraceCodeUpdateDTO {

    /** 目标名称 */
    @Schema(description = "目标名称")
    private String targetName;

    /** 批次号 */
    @Schema(description = "批次号")
    private String batchNo;

    /** 生产/加工日期 */
    @Schema(description = "生产日期")
    private LocalDate productionDate;

    /** 有效期至 */
    @Schema(description = "有效期至")
    private LocalDate expiryDate;

    /** 供应商名称 */
    @Schema(description = "供应商名称")
    private String supplierName;

    /** 仓库ID */
    @Schema(description = "仓库ID")
    private Long warehouseId;

    /** 当前位置 */
    @Schema(description = "当前位置")
    private String currentLocation;

    /** 状态：1正常 2即将过期 3已过期 4已召回 5已消费 */
    @Schema(description = "状态")
    private Integer status;

    /** 风险等级：1低风险 2中风险 3高风险 */
    @Schema(description = "风险等级")
    private Integer riskLevel;

    /** 二维码图片URL */
    @Schema(description = "二维码图片URL")
    private String qrCodeImageUrl;

    /** 额外信息 */
    @Schema(description = "额外信息")
    private String extraInfo;

    // Getter和Setter方法

    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public LocalDate getProductionDate() { return productionDate; }
    public void setProductionDate(LocalDate productionDate) { this.productionDate = productionDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    public String getCurrentLocation() { return currentLocation; }
    public void setCurrentLocation(String currentLocation) { this.currentLocation = currentLocation; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getRiskLevel() { return riskLevel; }
    public void setRiskLevel(Integer riskLevel) { this.riskLevel = riskLevel; }
    public String getQrCodeImageUrl() { return qrCodeImageUrl; }
    public void setQrCodeImageUrl(String qrCodeImageUrl) { this.qrCodeImageUrl = qrCodeImageUrl; }
    public String getExtraInfo() { return extraInfo; }
    public void setExtraInfo(String extraInfo) { this.extraInfo = extraInfo; }
}
