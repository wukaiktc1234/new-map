package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据变更历史表
 * 记录核心业务数据的变更历史，实现数据版本控制
 */
@TableName("data_change_history")
@Schema(description = "数据变更历史实体")
public class DataChangeHistory implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 历史ID，主键
     */
    @TableId(value = "history_id", type = IdType.ASSIGN_ID)
    @Schema(description = "历史ID", example = "1")
    private Long historyId;
    /**
     * 表名
     */
    @TableField("table_name")
    @Schema(description = "表名", example = "food")
    private String tableName;
    /**
     * 记录ID
     */
    @TableField("record_id")
    @Schema(description = "记录ID", example = "F001")
    private String recordId;
    /**
     * 变更类型（CREATE/UPDATE/DELETE）
     */
    @TableField("change_type")
    @Schema(description = "变更类型", example = "UPDATE")
    private String changeType;
    /**
     * 变更前数据（JSON格式）
     */
    @TableField("old_data")
    @Schema(description = "变更前数据", example = "{\"foodName\":\"旧名称\",\"price\":10.00}")
    private String oldData;
    /**
     * 变更后数据（JSON格式）
     */
    @TableField("new_data")
    @Schema(description = "变更后数据", example = "{\"foodName\":\"新名称\",\"price\":12.00}")
    private String newData;
    /**
     * 操作人
     */
    @TableField("operation_user")
    @Schema(description = "操作人", example = "admin")
    private String operationUser;
    /**
     * 操作时间
     */
    @TableField(value = "operation_time", fill = FieldFill.INSERT)
    @Schema(description = "操作时间")
    private LocalDateTime operationTime;
    /**
     * 版本号
     */
    @TableField("version")
    @Schema(description = "版本号", example = "1")
    private Integer version;
    /**
     * 操作IP
     */
    @TableField("operation_ip")
    @Schema(description = "操作IP", example = "127.0.0.1")
    private String operationIp;
    /**
     * 请求ID（用于追踪请求链路）
     */
    @TableField("request_id")
    @Schema(description = "请求ID", example = "REQ123456")
    private String requestId;
    /**
     * 备注信息
     */
    @TableField("remarks")
    @Schema(description = "备注信息", example = "手动更新")
    private String remarks;

    public LocalDateTime getOperationTime() {
        return this.operationTime;
    }

    public Long getHistoryId() {
        return this.historyId;
    }

    public String getTableName() {
        return this.tableName;
    }

    public String getRecordId() {
        return this.recordId;
    }

    public String getChangeType() {
        return this.changeType;
    }

    public String getOldData() {
        return this.oldData;
    }

    public String getNewData() {
        return this.newData;
    }

    public String getOperationUser() {
        return this.operationUser;
    }

    public Integer getVersion() {
        return this.version;
    }

    public String getOperationIp() {
        return this.operationIp;
    }

    public String getRequestId() {
        return this.requestId;
    }

    public String getRemarks() {
        return this.remarks;
    }

    public DataChangeHistory() {
    }

    /**
     * 历史ID，主键
     */
    public void setHistoryId(final Long historyId) {
        this.historyId = historyId;
    }

    /**
     * 表名
     */
    public void setTableName(final String tableName) {
        this.tableName = tableName;
    }

    /**
     * 记录ID
     */
    public void setRecordId(final String recordId) {
        this.recordId = recordId;
    }

    /**
     * 变更类型（CREATE/UPDATE/DELETE）
     */
    public void setChangeType(final String changeType) {
        this.changeType = changeType;
    }

    /**
     * 变更前数据（JSON格式）
     */
    public void setOldData(final String oldData) {
        this.oldData = oldData;
    }

    /**
     * 变更后数据（JSON格式）
     */
    public void setNewData(final String newData) {
        this.newData = newData;
    }

    /**
     * 操作人
     */
    public void setOperationUser(final String operationUser) {
        this.operationUser = operationUser;
    }

    /**
     * 操作时间
     */
    public void setOperationTime(final LocalDateTime operationTime) {
        this.operationTime = operationTime;
    }

    /**
     * 版本号
     */
    public void setVersion(final Integer version) {
        this.version = version;
    }

    /**
     * 操作IP
     */
    public void setOperationIp(final String operationIp) {
        this.operationIp = operationIp;
    }

    /**
     * 请求ID（用于追踪请求链路）
     */
    public void setRequestId(final String requestId) {
        this.requestId = requestId;
    }

    /**
     * 备注信息
     */
    public void setRemarks(final String remarks) {
        this.remarks = remarks;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "DataChangeHistory(historyId=" + this.getHistoryId() + ", tableName=" + this.getTableName() + ", recordId=" + this.getRecordId() + ", changeType=" + this.getChangeType() + ", oldData=" + this.getOldData() + ", newData=" + this.getNewData() + ", operationUser=" + this.getOperationUser() + ", operationTime=" + this.getOperationTime() + ", version=" + this.getVersion() + ", operationIp=" + this.getOperationIp() + ", requestId=" + this.getRequestId() + ", remarks=" + this.getRemarks() + ")";
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof DataChangeHistory)) return false;
        final DataChangeHistory other = (DataChangeHistory) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$historyId = this.getHistoryId();
        final java.lang.Object other$historyId = other.getHistoryId();
        if (this$historyId == null ? other$historyId != null : !this$historyId.equals(other$historyId)) return false;
        final java.lang.Object this$version = this.getVersion();
        final java.lang.Object other$version = other.getVersion();
        if (this$version == null ? other$version != null : !this$version.equals(other$version)) return false;
        final java.lang.Object this$tableName = this.getTableName();
        final java.lang.Object other$tableName = other.getTableName();
        if (this$tableName == null ? other$tableName != null : !this$tableName.equals(other$tableName)) return false;
        final java.lang.Object this$recordId = this.getRecordId();
        final java.lang.Object other$recordId = other.getRecordId();
        if (this$recordId == null ? other$recordId != null : !this$recordId.equals(other$recordId)) return false;
        final java.lang.Object this$changeType = this.getChangeType();
        final java.lang.Object other$changeType = other.getChangeType();
        if (this$changeType == null ? other$changeType != null : !this$changeType.equals(other$changeType)) return false;
        final java.lang.Object this$oldData = this.getOldData();
        final java.lang.Object other$oldData = other.getOldData();
        if (this$oldData == null ? other$oldData != null : !this$oldData.equals(other$oldData)) return false;
        final java.lang.Object this$newData = this.getNewData();
        final java.lang.Object other$newData = other.getNewData();
        if (this$newData == null ? other$newData != null : !this$newData.equals(other$newData)) return false;
        final java.lang.Object this$operationUser = this.getOperationUser();
        final java.lang.Object other$operationUser = other.getOperationUser();
        if (this$operationUser == null ? other$operationUser != null : !this$operationUser.equals(other$operationUser)) return false;
        final java.lang.Object this$operationTime = this.getOperationTime();
        final java.lang.Object other$operationTime = other.getOperationTime();
        if (this$operationTime == null ? other$operationTime != null : !this$operationTime.equals(other$operationTime)) return false;
        final java.lang.Object this$operationIp = this.getOperationIp();
        final java.lang.Object other$operationIp = other.getOperationIp();
        if (this$operationIp == null ? other$operationIp != null : !this$operationIp.equals(other$operationIp)) return false;
        final java.lang.Object this$requestId = this.getRequestId();
        final java.lang.Object other$requestId = other.getRequestId();
        if (this$requestId == null ? other$requestId != null : !this$requestId.equals(other$requestId)) return false;
        final java.lang.Object this$remarks = this.getRemarks();
        final java.lang.Object other$remarks = other.getRemarks();
        if (this$remarks == null ? other$remarks != null : !this$remarks.equals(other$remarks)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof DataChangeHistory;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $historyId = this.getHistoryId();
        result = result * PRIME + ($historyId == null ? 43 : $historyId.hashCode());
        final java.lang.Object $version = this.getVersion();
        result = result * PRIME + ($version == null ? 43 : $version.hashCode());
        final java.lang.Object $tableName = this.getTableName();
        result = result * PRIME + ($tableName == null ? 43 : $tableName.hashCode());
        final java.lang.Object $recordId = this.getRecordId();
        result = result * PRIME + ($recordId == null ? 43 : $recordId.hashCode());
        final java.lang.Object $changeType = this.getChangeType();
        result = result * PRIME + ($changeType == null ? 43 : $changeType.hashCode());
        final java.lang.Object $oldData = this.getOldData();
        result = result * PRIME + ($oldData == null ? 43 : $oldData.hashCode());
        final java.lang.Object $newData = this.getNewData();
        result = result * PRIME + ($newData == null ? 43 : $newData.hashCode());
        final java.lang.Object $operationUser = this.getOperationUser();
        result = result * PRIME + ($operationUser == null ? 43 : $operationUser.hashCode());
        final java.lang.Object $operationTime = this.getOperationTime();
        result = result * PRIME + ($operationTime == null ? 43 : $operationTime.hashCode());
        final java.lang.Object $operationIp = this.getOperationIp();
        result = result * PRIME + ($operationIp == null ? 43 : $operationIp.hashCode());
        final java.lang.Object $requestId = this.getRequestId();
        result = result * PRIME + ($requestId == null ? 43 : $requestId.hashCode());
        final java.lang.Object $remarks = this.getRemarks();
        result = result * PRIME + ($remarks == null ? 43 : $remarks.hashCode());
        return result;
    }
}
