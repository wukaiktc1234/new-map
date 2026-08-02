package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 权限分配日志实体类
 * 用于记录员工权限变更历史
 */
@TableName("permission_assignment_log")
@Schema(description = "权限分配日志实体")
public class PermissionAssignmentLog {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO, value = "id")
    @Schema(description = "主键ID", example = "1")
    private Long id;

    /**
     * 员工ID
     */
    @TableField("employee_id")
    @Schema(description = "员工ID", example = "1234567890")
    private String employeeId;

    /**
     * 员工姓名（非数据库字段，用于显示）
     */
    @TableField(exist = false)
    @Schema(description = "员工姓名", example = "张三")
    private String employeeName;

    /**
     * 操作类型（onboard:入职, position_change:职位变更, store_assign:门店分配, manual:手动调整）
     */
    @TableField("operation_type")
    @Schema(description = "操作类型（onboard:入职, position_change:职位变更, store_assign:门店分配, manual:手动调整）", example = "onboard")
    private String operationType;

    /**
     * 原职位ID
     */
    @TableField("old_position_id")
    @Schema(description = "原职位ID", example = "1234567890")
    private String oldPositionId;

    /**
     * 原职位名称（非数据库字段，用于显示）
     */
    @TableField(exist = false)
    @Schema(description = "原职位名称", example = "店员")
    private String oldPositionName;

    /**
     * 新职位ID
     */
    @TableField("new_position_id")
    @Schema(description = "新职位ID", example = "1234567890")
    private String newPositionId;

    /**
     * 新职位名称（非数据库字段，用于显示）
     */
    @TableField(exist = false)
    @Schema(description = "新职位名称", example = "店长")
    private String newPositionName;

    /**
     * 原角色列表（JSON格式）
     */
    @TableField("old_roles")
    @Schema(description = "原角色列表（JSON格式）", example = "[{\"roleId\":\"1\",\"roleName\":\"店员角色\"}]")
    private String oldRoles;

    /**
     * 新角色列表（JSON格式）
     */
    @TableField("new_roles")
    @Schema(description = "新角色列表（JSON格式）", example = "[{\"roleId\":\"2\",\"roleName\":\"店长角色\"}]")
    private String newRoles;

    /**
     * 原数据权限（JSON格式）
     */
    @TableField("old_data_scopes")
    @Schema(description = "原数据权限（JSON格式）", example = "[{\"scopeType\":\"store\",\"scopeId\":\"1\",\"scopeName\":\"分店1\"}]")
    private String oldDataScopes;

    /**
     * 新数据权限（JSON格式）
     */
    @TableField("new_data_scopes")
    @Schema(description = "新数据权限（JSON格式）", example = "[{\"scopeType\":\"store\",\"scopeId\":\"2\",\"scopeName\":\"分店2\"}]")
    private String newDataScopes;

    /**
     * 操作人
     */
    @Schema(description = "操作人", example = "admin")
    private String operator;

    /**
     * 操作时间
     */
    @TableField("operated_at")
    @Schema(description = "操作时间")
    private LocalDateTime operatedAt;

    /**
     * 备注
     */
    @Schema(description = "备注", example = "员工入职自动分配权限")
    private String remark;
    /**
     * 是否删除（0-未删除，1-已删除）
     */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "是否删除")
    private Integer deleted;

    // Getter and Setter methods
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getOldPositionId() {
        return oldPositionId;
    }

    public void setOldPositionId(String oldPositionId) {
        this.oldPositionId = oldPositionId;
    }

    public String getOldPositionName() {
        return oldPositionName;
    }

    public void setOldPositionName(String oldPositionName) {
        this.oldPositionName = oldPositionName;
    }

    public String getNewPositionId() {
        return newPositionId;
    }

    public void setNewPositionId(String newPositionId) {
        this.newPositionId = newPositionId;
    }

    public String getNewPositionName() {
        return newPositionName;
    }

    public void setNewPositionName(String newPositionName) {
        this.newPositionName = newPositionName;
    }

    public String getOldRoles() {
        return oldRoles;
    }

    public void setOldRoles(String oldRoles) {
        this.oldRoles = oldRoles;
    }

    public String getNewRoles() {
        return newRoles;
    }

    public void setNewRoles(String newRoles) {
        this.newRoles = newRoles;
    }

    public String getOldDataScopes() {
        return oldDataScopes;
    }

    public void setOldDataScopes(String oldDataScopes) {
        this.oldDataScopes = oldDataScopes;
    }

    public String getNewDataScopes() {
        return newDataScopes;
    }

    public void setNewDataScopes(String newDataScopes) {
        this.newDataScopes = newDataScopes;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public LocalDateTime getOperatedAt() {
        return operatedAt;
    }

    public void setOperatedAt(LocalDateTime operatedAt) {
        this.operatedAt = operatedAt;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    /**
     * 是否删除（0-未删除，1-已删除）
     */
    public Integer getDeleted() {
        return deleted;
    }

    /**
     * 是否删除（0-未删除，1-已删除）
     */
    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
