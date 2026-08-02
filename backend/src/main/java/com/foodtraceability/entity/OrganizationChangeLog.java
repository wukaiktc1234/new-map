package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 组织架构变更日志实体类
 * 用于记录组织架构的变更历史
 */
@TableName("organization_change_logs")
@Schema(description = "组织架构变更日志实体")
public class OrganizationChangeLog {

    /**
     * 变更ID
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    @Schema(description = "变更ID", example = "1234567890")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 变更类型
     * 1: 部门变更, 2: 职位变更, 3: 员工变更
     */
    @Schema(description = "变更类型（1: 部门变更, 2: 职位变更, 3: 员工变更）", example = "1")
    private Integer changeType;

    /**
     * 变更对象ID
     */
    @Schema(description = "变更对象ID", example = "1234567890")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long objectId;

    /**
     * 变更对象名称
     */
    @Schema(description = "变更对象名称", example = "技术部")
    private String objectName;

    /**
     * 变更前状态
     */
    @Schema(description = "变更前状态", example = "{\"name\":\"旧名称\",\"parentId\":\"123\"}")
    private String beforeChange;

    /**
     * 变更后状态
     */
    @Schema(description = "变更后状态", example = "{\"name\":\"新名称\",\"parentId\":\"456\"}")
    private String afterChange;

    /**
     * 变更原因
     */
    @Schema(description = "变更原因", example = "组织架构调整")
    private String changeReason;

    /**
     * 操作人
     */
    @Schema(description = "操作人", example = "admin")
    private String operator;

    /**
     * 操作时间
     */
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "操作时间")
    private LocalDateTime operateTime;

    /**
     * 变更状态
     * 1: 成功, 0: 失败
     */
    @Schema(description = "变更状态（1: 成功, 0: 失败）", example = "1")
    private Integer status;

    /**
     * 备注
     */
    @Schema(description = "备注", example = "部门名称变更")
    private String remark;

    /**
     * 逻辑删除标记
     */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除：1-已删除，0-未删除", example = "0")
    private Integer deleted;

    // Getter and Setter methods
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getChangeType() {
        return changeType;
    }

    public void setChangeType(Integer changeType) {
        this.changeType = changeType;
    }

    public Long getObjectId() {
        return objectId;
    }

    public void setObjectId(Long objectId) {
        this.objectId = objectId;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public String getBeforeChange() {
        return beforeChange;
    }

    public void setBeforeChange(String beforeChange) {
        this.beforeChange = beforeChange;
    }

    public String getAfterChange() {
        return afterChange;
    }

    public void setAfterChange(String afterChange) {
        this.afterChange = afterChange;
    }

    public String getChangeReason() {
        return changeReason;
    }

    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public LocalDateTime getOperateTime() {
        return operateTime;
    }

    public void setOperateTime(LocalDateTime operateTime) {
        this.operateTime = operateTime;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
