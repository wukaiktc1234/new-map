package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 追溯码操作日志实体类
 * 用于记录扫码的历史操作和追溯码状态变更
 */
@TableName("trace_code_log")
@Schema(description = "追溯码操作日志实体")
public class TraceCodeLog {
    /**
     * 日志ID，主键
     */
    @TableId(value = "trace_code_log_id", type = IdType.ASSIGN_ID)
    @Schema(description = "日志ID", example = "TCL20251205001")
    private String traceCodeLogId;
    /**
     * 追溯码ID
     */
    @TableField("trace_code_id")
    @Schema(description = "追溯码ID", example = "TC20251205001")
    private String traceCodeId;
    /**
     * 追溯码
     */
    @TableField("code")
    @Schema(description = "追溯码", example = "TC20251205001ABC")
    private String code;
    /**
     * 操作类型（create-创建，inbound-入库，outbound-出库，return-退回，expire-过期）
     */
    @TableField("operation_type")
    @Schema(description = "操作类型", example = "outbound")
    private String operationType;
    /**
     * 操作前状态
     */
    @TableField("before_status")
    @Schema(description = "操作前状态", example = "created")
    private String beforeStatus;
    /**
     * 操作后状态
     */
    @TableField("after_status")
    @Schema(description = "操作后状态", example = "used")
    private String afterStatus;
    /**
     * 操作人
     */
    @TableField("operator")
    @Schema(description = "操作人", example = "张三")
    private String operator;
    /**
     * 操作人ID
     */
    @TableField("operator_id")
    @Schema(description = "操作人ID", example = "1")
    private Long operatorId;
    /**
     * 操作时间
     */
    @TableField("operation_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "操作时间")
    private LocalDateTime operationTime;
    /**
     * 操作地点
     */
    @TableField("operation_location")
    @Schema(description = "操作地点", example = "仓库A")
    private String operationLocation;
    /**
     * 操作设备
     */
    @TableField("operation_device")
    @Schema(description = "操作设备", example = "扫码枪")
    private String operationDevice;
    /**
     * 操作描述
     */
    @TableField("operation_description")
    @Schema(description = "操作描述", example = "扫描追溯码出库使用")
    private String operationDescription;
    /**
     * 备注
     */
    @TableField("remark")
    @Schema(description = "备注")
    private String remark;
    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    /**
     * 逻辑删除标记（0-正常，1-删除）
     */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "删除标记", example = "0")
    private Integer deleted;

    // Getter and Setter methods
    public String getTraceCodeLogId() {
        return traceCodeLogId;
    }

    public void setTraceCodeLogId(String traceCodeLogId) {
        this.traceCodeLogId = traceCodeLogId;
    }

    public String getTraceCodeId() {
        return traceCodeId;
    }

