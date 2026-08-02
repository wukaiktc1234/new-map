package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 硬件设备操作日志实体类
 */
@TableName("hardware_operation_log")
@Schema(description = "硬件设备操作日志")
public class HardwareOperationLog implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "日志ID")
    private String id;
    @TableField("hardware_id")
    @Schema(description = "设备配置ID")
    private Long hardwareId;
    @TableField("device_type")
    @Schema(description = "设备类型")
    private String deviceType;
    @TableField("device_name")
    @Schema(description = "设备名称")
    private String deviceName;
    @TableField("store_id")
    @Schema(description = "门店ID")
    private Long storeId;
    @TableField("operation_type")
    @Schema(description = "操作类型（CREATE-创建，UPDATE-更新，DELETE-删除，TEST-测试）")
    private String operationType;
    @TableField("operation_desc")
    @Schema(description = "操作描述")
    private String operationDesc;
    @TableField("old_data")
    @Schema(description = "操作前的数据（JSON格式）")
    private String oldData;
    @TableField("new_data")
    @Schema(description = "操作后的数据（JSON格式）")
    private String newData;
    @TableField("operator_id")
    @Schema(description = "操作人ID")
    private String operatorId;
    @TableField("operator_name")
    @Schema(description = "操作人名称")
    private String operatorName;
    @TableField(value = "operation_time", fill = FieldFill.INSERT)
    @Schema(description = "操作时间")
    private LocalDateTime operationTime;
    @TableField("ip_address")
    @Schema(description = "操作IP地址")
    private String ipAddress;
    @TableField("user_agent")
    @Schema(description = "用户代理信息")
    private String userAgent;

    // Getter and Setter methods
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getHardwareId() {
        return hardwareId;
    }

    public void setHardwareId(Long hardwareId) {
        this.hardwareId = hardwareId;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getOperationDesc() {
        return operationDesc;
    }

    public void setOperationDesc(String operationDesc) {
        this.operationDesc = operationDesc;
    }

    public String getOldData() {
        return oldData;
    }

    public void setOldData(String oldData) {
        this.oldData = oldData;
    }

    public String getNewData() {
        return newData;
    }

    public void setNewData(String newData) {
        this.newData = newData;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
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

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof HardwareOperationLog)) return false;
        final HardwareOperationLog other = (HardwareOperationLog) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$hardwareId = this.getHardwareId();
        final java.lang.Object other$hardwareId = other.getHardwareId();
        if (this$hardwareId == null ? other$hardwareId != null : !this$hardwareId.equals(other$hardwareId)) return false;
        final java.lang.Object this$storeId = this.getStoreId();
        final java.lang.Object other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !this$storeId.equals(other$storeId)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$deviceType = this.getDeviceType();
        final java.lang.Object other$deviceType = other.getDeviceType();
        if (this$deviceType == null ? other$deviceType != null : !this$deviceType.equals(other$deviceType)) return false;
        final java.lang.Object this$deviceName = this.getDeviceName();
        final java.lang.Object other$deviceName = other.getDeviceName();
        if (this$deviceName == null ? other$deviceName != null : !this$deviceName.equals(other$deviceName)) return false;
        final java.lang.Object this$operationType = this.getOperationType();
        final java.lang.Object other$operationType = other.getOperationType();
        if (this$operationType == null ? other$operationType != null : !this$operationType.equals(other$operationType)) return false;
        final java.lang.Object this$operationDesc = this.getOperationDesc();
        final java.lang.Object other$operationDesc = other.getOperationDesc();
        if (this$operationDesc == null ? other$operationDesc != null : !this$operationDesc.equals(other$operationDesc)) return false;
        final java.lang.Object this$oldData = this.getOldData();
        final java.lang.Object other$oldData = other.getOldData();
        if (this$oldData == null ? other$oldData != null : !this$oldData.equals(other$oldData)) return false;
        final java.lang.Object this$newData = this.getNewData();
        final java.lang.Object other$newData = other.getNewData();
        if (this$newData == null ? other$newData != null : !this$newData.equals(other$newData)) return false;
        final java.lang.Object this$operatorId = this.getOperatorId();
        final java.lang.Object other$operatorId = other.getOperatorId();
        if (this$operatorId == null ? other$operatorId != null : !this$operatorId.equals(other$operatorId)) return false;
        final java.lang.Object this$operatorName = this.getOperatorName();
        final java.lang.Object other$operatorName = other.getOperatorName();
        if (this$operatorName == null ? other$operatorName != null : !this$operatorName.equals(other$operatorName)) return false;
        final java.lang.Object this$operationTime = this.getOperationTime();
        final java.lang.Object other$operationTime = other.getOperationTime();
        if (this$operationTime == null ? other$operationTime != null : !this$operationTime.equals(other$operationTime)) return false;
        final java.lang.Object this$ipAddress = this.getIpAddress();
        final java.lang.Object other$ipAddress = other.getIpAddress();
        if (this$ipAddress == null ? other$ipAddress != null : !this$ipAddress.equals(other$ipAddress)) return false;
        final java.lang.Object this$userAgent = this.getUserAgent();
        final java.lang.Object other$userAgent = other.getUserAgent();
        if (this$userAgent == null ? other$userAgent != null : !this$userAgent.equals(other$userAgent)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof HardwareOperationLog;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $hardwareId = this.getHardwareId();
        result = result * PRIME + ($hardwareId == null ? 43 : $hardwareId.hashCode());
        final java.lang.Object $storeId = this.getStoreId();
        result = result * PRIME + ($storeId == null ? 43 : $storeId.hashCode());
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $deviceType = this.getDeviceType();
        result = result * PRIME + ($deviceType == null ? 43 : $deviceType.hashCode());
        final java.lang.Object $deviceName = this.getDeviceName();
        result = result * PRIME + ($deviceName == null ? 43 : $deviceName.hashCode());
        final java.lang.Object $operationType = this.getOperationType();
        result = result * PRIME + ($operationType == null ? 43 : $operationType.hashCode());
        final java.lang.Object $operationDesc = this.getOperationDesc();
        result = result * PRIME + ($operationDesc == null ? 43 : $operationDesc.hashCode());
        final java.lang.Object $oldData = this.getOldData();
        result = result * PRIME + ($oldData == null ? 43 : $oldData.hashCode());
        final java.lang.Object $newData = this.getNewData();
        result = result * PRIME + ($newData == null ? 43 : $newData.hashCode());
        final java.lang.Object $operatorId = this.getOperatorId();
        result = result * PRIME + ($operatorId == null ? 43 : $operatorId.hashCode());
        final java.lang.Object $operatorName = this.getOperatorName();
        result = result * PRIME + ($operatorName == null ? 43 : $operatorName.hashCode());
        final java.lang.Object $operationTime = this.getOperationTime();
        result = result * PRIME + ($operationTime == null ? 43 : $operationTime.hashCode());
        final java.lang.Object $ipAddress = this.getIpAddress();
        result = result * PRIME + ($ipAddress == null ? 43 : $ipAddress.hashCode());
        final java.lang.Object $userAgent = this.getUserAgent();
        result = result * PRIME + ($userAgent == null ? 43 : $userAgent.hashCode());
        return result;
    }
}
