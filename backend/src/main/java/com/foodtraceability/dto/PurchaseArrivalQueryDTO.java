package com.foodtraceability.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * 采购到货单查询条件
 */
@Schema(description = "采购到货单查询条件")
public class PurchaseArrivalQueryDTO {

    @Schema(description = "到货单编号（模糊查询）")
    private String arrivalCode;

    @Schema(description = "采购订单编号（模糊查询）")
    private String orderCode;

    @Schema(description = "供应商ID")
    private Long supplierId;

    @Schema(description = "到货单状态（0-待收货, 1-收货中, 2-部分收货, 3-已收货, 4-已关闭）")
    private Integer status;

    @Schema(description = "收货方类型（STORE-门店, WAREHOUSE-仓库）")
    private String receiverType;

    @Schema(description = "收货门店ID")
    private String storeId;

    @Schema(description = "收货仓库ID")
    private Long warehouseId;

    @Schema(description = "仅看超期（true-是）")
    private Boolean overdueOnly;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "预计到货开始日期")
    private LocalDate estimatedStartDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "预计到货结束日期")
    private LocalDate estimatedEndDate;

    @Schema(description = "创建人ID（我的单据筛选）")
    private Long createUserId;

    @Schema(description = "当前页码", example = "1")
    private Integer current = 1;

    @Schema(description = "每页条数", example = "10")
    private Integer size = 10;

    public String getArrivalCode() {
        return arrivalCode;
    }

    public void setArrivalCode(String arrivalCode) {
        this.arrivalCode = arrivalCode;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getReceiverType() {
        return receiverType;
    }

    public void setReceiverType(String receiverType) {
        this.receiverType = receiverType;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Boolean getOverdueOnly() {
        return overdueOnly;
    }

    public void setOverdueOnly(Boolean overdueOnly) {
        this.overdueOnly = overdueOnly;
    }

    public LocalDate getEstimatedStartDate() {
        return estimatedStartDate;
    }

    public void setEstimatedStartDate(LocalDate estimatedStartDate) {
        this.estimatedStartDate = estimatedStartDate;
    }

    public LocalDate getEstimatedEndDate() {
        return estimatedEndDate;
    }

    public void setEstimatedEndDate(LocalDate estimatedEndDate) {
        this.estimatedEndDate = estimatedEndDate;
    }

    public Long getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(Long createUserId) {
        this.createUserId = createUserId;
    }

    public Integer getCurrent() {
        return current;
    }

    public void setCurrent(Integer current) {
        this.current = current;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
