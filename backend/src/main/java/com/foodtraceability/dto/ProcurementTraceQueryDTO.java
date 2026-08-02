package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 采购溯源查询条件
 */
@Schema(description = "采购溯源查询条件")
public class ProcurementTraceQueryDTO {

    /**
     * 物料ID
     */
    @Schema(description = "物料ID")
    private Long materialId;

    /**
     * 采购订单编号
     */
    @Schema(description = "采购订单编号")
    private String orderNo;

    /**
     * 采购申请编号
     */
    @Schema(description = "采购申请编号")
    private String requestNo;

    /**
     * 入库单号
     */
    @Schema(description = "入库单号")
    private String stockinCode;

    /**
     * 批次号
     */
    @Schema(description = "批次号")
    private String batchNo;

    public ProcurementTraceQueryDTO() {
    }

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getRequestNo() {
        return requestNo;
    }

    public void setRequestNo(String requestNo) {
        this.requestNo = requestNo;
    }

    public String getStockinCode() {
        return stockinCode;
    }

    public void setStockinCode(String stockinCode) {
        this.stockinCode = stockinCode;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }
}
