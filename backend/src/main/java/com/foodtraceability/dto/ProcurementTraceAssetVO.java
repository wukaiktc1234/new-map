package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 采购溯源资产视图对象
 * 展示由采购入库自动生成的资产卡片信息
 */
@Schema(description = "采购溯源资产信息")
public class ProcurementTraceAssetVO {

    /**
     * 资产ID
     */
    @Schema(description = "资产ID")
    private Long assetId;

    /**
     * 资产编码
     */
    @Schema(description = "资产编码")
    private String assetCode;

    /**
     * 资产名称
     */
    @Schema(description = "资产名称")
    private String assetName;

    /**
     * 规格
     */
    @Schema(description = "规格")
    private String specification;

    /**
     * 采购订单ID
     */
    @Schema(description = "采购订单ID")
    private Long purchaseOrderId;

    /**
     * 采购订单编号
     */
    @Schema(description = "采购订单编号")
    private String purchaseOrderNo;

    /**
     * 供应商ID
     */
    @Schema(description = "供应商ID")
    private Long supplierId;

    /**
     * 供应商名称
     */
    @Schema(description = "供应商名称")
    private String supplierName;

    /**
     * 采购日期
     */
    @Schema(description = "采购日期")
    private LocalDate purchaseDate;

    /**
     * 原值（元）
     */
    @Schema(description = "原值（元）")
    private BigDecimal originalValue;

    /**
     * 净值（元）
     */
    @Schema(description = "净值（元）")
    private BigDecimal netValue;

    /**
     * 使用状态
     */
    @Schema(description = "使用状态")
    private String status;

    /**
     * 折旧方法
     */
    @Schema(description = "折旧方法")
    private Integer depreciationMethod;

    /**
     * 使用年限（月）
     */
    @Schema(description = "使用年限（月）")
    private Integer usefulLifeMonths;

    public ProcurementTraceAssetVO() {
    }

    public Long getAssetId() {
        return assetId;
    }

    public void setAssetId(Long assetId) {
        this.assetId = assetId;
    }

    public String getAssetCode() {
        return assetCode;
    }

    public void setAssetCode(String assetCode) {
        this.assetCode = assetCode;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public String getSpecification() {
        return specification;
    }

    public void setSpecification(String specification) {
        this.specification = specification;
    }

    public Long getPurchaseOrderId() {
        return purchaseOrderId;
    }

    public void setPurchaseOrderId(Long purchaseOrderId) {
        this.purchaseOrderId = purchaseOrderId;
    }

    public String getPurchaseOrderNo() {
        return purchaseOrderNo;
    }

    public void setPurchaseOrderNo(String purchaseOrderNo) {
        this.purchaseOrderNo = purchaseOrderNo;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public BigDecimal getOriginalValue() {
        return originalValue;
    }

    public void setOriginalValue(BigDecimal originalValue) {
        this.originalValue = originalValue;
    }

    public BigDecimal getNetValue() {
        return netValue;
    }

    public void setNetValue(BigDecimal netValue) {
        this.netValue = netValue;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getDepreciationMethod() {
        return depreciationMethod;
    }

    public void setDepreciationMethod(Integer depreciationMethod) {
        this.depreciationMethod = depreciationMethod;
    }

    public Integer getUsefulLifeMonths() {
        return usefulLifeMonths;
    }

    public void setUsefulLifeMonths(Integer usefulLifeMonths) {
        this.usefulLifeMonths = usefulLifeMonths;
    }
}
