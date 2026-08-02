package com.foodtraceability.dto;

/**
 * LED显示屏请求DTO
 */
public class CallNumberHardwareDisplayRequest {
    private String orderNumber;
    private String orderType;
    private String tableNumber;

    public CallNumberHardwareDisplayRequest() {}

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }
    public String getTableNumber() { return tableNumber; }
    public void setTableNumber(String tableNumber) { this.tableNumber = tableNumber; }
}
