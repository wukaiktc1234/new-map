package com.foodtraceability.dto;

import java.util.Map;

/**
 * 设备打印请求参数DTO
 */
public class DevicePrintRequest {
    /** 打印类型：THERMAL_PAPER, FILE, INVOICE, TRACEABILITY_LABEL */
    private String printType;
    /** 打印内容 */
    private String content;
    /** 文件路径 */
    private String filePath;
    /** 发票数据 */
    private Map<String, Object> invoiceData;
    /** 溯源码 */
    private String traceabilityCode;
    /** 产品名称 */
    private String productName;
    /** 设备类型 */
    private String deviceType;

    public String getPrintType() {
        return printType;
    }

    public void setPrintType(String printType) {
        this.printType = printType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Map<String, Object> getInvoiceData() {
        return invoiceData;
    }

    public void setInvoiceData(Map<String, Object> invoiceData) {
        this.invoiceData = invoiceData;
    }

    public String getTraceabilityCode() {
        return traceabilityCode;
    }

    public void setTraceabilityCode(String traceabilityCode) {
        this.traceabilityCode = traceabilityCode;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }
}
