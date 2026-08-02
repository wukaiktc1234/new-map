package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 分类操作日志实体类
 */
@TableName("category_operation_log")
@Schema(description = "分类操作日志")
public class CategoryOperationLog implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @Schema(description = "日志ID")
    private String id;
    @TableField("category_id")
    @Schema(description = "分类ID")
    private String categoryId;
    @TableField("category_name")
    @Schema(description = "分类名称")
    private String categoryName;
    @TableField("category_code")
    @Schema(description = "分类编码")
    private String categoryCode;
    @TableField("parent_id")
    @Schema(description = "父分类ID")
    private String parentId;
    @TableField("operation_type")
    @Schema(description = "操作类型（CREATE-创建，UPDATE-更新，DELETE-删除）")
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

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
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
        if (!(o instanceof CategoryOperationLog)) return false;
        final CategoryOperationLog other = (CategoryOperationLog) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$categoryId = this.getCategoryId();
        final java.lang.Object other$categoryId = other.getCategoryId();
        if (this$categoryId == null ? other$categoryId != null : !this$categoryId.equals(other$categoryId)) return false;
        final java.lang.Object this$categoryName = this.getCategoryName();
        final java.lang.Object other$categoryName = other.getCategoryName();
        if (this$categoryName == null ? other$categoryName != null : !this$categoryName.equals(other$categoryName)) return false;
        final java.lang.Object this$categoryCode = this.getCategoryCode();
        final java.lang.Object other$categoryCode = other.getCategoryCode();
        if (this$categoryCode == null ? other$categoryCode != null : !this$categoryCode.equals(other$categoryCode)) return false;
        final java.lang.Object this$parentId = this.getParentId();
        final java.lang.Object other$parentId = other.getParentId();
        if (this$parentId == null ? other$parentId != null : !this$parentId.equals(other$parentId)) return false;
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
        return other instanceof CategoryOperationLog;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $categoryId = this.getCategoryId();
        result = result * PRIME + ($categoryId == null ? 43 : $categoryId.hashCode());
        final java.lang.Object $categoryName = this.getCategoryName();
        result = result * PRIME + ($categoryName == null ? 43 : $categoryName.hashCode());
        final java.lang.Object $categoryCode = this.getCategoryCode();
        result = result * PRIME + ($categoryCode == null ? 43 : $categoryCode.hashCode());
        final java.lang.Object $parentId = this.getParentId();
        result = result * PRIME + ($parentId == null ? 43 : $parentId.hashCode());
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
