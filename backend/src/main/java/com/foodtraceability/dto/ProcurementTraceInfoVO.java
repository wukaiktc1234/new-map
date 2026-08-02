package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 采购溯源信息视图对象
 * 聚合采购申请、订单、入库、库存、资产等全链路信息
 */
@Schema(description = "采购溯源信息")
public class ProcurementTraceInfoVO {

    /**
     * 溯源批次号（取最新入库批次号）
     */
    @Schema(description = "溯源批次号")
    private String traceBatchNo;

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
     * 采购申请ID
     */
    @Schema(description = "采购申请ID")
    private String requestId;

    /**
     * 采购申请编号
     */
    @Schema(description = "采购申请编号")
    private String requestNo;

    /**
     * 采购申请标题
     */
    @Schema(description = "采购申请标题")
    private String requestTitle;

    /**
     * 申请人
     */
    @Schema(description = "申请人")
    private String applicantName;

    /**
     * 申请时间
     */
    @Schema(description = "申请时间")
    private LocalDateTime requestTime;

    /**
     * 采购订单ID
     */
    @Schema(description = "采购订单ID")
    private Long orderId;

    /**
     * 采购订单编号
     */
    @Schema(description = "采购订单编号")
    private String orderNo;

    /**
     * 订单金额（元）
     */
    @Schema(description = "订单金额（元）")
    private BigDecimal orderAmount;

    /**
     * 下单日期
     */
    @Schema(description = "下单日期")
    private LocalDate orderDate;

    /**
     * 订单状态
     */
    @Schema(description = "订单状态")
    private String orderStatus;

    /**
     * 订单状态描述
     */
    @Schema(description = "订单状态描述")
    private String orderStatusText;

    /**
     * 入库单号列表
     */
    @Schema(description = "入库单号列表")
    private List<String> stockinCodes = new ArrayList<>();

    /**
     * 入库时间
     */
    @Schema(description = "入库时间")
    private LocalDateTime stockinTime;

    /**
     * 入库总数量
     */
    @Schema(description = "入库总数量")
    private BigDecimal totalStockinQuantity;

    /**
     * 质检结果：1合格 2不合格 3待检
     */
    @Schema(description = "质检结果")
    private Integer qualityCheckResult;

    /**
     * 质检结果描述
     */
    @Schema(description = "质检结果描述")
    private String qualityCheckResultText;

    /**
     * 仓库ID
     */
    @Schema(description = "仓库ID")
    private Long warehouseId;

    /**
     * 仓库名称
     */
    @Schema(description = "仓库名称")
    private String warehouseName;

    /**
     * 物料明细列表
     */
    @Schema(description = "物料明细列表")
    private List<ProcurementTraceMaterialItemVO> materialItems = new ArrayList<>();

    /**
     * 资产列表
     */
    @Schema(description = "资产列表")
    private List<ProcurementTraceAssetVO> assets = new ArrayList<>();

    /**
     * 溯源链路节点
     */
    @Schema(description = "溯源链路节点")
    private List<ProcurementTraceNodeVO> traceNodes = new ArrayList<>();

    public ProcurementTraceInfoVO() {
    }

    public String getTraceBatchNo() {
        return traceBatchNo;
    }

    public void setTraceBatchNo(String traceBatchNo) {
        this.traceBatchNo = traceBatchNo;
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

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getRequestNo() {
        return requestNo;
    }

    public void setRequestNo(String requestNo) {
        this.requestNo = requestNo;
    }

    public String getRequestTitle() {
        return requestTitle;
    }

    public void setRequestTitle(String requestTitle) {
        this.requestTitle = requestTitle;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public LocalDateTime getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(LocalDateTime requestTime) {
        this.requestTime = requestTime;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public BigDecimal getOrderAmount() {
        return orderAmount;
    }

    public void setOrderAmount(BigDecimal orderAmount) {
        this.orderAmount = orderAmount;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getOrderStatusText() {
        return orderStatusText;
    }

    public void setOrderStatusText(String orderStatusText) {
        this.orderStatusText = orderStatusText;
    }

    public List<String> getStockinCodes() {
        return stockinCodes;
    }

    public void setStockinCodes(List<String> stockinCodes) {
        this.stockinCodes = stockinCodes;
    }

    public LocalDateTime getStockinTime() {
        return stockinTime;
    }

    public void setStockinTime(LocalDateTime stockinTime) {
        this.stockinTime = stockinTime;
    }

    public BigDecimal getTotalStockinQuantity() {
        return totalStockinQuantity;
    }

    public void setTotalStockinQuantity(BigDecimal totalStockinQuantity) {
        this.totalStockinQuantity = totalStockinQuantity;
    }

    public Integer getQualityCheckResult() {
        return qualityCheckResult;
    }

    public void setQualityCheckResult(Integer qualityCheckResult) {
        this.qualityCheckResult = qualityCheckResult;
    }

    public String getQualityCheckResultText() {
        return qualityCheckResultText;
    }

    public void setQualityCheckResultText(String qualityCheckResultText) {
        this.qualityCheckResultText = qualityCheckResultText;
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

    public List<ProcurementTraceMaterialItemVO> getMaterialItems() {
        return materialItems;
    }

    public void setMaterialItems(List<ProcurementTraceMaterialItemVO> materialItems) {
        this.materialItems = materialItems;
    }

    public List<ProcurementTraceAssetVO> getAssets() {
        return assets;
    }

    public void setAssets(List<ProcurementTraceAssetVO> assets) {
        this.assets = assets;
    }

    public List<ProcurementTraceNodeVO> getTraceNodes() {
        return traceNodes;
    }

    public void setTraceNodes(List<ProcurementTraceNodeVO> traceNodes) {
        this.traceNodes = traceNodes;
    }
}
