package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 进货台账实体类
 * 电子化进货台账，符合《食品安全法》关于进货查验记录的要求
 */
@TableName("purchase_ledgers")
@Schema(description = "进货台账实体")
public class PurchaseLedger {

    /** 主键ID */
    @TableId(value = "ledger_id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long ledgerId;

    /** 台账编号（唯一） */
    @TableField("ledger_no")
    @Schema(description = "台账编号", example = "LD20260425001")
    private String ledgerNo;

    /** 关联采购入库单ID */
    @TableField("purchase_stockin_id")
    @Schema(description = "采购入库单ID")
    private Long purchaseStockinId;

    /** 供应商ID */
    @TableField("supplier_id")
    @Schema(description = "供应商ID")
    private Long supplierId;

    /** 供应商名称 */
    @TableField("supplier_name")
    @Schema(description = "供应商名称", example = "绿色农场")
    private String supplierName;

    /** 物料名称 */
    @TableField("material_name")
    @Schema(description = "物料名称", example = "土豆")
    private String materialName;

    /** 规格型号 */
    @TableField("specification")
    @Schema(description = "规格型号", example = "一级品")
    private String specification;

    /** 单位 */
    @TableField("unit")
    @Schema(description = "单位", example = "kg")
    private String unit;

    /** 数量 */
    @TableField("quantity")
    @Schema(description = "数量", example = "50.0000")
    private BigDecimal quantity;

    /** 单价 */
    @TableField("unit_price")
    @Schema(description = "单价", example = "3.50")
    private BigDecimal unitPrice;

    /** 金额 */
    @TableField("amount")
    @Schema(description = "金额", example = "175.00")
    private BigDecimal amount;

    /** 批次号 */
    @TableField("batch_no")
    @Schema(description = "批次号", example = "BATCH20260420")
    private String batchNo;

    /** 生产日期 */
    @TableField("production_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "生产日期")
    private LocalDate productionDate;

    /** 有效期至 */
    @TableField("expiry_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "有效期至")
    private LocalDate expiryDate;

    /** 质检结果：1合格 2不合格 3待检 */
    @TableField("quality_inspection_result")
    @Schema(description = "质检结果", example = "1")
    private Integer qualityInspectionResult;

    /** 质检人ID */
    @TableField("inspection_user_id")
    @Schema(description = "质检人ID")
    private Long inspectionUserId;

    /** 质检时间 */
    @TableField("inspection_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "质检时间")
    private LocalDateTime inspectionTime;

    /** 索证票号/检疫证号 */
    @TableField("certificate_no")
    @Schema(description = "证件号码")
    private String certificateNo;

    /** 证件类型：1检疫证 2合格证 3检测报告 4其他 */
    @TableField("certificate_type")
    @Schema(description = "证件类型", example = "1")
    private Integer certificateType;

    /** 证件图片URL */
    @TableField("certificate_image_url")
    @Schema(description = "证件图片URL")
    private String certificateImageUrl;

    /** 存放位置 */
    @TableField("storage_location")
    @Schema(description = "存放位置", example = "冷库A区-03-02")
    private String storageLocation;

    /** 备注 */
    @TableField("remark")
    @Schema(description = "备注")
    private String remark;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "删除标记")
    private Integer deleted;

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
    public Long getInspectionUserId() { return inspectionUserId; }
    public void setInspectionUserId(Long inspectionUserId) { this.inspectionUserId = inspectionUserId; }
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
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
