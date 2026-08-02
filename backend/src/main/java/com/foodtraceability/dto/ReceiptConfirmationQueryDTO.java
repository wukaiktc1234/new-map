package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 收货确认单查询条件
 */
@Schema(description = "收货确认单查询条件")
public class ReceiptConfirmationQueryDTO {

    @Schema(description = "收货确认单编号（模糊查询）")
    private String confirmationCode;

    @Schema(description = "关联到货单ID")
    private Long arrivalId;

    @Schema(description = "关联采购订单ID")
    private Long orderId;

    @Schema(description = "收货方类型（STORE-门店, WAREHOUSE-仓库）")
    private String receiverType;

    @Schema(description = "收货门店ID")
    private String storeId;

    @Schema(description = "收货仓库ID")
    private Long warehouseId;

    @Schema(description = "当前页码", example = "1")
    private Integer current = 1;

    @Schema(description = "每页条数", example = "10")
    private Integer size = 10;

    public String getConfirmationCode() {
        return confirmationCode;
    }

    public void setConfirmationCode(String confirmationCode) {
        this.confirmationCode = confirmationCode;
    }

    public Long getArrivalId() {
        return arrivalId;
    }

    public void setArrivalId(Long arrivalId) {
        this.arrivalId = arrivalId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
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
