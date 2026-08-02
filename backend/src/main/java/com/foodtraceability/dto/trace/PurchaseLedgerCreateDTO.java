package com.foodtraceability.dto.trace;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 进货台账创建DTO
 */
@Schema(description = "进货台账创建请求")
public class PurchaseLedgerCreateDTO {

    /** 关联采购入库单ID */
    @Schema(description = "采购入库单ID")
    private Long purchaseStockinId;

    /** 供应商ID */
    @NotNull(message = "供应商不能为空")
    @Schema(description = "供应商ID")
    private Long supplierId;

    /** 供应商名称 */
    @NotBlank(message = "供应商名称不能为空")
    @Schema(description = "供应商名称")
    private String supplierName;

    /** 物料名称 */
    @NotBlank(message = "物料名称不能为空")
    @Schema(description = "物料名称")
    private String materialName;

    /** 规格型号 */
    @Schema(description = "规格型号")
    private String specification;

    /** 单位 */
    @NotBlank(message = "单位不能为空")
    @Schema(description = "单位")
    private String unit;

    /** 数量 */
    @NotNull(message = "数量不能为空")
    @Schema(description = "数量")
    private BigDecimal quantity;

    /** 单价 */
    @NotNull(message = "单价不能为空")
    @Schema(description = "单价")
    private BigDecimal unitPrice;

    /** 批次号 */
    @Schema(description = "批次号")
    private String batchNo;

    /** 生产日期 */
    @Schema(description = "生产日期")
    private LocalDate productionDate;

    /** 有效期至 */
    @Schema(description = "有效期至")
    private LocalDate expiryDate;

    /** 质检结果：1合格 2不合格 3待检 */
    @Schema(description = "质检结果")
    private Integer qualityInspectionResult;

    /** 证件号码 */
    @Schema(description = "证件号码")
    private String certificateNo;

    /** 证件类型：1检疫证 2合格证 3检测报告 4其他 */
    @Schema(description = "证件类型")
    private Integer certificateType;

    /** 证件图片URL */
    @Schema(description = "证件图片URL")
    private String certificateImageUrl;

    /** 存放位置 */
    @Schema(description = "存放位置")
    private String storageLocation;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;

    // Getter和Setter方法

    public Long getPurchaseStockinId() { return purchaseStockinId; }
    public void setPurchaseStockinId(Long purchaseStockinId) { this.purchaseStockinId = purchaseStockinId; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }
    public String getSpecification() { return specification; }
    public void setSpecification(String specification) { this.specification = specification; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public LocalDate getProductionDate() { return productionDate; }
    public void setProductionDate(LocalDate productionDate) { this.productionDate = productionDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public Integer getQualityInspectionResult() { return qualityInspectionResult; }
    public void setQualityInspectionResult(Integer qualityInspectionResult) { this.qualityInspectionResult = qualityInspectionResult; }
    public String getCertificateNo() { return certificateNo; }
    public void setCertificateNo(String certificateNo) { this.certificateNo = certificateNo; }
    public Integer getCertificateType() { return certificateType; }
    public void setCertificateType(Integer certificateType) { this.certificateType = certificateType; }
    public String getCertificateImageUrl() { return certificateImageUrl; }
    public void setCertificateImageUrl(String certificateImageUrl) { this.certificateImageUrl = certificateImageUrl; }
    public String getStorageLocation() { return storageLocation; }
    public void setStorageLocation(String storageLocation) { this.storageLocation = storageLocation; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    /** 计算金额（单价*数量） */
    public BigDecimal getAmount() {
        if (quantity != null && unitPrice != null) {
            return quantity.multiply(unitPrice);
        }
        return null;
    }
}
