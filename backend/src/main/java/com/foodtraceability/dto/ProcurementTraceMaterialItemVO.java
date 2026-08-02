package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 采购溯源物料明细视图对象
 * 展示单个物料在采购全链路中的流转信息
 */
@Schema(description = "采购溯源物料明细")
public class ProcurementTraceMaterialItemVO {

    /**
     * 物料ID
     */
    @Schema(description = "物料ID")
    private Long materialId;

    /**
     * 物料编码
     */
    @Schema(description = "物料编码")
    private String materialCode;

    /**
     * 物料名称
     */
    @Schema(description = "物料名称")
    private String materialName;

    /**
     * 规格
     */
    @Schema(description = "规格")
    private String specification;

    /**
     * 单位
     */
    @Schema(description = "单位")
    private String unit;

    /**
     * 采购申请数量
     */
    @Schema(description = "采购申请数量")
    private BigDecimal requestQuantity;

    /**
     * 采购订单数量
     */
    @Schema(description = "采购订单数量")
    private BigDecimal orderQuantity;

    /**
     * 已入库数量
     */
    @Schema(description = "已入库数量")
    private BigDecimal stockinQuantity;

    /**
     * 单价（元）
     */
    @Schema(description = "单价（元）")
    private BigDecimal unitPrice;

    /**
     * 批次号
     */
    @Schema(description = "批次号")
    private String batchNo;

    /**
     * 生产日期
     */
    @Schema(description = "生产日期")
    private LocalDate productionDate;

    /**
     * 有效期至
     */
    @Schema(description = "有效期至")
    private LocalDate expiryDate;

    /**
     * 当前库存数量
     */
    @Schema(description = "当前库存数量")
    private BigDecimal currentStock;

    /**
     * 仓库ID
     */
    @Schema(description = "仓库ID")
    private Long warehouseId;

    public ProcurementTraceMaterialItemVO() {
    }

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public String getMaterialCode() {
        return materialCode;
    }

    public void setMaterialCode(String materialCode) {
        this.materialCode = materialCode;
    }

    public String getMaterialName() {
        return materialName;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
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

    public BigDecimal getRequestQuantity() {
        return requestQuantity;
    }

    public void setRequestQuantity(BigDecimal requestQuantity) {
        this.requestQuantity = requestQuantity;
    }

    public BigDecimal getOrderQuantity() {
        return orderQuantity;
    }

    public void setOrderQuantity(BigDecimal orderQuantity) {
        this.orderQuantity = orderQuantity;
    }

    public BigDecimal getStockinQuantity() {
        return stockinQuantity;
    }

    public void setStockinQuantity(BigDecimal stockinQuantity) {
        this.stockinQuantity = stockinQuantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
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

    public BigDecimal getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(BigDecimal currentStock) {
        this.currentStock = currentStock;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }
}
