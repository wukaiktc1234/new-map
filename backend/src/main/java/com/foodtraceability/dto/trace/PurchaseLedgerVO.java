package com.foodtraceability.dto.trace;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 进货台账VO
 */
@Schema(description = "进货台账信息")
public class PurchaseLedgerVO {

    /** 主键ID */
    @Schema(description = "主键ID")
    private Long ledgerId;

    /** 台账编号 */
    @Schema(description = "台账编号")
    private String ledgerNo;

    /** 采购入库单ID */
    @Schema(description = "采购入库单ID")
    private Long purchaseStockinId;

    /** 供应商ID */
    @Schema(description = "供应商ID")
    private Long supplierId;

    /** 供应商名称 */
    @Schema(description = "供应商名称")
    private String supplierName;

    /** 物料名称 */
    @Schema(description = "物料名称")
    private String materialName;

    /** 规格型号 */
    @Schema(description = "规格型号")
    private String specification;

    /** 单位 */
    @Schema(description = "单位")
    private String unit;

    /** 数量 */
    @Schema(description = "数量")
    private BigDecimal quantity;

    /** 单价 */
    @Schema(description = "单价")
    private BigDecimal unitPrice;

    /** 金额 */
    @Schema(description = "金额")
    private BigDecimal amount;

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

    /** 质检结果 */
    @Schema(description = "质检结果")
    private Integer qualityInspectionResult;

    /** 质检结果名称 */
    @Schema(description = "质检结果名称")
    private String qualityInspectionResultName;

    /** 质检时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "质检时间")
    private LocalDateTime inspectionTime;

    /** 证件号码 */
    @Schema(description = "证件号码")
    private String certificateNo;

    /** 证件类型 */
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

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    // Getter和Setter方法

    public Long getLedgerId() { return ledgerId; }
    public void setLedgerId(Long ledgerId) { this.ledgerId = ledgerId; }
    public String getLedgerNo() { return ledgerNo; }
    public void setLedgerNo(String ledgerNo) { this.ledgerNo = ledgerNo; }
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
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public LocalDate getProductionDate() { return productionDate; }
    public void setProductionDate(LocalDate productionDate) { this.productionDate = productionDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public Integer getQualityInspectionResult() { return qualityInspectionResult; }
    public void setQualityInspectionResult(Integer qualityInspectionResult) { this.qualityInspectionResult = qualityInspectionResult; }
    public String getQualityInspectionResultName() { return qualityInspectionResultName; }
    public void setQualityInspectionResultName(String qualityInspectionResultName) { this.qualityInspectionResultName = qualityInspectionResultName; }
    public LocalDateTime getInspectionTime() { return inspectionTime; }
    public void setInspectionTime(LocalDateTime inspectionTime) { this.inspectionTime = inspectionTime; }
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
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
