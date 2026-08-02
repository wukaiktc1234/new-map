package com.foodtraceability.dto;

/**
 * 扫码出库请求DTO
 */
public class TraceCodeScanRequest {
    private String code;
    private String operator;
    private Long operatorId;
    private String location;
    private String purpose;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
}