    public void setTraceCodeId(String traceCodeId) {
        this.traceCodeId = traceCodeId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getBeforeStatus() {
        return beforeStatus;
    }

    public void setBeforeStatus(String beforeStatus) {
        this.beforeStatus = beforeStatus;
    }

    public String getAfterStatus() {
        return afterStatus;
    }

    public void setAfterStatus(String afterStatus) {
        this.afterStatus = afterStatus;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public LocalDateTime getOperationTime() {
        return operationTime;
    }

    public void setOperationTime(LocalDateTime operationTime) {
        this.operationTime = operationTime;
    }

    public String getOperationLocation() {
        return operationLocation;
    }

    public void setOperationLocation(String operationLocation) {
        this.operationLocation = operationLocation;
    }

    public String getOperationDevice() {
        return operationDevice;
    }

    public void setOperationDevice(String operationDevice) {
        this.operationDevice = operationDevice;
    }

    public String getOperationDescription() {
        return operationDescription;
    }

    public void setOperationDescription(String operationDescription) {
        this.operationDescription = operationDescription;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public TraceCodeLog() {
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof TraceCodeLog)) return false;
        final TraceCodeLog other = (TraceCodeLog) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$operatorId = this.getOperatorId();
        final java.lang.Object other$operatorId = other.getOperatorId();
        if (this$operatorId == null ? other$operatorId != null : !this$operatorId.equals(other$operatorId)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$traceCodeLogId = this.getTraceCodeLogId();
        final java.lang.Object other$traceCodeLogId = other.getTraceCodeLogId();
        if (this$traceCodeLogId == null ? other$traceCodeLogId != null : !this$traceCodeLogId.equals(other$traceCodeLogId)) return false;
        final java.lang.Object this$traceCodeId = this.getTraceCodeId();
        final java.lang.Object other$traceCodeId = other.getTraceCodeId();
        if (this$traceCodeId == null ? other$traceCodeId != null : !this$traceCodeId.equals(other$traceCodeId)) return false;
        final java.lang.Object this$code = this.getCode();
        final java.lang.Object other$code = other.getCode();
        if (this$code == null ? other$code != null : !this$code.equals(other$code)) return false;
        final java.lang.Object this$operationType = this.getOperationType();
        final java.lang.Object other$operationType = other.getOperationType();
        if (this$operationType == null ? other$operationType != null : !this$operationType.equals(other$operationType)) return false;
        final java.lang.Object this$beforeStatus = this.getBeforeStatus();
        final java.lang.Object other$beforeStatus = other.getBeforeStatus();
        if (this$beforeStatus == null ? other$beforeStatus != null : !this$beforeStatus.equals(other$beforeStatus)) return false;
        final java.lang.Object this$afterStatus = this.getAfterStatus();
        final java.lang.Object other$afterStatus = other.getAfterStatus();
        if (this$afterStatus == null ? other$afterStatus != null : !this$afterStatus.equals(other$afterStatus)) return false;
        final java.lang.Object this$operator = this.getOperator();
        final java.lang.Object other$operator = other.getOperator();
        if (this$operator == null ? other$operator != null : !this$operator.equals(other$operator)) return false;
        final java.lang.Object this$operationTime = this.getOperationTime();
        final java.lang.Object other$operationTime = other.getOperationTime();
        if (this$operationTime == null ? other$operationTime != null : !this$operationTime.equals(other$operationTime)) return false;
        final java.lang.Object this$operationLocation = this.getOperationLocation();
        final java.lang.Object other$operationLocation = other.getOperationLocation();
        if (this$operationLocation == null ? other$operationLocation != null : !this$operationLocation.equals(other$operationLocation)) return false;
        final java.lang.Object this$operationDevice = this.getOperationDevice();
        final java.lang.Object other$operationDevice = other.getOperationDevice();
        if (this$operationDevice == null ? other$operationDevice != null : !this$operationDevice.equals(other$operationDevice)) return false;
        final java.lang.Object this$operationDescription = this.getOperationDescription();
        final java.lang.Object other$operationDescription = other.getOperationDescription();
        if (this$operationDescription == null ? other$operationDescription != null : !this$operationDescription.equals(other$operationDescription)) return false;
        final java.lang.Object this$remark = this.getRemark();
        final java.lang.Object other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) return false;
        final java.lang.Object this$createTime = this.getCreateTime();
        final java.lang.Object other$createTime = other.getCreateTime();
        if (this$createTime == null ? other$createTime != null : !this$createTime.equals(other$createTime)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof TraceCodeLog;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $operatorId = this.getOperatorId();
        result = result * PRIME + ($operatorId == null ? 43 : $operatorId.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $traceCodeLogId = this.getTraceCodeLogId();
        result = result * PRIME + ($traceCodeLogId == null ? 43 : $traceCodeLogId.hashCode());
        final java.lang.Object $traceCodeId = this.getTraceCodeId();
        result = result * PRIME + ($traceCodeId == null ? 43 : $traceCodeId.hashCode());
        final java.lang.Object $code = this.getCode();
        result = result * PRIME + ($code == null ? 43 : $code.hashCode());
        final java.lang.Object $operationType = this.getOperationType();
        result = result * PRIME + ($operationType == null ? 43 : $operationType.hashCode());
        final java.lang.Object $beforeStatus = this.getBeforeStatus();
        result = result * PRIME + ($beforeStatus == null ? 43 : $beforeStatus.hashCode());
        final java.lang.Object $afterStatus = this.getAfterStatus();
        result = result * PRIME + ($afterStatus == null ? 43 : $afterStatus.hashCode());
        final java.lang.Object $operator = this.getOperator();
        result = result * PRIME + ($operator == null ? 43 : $operator.hashCode());
        final java.lang.Object $operationTime = this.getOperationTime();
        result = result * PRIME + ($operationTime == null ? 43 : $operationTime.hashCode());
        final java.lang.Object $operationLocation = this.getOperationLocation();
        result = result * PRIME + ($operationLocation == null ? 43 : $operationLocation.hashCode());
        final java.lang.Object $operationDevice = this.getOperationDevice();
        result = result * PRIME + ($operationDevice == null ? 43 : $operationDevice.hashCode());
        final java.lang.Object $operationDescription = this.getOperationDescription();
        result = result * PRIME + ($operationDescription == null ? 43 : $operationDescription.hashCode());
        final java.lang.Object $remark = this.getRemark();
        result = result * PRIME + ($remark == null ? 43 : $remark.hashCode());
        final java.lang.Object $createTime = this.getCreateTime();
        result = result * PRIME + ($createTime == null ? 43 : $createTime.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "TraceCodeLog(traceCodeLogId=" + this.getTraceCodeLogId() + ", traceCodeId=" + this.getTraceCodeId() + ", code=" + this.getCode() + ", operationType=" + this.getOperationType() + ", beforeStatus=" + this.getBeforeStatus() + ", afterStatus=" + this.getAfterStatus() + ", operator=" + this.getOperator() + ", operatorId=" + this.getOperatorId() + ", operationTime=" + this.getOperationTime() + ", operationLocation=" + this.getOperationLocation() + ", operationDevice=" + this.getOperationDevice() + ", operationDescription=" + this.getOperationDescription() + ", remark=" + this.getRemark() + ", createTime=" + this.getCreateTime() + ", deleted=" + this.getDeleted() + ")";
    }
}
