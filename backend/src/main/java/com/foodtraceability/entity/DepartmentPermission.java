package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 部门权限实体类
 * 用于控制部门数据的访问权限
 */
@TableName("department_permissions")
@Schema(description = "部门权限实体")
public class DepartmentPermission {

    /**
     * 权限ID
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    @Schema(description = "权限ID", example = "1234567890")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 部门ID
     */
    @TableField("department_id")
    @Schema(description = "部门ID", example = "1234567890")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long departmentId;

    /**
     * 部门名称
     */
    @TableField(exist = false)
    @Schema(description = "部门名称", example = "技术部")
    private String departmentName;

    /**
     * 角色ID
     */
    @TableField("role_id")
    @Schema(description = "角色ID", example = "1234567890")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long roleId;

    /**
     * 角色名称
     */
    @TableField(exist = false)
    @Schema(description = "角色名称", example = "部门管理员")
    private String roleName;

    /**
     * 用户ID
     */
    @TableField("user_id")
    @Schema(description = "用户ID", example = "1234567890")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /**
     * 用户名
     */
    @TableField(exist = false)
    @Schema(description = "用户名", example = "admin")
    private String userName;

    /**
     * 权限类型
     * 1: 查看权限, 2: 编辑权限, 3: 管理权限
     */
    @Schema(description = "权限类型（1: 查看权限, 2: 编辑权限, 3: 管理权限）", example = "2")
    private Integer permissionType;

    /**
     * 权限状态
     * 1: 启用, 0: 禁用
     */
    @Schema(description = "权限状态（1: 启用, 0: 禁用）", example = "1")
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记
     */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除：1-已删除，0-未删除", example = "0")
    private Integer deleted;

    /**
     * 创建人
     */
    @TableField(fill = FieldFill.INSERT, exist = false)
    @Schema(description = "创建人", example = "system")
    private String createBy;

    /**
     * 更新人
     */
    @TableField(fill = FieldFill.INSERT_UPDATE, exist = false)
    @Schema(description = "更新人", example = "admin")
    private String updateBy;

    // Getter and Setter methods
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Integer getPermissionType() {
        return permissionType;
    }

    public void setPermissionType(Integer permissionType) {
        this.permissionType = permissionType;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }
}
