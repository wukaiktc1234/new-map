package com.foodtraceability.dto;

/**
 * 未拆封退回请求DTO
 */
public class TraceCodeReturnRequest {
    private String code;
    private String operator;
    private Long operatorId;
    private String location;
    private String reason;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
