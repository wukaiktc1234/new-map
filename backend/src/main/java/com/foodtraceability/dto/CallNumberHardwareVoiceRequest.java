package com.foodtraceability.dto;

/**
 * 硬件语音播报请求DTO
 */
public class CallNumberHardwareVoiceRequest {
    private String orderNumber;
    private String orderType;

    public CallNumberHardwareVoiceRequest() {}

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }
}
