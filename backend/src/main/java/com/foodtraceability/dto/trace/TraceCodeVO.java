package com.foodtraceability.dto.trace;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 追溯码VO - 前端展示对象
 */
@Schema(description = "追溯码信息")
public class TraceCodeVO {

    /** 主键ID */
    @Schema(description = "主键ID")
    private Long traceCodeId;

    /** 追溯码 */
    @Schema(description = "追溯码")
    private String traceCode;

    /** 追溯类型 */
    @Schema(description = "追溯类型")
    private Integer traceType;

    /** 追溯类型名称 */
    @Schema(description = "追溯类型名称")
    private String traceTypeName;

    /** 目标类型 */
    @Schema(description = "目标类型")
    private Integer targetType;

    /** 目标类型名称 */
    @Schema(description = "目标类型名称")
    private String targetTypeName;

    /** 目标ID */
    @Schema(description = "目标ID")
    private Long targetId;

    /** 目标名称 */
    @Schema(description = "目标名称")
    private String targetName;

    /** 批次号 */
    @Schema(description = "批次号")
    private String batchNo;

    /** 生产日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "生产日期")
    private LocalDate productionDate;

    /** 有效期至 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "有效期至")
    private LocalDate expiryDate;

    /** 供应商ID */
    @Schema(description = "供应商ID")
    private Long supplierId;

    /** 供应商名称 */
    @Schema(description = "供应商名称")
    private String supplierName;

    /** 入库单ID */
    @Schema(description = "入库单ID")
    private Long purchaseStockinId;

    /** 仓库ID */
    @Schema(description = "仓库ID")
    private Long warehouseId;

    /** 当前位置 */
    @Schema(description = "当前位置")
    private String currentLocation;

    /** 状态 */
    @Schema(description = "状态")
    private Integer status;

    /** 状态名称 */
    @Schema(description = "状态名称")
    private String statusName;

    /** 风险等级 */
    @Schema(description = "风险等级")
    private Integer riskLevel;

    /** 风险等级名称 */
    @Schema(description = "风险等级名称")
    private String riskLevelName;

    /** 二维码图片URL */
    @Schema(description = "二维码图片URL")
    private String qrCodeImageUrl;

    /** 追溯链节点列表 */
    @Schema(description = "追溯链节点列表")
    private List<TraceChainNodeVO> chainNodes;

    /** 额外信息 */
    @Schema(description = "额外信息")
    private String extraInfo;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    // Getter和Setter方法

    public Long getTraceCodeId() { return traceCodeId; }
    public void setTraceCodeId(Long traceCodeId) { this.traceCodeId = traceCodeId; }
    public String getTraceCode() { return traceCode; }
    public void setTraceCode(String traceCode) { this.traceCode = traceCode; }
    public Integer getTraceType() { return traceType; }
    public void setTraceType(Integer traceType) { this.traceType = traceType; }
    public String getTraceTypeName() { return traceTypeName; }
    public void setTraceTypeName(String traceTypeName) { this.traceTypeName = traceTypeName; }
    public Integer getTargetType() { return targetType; }
    public void setTargetType(Integer targetType) { this.targetType = targetType; }
    public String getTargetTypeName() { return targetTypeName; }
    public void setTargetTypeName(String targetTypeName) { this.targetTypeName = targetTypeName; }
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
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }
    public Integer getRiskLevel() { return riskLevel; }
    public void setRiskLevel(Integer riskLevel) { this.riskLevel = riskLevel; }
    public String getRiskLevelName() { return riskLevelName; }
    public void setRiskLevelName(String riskLevelName) { this.riskLevelName = riskLevelName; }
    public String getQrCodeImageUrl() { return qrCodeImageUrl; }
    public void setQrCodeImageUrl(String qrCodeImageUrl) { this.qrCodeImageUrl = qrCodeImageUrl; }
    public List<TraceChainNodeVO> getChainNodes() { return chainNodes; }
    public void setChainNodes(List<TraceChainNodeVO> chainNodes) { this.chainNodes = chainNodes; }
    public String getExtraInfo() { return extraInfo; }
    public void setExtraInfo(String extraInfo) { this.extraInfo = extraInfo; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
