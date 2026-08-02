package com.foodtraceability.entity.finance;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;

/**
 * 财务收支记录操作日志实体类
 * @author example
 * @since 2025-12-07
 */
@TableName("finance_record_operation_log")
public class FinanceRecordOperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 财务记录ID
     */
    @TableField("record_id")
    private Long recordId;

    /**
     * 操作类型：新增(ADD)、编辑(EDIT)、删除(DELETE)
     */
    @TableField("operation_type")
    private String operationType;

    /**
     * 操作内容
     */
    @TableField("operation_content")
    private String operationContent;

    /**
     * 操作人ID
     */
    @TableField("operator_id")
    private Long operatorId;

    /**
     * 操作人名称
     */
    @TableField("operator_name")
    private String operatorName;

    /**
     * 操作时间
     */
    @TableField("operation_time")
    private LocalDateTime operationTime;

    /**
     * IP地址
     */
    @TableField("ip_address")
    private String ipAddress;

    /**
     * 浏览器信息
     */
    @TableField("browser_info")
    private String browserInfo;

    // getter and setter methods
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getOperationContent() {
        return operationContent;
    }

    public void setOperationContent(String operationContent) {
        this.operationContent = operationContent;
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

    public LocalDateTime getOperationTime() {
        return operationTime;
    }

    public void setOperationTime(LocalDateTime operationTime) {
        this.operationTime = operationTime;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getBrowserInfo() {
        return browserInfo;
    }

    public void setBrowserInfo(String browserInfo) {
        this.browserInfo = browserInfo;
    }

    @Override
    public String toString() {
        return "FinanceRecordOperationLog{" +
            "id=" + id +
            ", recordId=" + recordId +
            ", operationType='" + operationType + '\'' +
            ", operationContent='" + operationContent + '\'' +
            ", operatorId=" + operatorId +
            ", operatorName='" + operatorName + '\'' +
            ", operationTime=" + operationTime +
            ", ipAddress='" + ipAddress + '\'' +
            ", browserInfo='" + browserInfo + '\'' +
            '}';
    }
}
