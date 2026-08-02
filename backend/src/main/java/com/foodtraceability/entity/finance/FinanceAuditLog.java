package com.foodtraceability.entity.finance;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 财务审计日志实体类
 * 用于记录财务模块的操作审计日志，仅允许INSERT，禁止UPDATE和DELETE
 * 不继承BaseEntity，不进行逻辑删除，仅保留创建时间字段
 */
@TableName("finance_audit_logs")
public class FinanceAuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 日志ID */
    @TableId(type = IdType.AUTO)
    private Long logId;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人姓名 */
    private String operatorName;

    /** 操作类型 */
    private String operationType;

    /** 模块 */
    private String module;

    /** 目标ID */
    private Long targetId;

    /** 目标类型 */
    private String targetType;

    /** 操作描述 */
    private String operationDesc;

    /** 操作数据（JSON） */
    private String operationData;

    /** IP地址 */
    private String ipAddress;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getOperationDesc() {
        return operationDesc;
    }

    public void setOperationDesc(String operationDesc) {
        this.operationDesc = operationDesc;
    }

    public String getOperationData() {
        return operationData;
    }

    public void setOperationData(String operationData) {
        this.operationData = operationData;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "FinanceAuditLog{" +
                "logId=" + logId +
                ", operatorId=" + operatorId +
                ", operatorName='" + operatorName + '\'' +
                ", operationType='" + operationType + '\'' +
                ", module='" + module + '\'' +
                ", targetId=" + targetId +
                ", targetType='" + targetType + '\'' +
                ", operationDesc='" + operationDesc + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", createTime=" + createTime +
                '}';
    }
}
