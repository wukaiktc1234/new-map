package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 角色部门关联实体
 * 用于定义角色可访问的部门列表（数据权限）
 */
@TableName("role_departments")
@Schema(description = "角色部门关联实体")
public class RoleDepartment implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;
    /**
     * 角色ID
     */
    @Schema(description = "角色ID")
    private Long roleId;
    /**
     * 部门ID
     */
    @Schema(description = "部门ID")
    private String departmentId;
    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
    /**
     * 创建人ID
     */
    @Schema(description = "创建人ID")
    private String createdBy;
    /**
     * 是否删除（0-未删除，1-已删除）
     */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "是否删除")
    private Integer deleted;

    public RoleDepartment() {
    }

    /**
     * 主键ID
     */
    public Long getId() {
        return this.id;
    }

    /**
     * 角色ID
     */
    public Long getRoleId() {
        return this.roleId;
    }

    /**
     * 部门ID
     */
    public String getDepartmentId() {
        return this.departmentId;
    }

    /**
     * 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    /**
     * 创建人ID
     */
    public String getCreatedBy() {
        return this.createdBy;
    }

    /**
     * 主键ID
     */
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * 角色ID
     */
    public void setRoleId(final Long roleId) {
        this.roleId = roleId;
    }

    /**
     * 部门ID
     */
    public void setDepartmentId(final String departmentId) {
        this.departmentId = departmentId;
    }

    /**
     * 创建时间
     */
    public void setCreatedAt(final LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 创建人ID
     */
    public void setCreatedBy(final String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     * 是否删除（0-未删除，1-已删除）
     */
    public Integer getDeleted() {
        return this.deleted;
    }

    /**
     * 是否删除（0-未删除，1-已删除）
     */
    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof RoleDepartment)) return false;
        final RoleDepartment other = (RoleDepartment) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$roleId = this.getRoleId();
        final java.lang.Object other$roleId = other.getRoleId();
        if (this$roleId == null ? other$roleId != null : !this$roleId.equals(other$roleId)) return false;
        final java.lang.Object this$departmentId = this.getDepartmentId();
        final java.lang.Object other$departmentId = other.getDepartmentId();
        if (this$departmentId == null ? other$departmentId != null : !this$departmentId.equals(other$departmentId)) return false;
        final java.lang.Object this$createdAt = this.getCreatedAt();
        final java.lang.Object other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) return false;
        final java.lang.Object this$createdBy = this.getCreatedBy();
        final java.lang.Object other$createdBy = other.getCreatedBy();
        if (this$createdBy == null ? other$createdBy != null : !this$createdBy.equals(other$createdBy)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof RoleDepartment;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $roleId = this.getRoleId();
        result = result * PRIME + ($roleId == null ? 43 : $roleId.hashCode());
        final java.lang.Object $departmentId = this.getDepartmentId();
        result = result * PRIME + ($departmentId == null ? 43 : $departmentId.hashCode());
        final java.lang.Object $createdAt = this.getCreatedAt();
        result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
        final java.lang.Object $createdBy = this.getCreatedBy();
        result = result * PRIME + ($createdBy == null ? 43 : $createdBy.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "RoleDepartment(id=" + this.getId() + ", roleId=" + this.getRoleId() + ", departmentId=" + this.getDepartmentId() + ", createdAt=" + this.getCreatedAt() + ", createdBy=" + this.getCreatedBy() + ")";
    }
}
