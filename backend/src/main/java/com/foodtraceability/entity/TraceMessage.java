package com.foodtraceability.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 食品溯源消息实体类
 * 用于在消息队列中传递溯源相关信息
 */
public class TraceMessage implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 消息ID
     */
    private String messageId;
    
    /**
     * 消息类型
     * trace.create - 创建溯源记录
     * trace.update - 更新溯源记录
     * trace.complete - 完成溯源阶段
     * trace.quality - 质检结果通知
     * trace.warning - 异常预警
     */
    private String messageType;
    
    /**
     * 溯源ID
     */
    private String traceId;
    
    /**
     * 食品ID
     */
    private String foodId;
    
    /**
     * 食品名称
     */
    private String foodName;
    
    /**
     * 批次号
     */
    private String batchNumber;
    
    /**
     * 流程阶段
     * 1-生产，2-加工，3-质检，4-包装，5-运输，6-仓储，7-销售
     */
    private Integer processStage;
    
    /**
     * 流程阶段名称
     */
    private String stageName;
    
    /**
     * 操作类型
     */
    private String operationType;
    
    /**
     * 操作描述
     */
    private String operationDescription;
    
    /**
     * 操作时间
     */
    private LocalDateTime operationTime;
    
    /**
     * 操作人
     */
    private String operator;
    
    /**
     * 质检结果
     * pass-合格，fail-不合格，pending-待检
     */
    private String qualityResult;
    
    /**
     * 消息创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 扩展数据，JSON格式
     */
    private String extData;

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getFoodId() {
        return foodId;
    }

    public void setFoodId(String foodId) {
        this.foodId = foodId;
    }

    public String getFoodName() {
        return foodName;
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public Integer getProcessStage() {
        return processStage;
    }

    public void setProcessStage(Integer processStage) {
        this.processStage = processStage;
    }

    public String getStageName() {
        return stageName;
    }

    public void setStageName(String stageName) {
        this.stageName = stageName;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getOperationDescription() {
        return operationDescription;
    }

    public void setOperationDescription(String operationDescription) {
        this.operationDescription = operationDescription;
    }

    public LocalDateTime getOperationTime() {
        return operationTime;
    }

    public void setOperationTime(LocalDateTime operationTime) {
        this.operationTime = operationTime;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getQualityResult() {
        return qualityResult;
    }

    public void setQualityResult(String qualityResult) {
        this.qualityResult = qualityResult;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getExtData() {
        return extData;
    }

    public void setExtData(String extData) {
        this.extData = extData;
    }
}
