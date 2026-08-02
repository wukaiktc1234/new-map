package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 职位-角色映射实体类
 * 用于管理职位与角色的自动映射关系
 */
@TableName("position_role_mapping")
@Schema(description = "职位-角色映射实体")
public class PositionRoleMapping {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO, value = "id")
    @Schema(description = "主键ID", example = "1")
    private Long id;

    /**
     * 职位ID
     */
    @TableField("position_id")
    @Schema(description = "职位ID", example = "1234567890")
    private Long positionId;

    /**
     * 职位名称（非数据库字段，用于显示）
     */
    @TableField(exist = false)
    @Schema(description = "职位名称", example = "店长")
    private String positionName;

    /**
     * 职位编码（非数据库字段，用于显示）
     */
    @TableField(exist = false)
    @Schema(description = "职位编码", example = "POS001")
    private String positionCode;

    /**
     * 角色ID
     */
    @TableField("role_id")
    @Schema(description = "角色ID", example = "1234567890")
    private Long roleId;

    /**
     * 角色名称（非数据库字段，用于显示）
     */
    @TableField(exist = false)
    @Schema(description = "角色名称", example = "店长角色")
    private String roleName;

    /**
     * 角色编码（非数据库字段，用于显示）
     */
    @TableField(exist = false)
    @Schema(description = "角色编码", example = "STORE_MANAGER")
    private String roleCode;

    /**
     * 是否主角色（1:是, 0:否）
     */
    @Schema(description = "是否主角色（1:是, 0:否）", example = "1")
    private Integer isPrimary;

    /**
     * 优先级（数字越大优先级越高）
     */
    @Schema(description = "优先级（数字越大优先级越高）", example = "10")
    private Integer priority;

    /**
     * 状态（1:启用, 0:禁用）
     */
    @Schema(description = "状态（1:启用, 0:禁用）", example = "1")
    private Integer status;

    /**
     * 映射描述
     */
    @Schema(description = "映射描述", example = "店长职位自动关联店长角色")
    private String description;

    /**
     * 创建时间
     */
    @TableField("create_time")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField("update_time")
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    /**
     * 创建人
     */
    @Schema(description = "创建人", example = "admin")
    private String createdBy;

    /**
     * 更新人
     */
    @Schema(description = "更新人", example = "admin")
    private String updatedBy;

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

    public Long getPositionId() {
        return positionId;
    }

    public void setPositionId(Long positionId) {
        this.positionId = positionId;
    }

    public String getPositionName() {
        return positionName;
    }

    public void setPositionName(String positionName) {
        this.positionName = positionName;
    }

    public String getPositionCode() {
        return positionCode;
    }

    public void setPositionCode(String positionCode) {
        this.positionCode = positionCode;
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

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public Integer getIsPrimary() {
        return isPrimary;
    }

    public void setIsPrimary(Integer isPrimary) {
        this.isPrimary = isPrimary;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
