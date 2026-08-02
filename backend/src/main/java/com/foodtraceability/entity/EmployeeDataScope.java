package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 员工数据权限实体类
 * 用于管理员工的数据访问权限范围
 */
@TableName("employee_data_scope")
@Schema(description = "员工数据权限实体")
public class EmployeeDataScope {

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
     * 权限范围类型（store:门店, department:部门, all:全部）
     */
    @TableField("scope_type")
    @Schema(description = "权限范围类型（store:门店, department:部门, all:全部）", example = "store")
    private String scopeType;

    /**
     * 权限范围ID（门店ID或部门ID）
     */
    @TableField("scope_id")
    @Schema(description = "权限范围ID（门店ID或部门ID）", example = "1234567890")
    private String scopeId;

    /**
     * 权限范围名称
     */
    @TableField("scope_name")
    @Schema(description = "权限范围名称", example = "中心店")
    private String scopeName;

    /**
     * 权限来源（auto:自动分配, manual:手动分配）
     */
    @Schema(description = "权限来源（auto:自动分配, manual:手动分配）", example = "auto")
    private String source;

    /**
     * 状态（1:启用, 0:禁用）
     */
    @Schema(description = "状态（1:启用, 0:禁用）", example = "1")
    private Integer status;

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

    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    public String getScopeId() {
        return scopeId;
    }

    public void setScopeId(String scopeId) {
        this.scopeId = scopeId;
    }

    public String getScopeName() {
        return scopeName;
    }

    public void setScopeName(String scopeName) {
        this.scopeName = scopeName;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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
