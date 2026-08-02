package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 角色实体类
 * 用于管理系统角色信息，支持权限分配
 */
@TableName("roles")
@Schema(description = "角色实体")
public class Role {
    /**
     * 角色ID
     * 注：PostgreSQL 真实表 roles 主键为 role_id（V1.0.0.100__init_postgresql.sql）。
     */
    @TableId(type = IdType.AUTO, value = "role_id")
    @Schema(description = "角色ID", example = "1")
    private Long id;

    /**
     * 角色标识（唯一，如ROLE_ADMIN）
     * 对应数据库 roles.role_id_str 列，NOT NULL
     */
    @TableField("role_id_str")
    @Schema(description = "角色标识（唯一，如ROLE_ADMIN）", example = "ROLE_ADMIN")
    private String roleIdStr;

    /**
     * 角色编码（唯一）
     */
    @TableField("ROLE_CODE")
    @Schema(description = "角色编码（唯一）", example = "admin")
    private String roleCode;
    /**
     * 角色名称
     */
    @TableField("ROLE_NAME")
    @Schema(description = "角色名称", example = "系统管理员")
    private String roleName;
    /**
     * 角色描述
     */
    @TableField("DESCRIPTION")
    @Schema(description = "角色描述", example = "拥有系统所有权限的管理员角色")
    private String roleDescription;
    /**
     * 角色级别（数字越大级别越高）
     * 注：roles 表无 level 列，此字段不映射数据库
     */
    @TableField(exist = false)
    @Schema(description = "角色级别（数字越大级别越高）", example = "100")
    private Integer roleLevel;
    /**
     * 角色状态（active: 启用, inactive: 禁用）
     * 数据库列为 varchar，直接以字符串存储，前端期望直接返回 'active'/'inactive'。
     */
    @TableField("STATUS")
    @Schema(description = "角色状态（active: 启用, inactive: 禁用）", example = "active")
    private String status;
    /**
     * 角色权限列表（JSON格式存储）
     * 注：roles 表无 permissions 列，此字段不映射数据库
     */
    @TableField(exist = false)
    @Schema(description = "角色权限列表（JSON格式存储）", example = "[\"food:*\", \"trace:*\", \"user:view\", \"system:config\"]")
    private String permissions;
    /**
     * 角色类型（1: 系统角色, 2: 自定义角色）
     * 注：roles 表无 role_type 列，此字段不映射数据库
     */
    @TableField(exist = false)
    @Schema(description = "角色类型（1: 系统角色, 2: 自定义角色）", example = "1")
    private Integer roleType;
    /**
     * 是否系统内置角色
     * 注：roles 表无 is_system 列，此字段不映射数据库
     */
    @TableField(exist = false)
    @Schema(description = "是否系统内置角色", example = "true")
    private Boolean systemBuilt;
    /**
     * 父角色ID（用于角色继承）
     * 注：roles 表无 parent_id 列，此字段不映射数据库
     */
    @TableField(exist = false)
    @Schema(description = "父角色ID（用于角色继承）", example = "1")
    private Long parentId;
    /**
     * 数据权限范围（all: 全部数据, company: 公司数据, store: 门店数据, department: 部门数据, self: 个人数据）
     */
    @TableField("data_scope")
    @Schema(description = "数据权限范围（all: 全部数据, company: 公司数据, store: 门店数据, department: 部门数据, self: 个人数据）", example = "store")
    private String dataScope;
    /**
     * 可访问的门店ID列表（JSON格式存储，仅当dataScope为store时有效）
     * 注：roles 表无 accessible_stores 列，此字段不映射数据库
     */
    @TableField(exist = false)
    @Schema(description = "可访问的门店ID列表（JSON格式存储，仅当dataScope为store时有效）", example = "[\"1234567890\", \"0987654321\"]")
    private String accessibleStores;
    /**
     * 可访问的部门ID列表（JSON格式存储，仅当dataScope为department时有效）
     * 注：roles 表无 accessible_departments 列，此字段不映射数据库
     */
    @TableField(exist = false)
    @Schema(description = "可访问的部门ID列表（JSON格式存储，仅当dataScope为department时有效）", example = "[\"1234567890\", \"0987654321\"]")
    private String accessibleDepartments;
    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdTime;
    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;
    /**
     * 创建人ID
     * 注：roles 表无 created_by 列，此字段不映射数据库
     */
    @TableField(exist = false)
    @Schema(description = "创建人ID", example = "1")
    private Long createdBy;
    /**
     * 更新人ID
     * 注：roles 表无 updated_by 列，此字段不映射数据库
     */
    @TableField(exist = false)
    @Schema(description = "更新人ID", example = "1")
    private Long updatedBy;
    /**
     * 逻辑删除标记
     */
    @TableLogic
    @TableField("DELETED")
    @Schema(description = "逻辑删除标记", example = "0")
    private Integer deleted;
    /**
     * 版本号（乐观锁）
     */
    @Version
    @TableField("VERSION")
    @Schema(description = "版本号（乐观锁）", example = "1")
    private Integer version;
    /**
     * 扩展字段（JSON格式存储）
     * 注：roles 表无 ext_data 列，此字段不映射数据库
     */
    @TableField(exist = false)
    @Schema(description = "扩展字段（JSON格式存储）", example = "{\"customField\": \"value\"}")
    private String extData;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取角色ID（兼容旧代码）
     */
    public Long getRoleId() {
        return id;
    }

    /**
     * 设置角色ID（兼容旧代码）
     */
    public void setRoleId(Long roleId) {
        this.id = roleId;
    }

    public String getRoleIdStr() {
        return roleIdStr;
    }

    public void setRoleIdStr(String roleIdStr) {
        this.roleIdStr = roleIdStr;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleDescription() {
        return roleDescription;
    }

    public void setRoleDescription(String roleDescription) {
        this.roleDescription = roleDescription;
    }

    public Integer getRoleLevel() {
        return roleLevel;
    }

    public void setRoleLevel(Integer roleLevel) {
        this.roleLevel = roleLevel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPermissions() {
        return permissions;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }

    public Integer getRoleType() {
        return roleType;
    }

    public void setRoleType(Integer roleType) {
        this.roleType = roleType;
    }

    /**
     * 获取角色类型字符串（用于前端兼容性）
     * system-系统角色, custom-自定义角色
     */
    public String getRoleTypeText() {
        if (roleType == null) {
            return "custom";
        }
        return roleType == 1 ? "system" : "custom";
    }

    /**
     * 设置角色类型字符串（用于前端兼容性）
     */
    public void setRoleTypeText(String roleTypeText) {
        if ("system".equals(roleTypeText)) {
            this.roleType = 1;
        } else {
            this.roleType = 2;
        }
    }

    public Boolean getSystemBuiltIn() {
        return systemBuilt;
    }

    public void setSystemBuiltIn(Boolean systemBuiltIn) {
        this.systemBuilt = systemBuiltIn;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getDataScope() {
        return dataScope;
    }

    public void setDataScope(String dataScope) {
        this.dataScope = dataScope;
    }

    public String getAccessibleStores() {
        return accessibleStores;
    }

    public void setAccessibleStores(String accessibleStores) {
        this.accessibleStores = accessibleStores;
    }

    public String getAccessibleDepartments() {
        return accessibleDepartments;
    }

    public void setAccessibleDepartments(String accessibleDepartments) {
        this.accessibleDepartments = accessibleDepartments;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public LocalDateTime getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(LocalDateTime updatedTime) {
        this.updatedTime = updatedTime;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getExtData() {
        return extData;
    }

    public void setExtData(String extData) {
        this.extData = extData;
    }

    public Role() {
    }

    /**
     * 是否系统内置角色
     */
    public Boolean getSystemBuilt() {
        return this.systemBuilt;
    }

    /**
     * 是否系统内置角色
     */
    public void setSystemBuilt(final Boolean systemBuilt) {
        this.systemBuilt = systemBuilt;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof Role)) return false;
        final Role other = (Role) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$roleLevel = this.getRoleLevel();
        final java.lang.Object other$roleLevel = other.getRoleLevel();
        if (this$roleLevel == null ? other$roleLevel != null : !this$roleLevel.equals(other$roleLevel)) return false;
        final java.lang.Object this$status = this.getStatus();
        final java.lang.Object other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) return false;
        final java.lang.Object this$roleType = this.getRoleType();
        final java.lang.Object other$roleType = other.getRoleType();
        if (this$roleType == null ? other$roleType != null : !this$roleType.equals(other$roleType)) return false;
        final java.lang.Object this$systemBuilt = this.getSystemBuilt();
        final java.lang.Object other$systemBuilt = other.getSystemBuilt();
        if (this$systemBuilt == null ? other$systemBuilt != null : !this$systemBuilt.equals(other$systemBuilt)) return false;
        final java.lang.Object this$createdBy = this.getCreatedBy();
        final java.lang.Object other$createdBy = other.getCreatedBy();
        if (this$createdBy == null ? other$createdBy != null : !this$createdBy.equals(other$createdBy)) return false;
        final java.lang.Object this$updatedBy = this.getUpdatedBy();
        final java.lang.Object other$updatedBy = other.getUpdatedBy();
        if (this$updatedBy == null ? other$updatedBy != null : !this$updatedBy.equals(other$updatedBy)) return false;
        final java.lang.Object this$deleted = this.getDeleted();
        final java.lang.Object other$deleted = other.getDeleted();
        if (this$deleted == null ? other$deleted != null : !this$deleted.equals(other$deleted)) return false;
        final java.lang.Object this$version = this.getVersion();
        final java.lang.Object other$version = other.getVersion();
        if (this$version == null ? other$version != null : !this$version.equals(other$version)) return false;
        final java.lang.Object this$id = this.getId();
        final java.lang.Object other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
        final java.lang.Object this$roleIdStr = this.getRoleIdStr();
        final java.lang.Object other$roleIdStr = other.getRoleIdStr();
        if (this$roleIdStr == null ? other$roleIdStr != null : !this$roleIdStr.equals(other$roleIdStr)) return false;
        final java.lang.Object this$roleCode = this.getRoleCode();
        final java.lang.Object other$roleCode = other.getRoleCode();
        if (this$roleCode == null ? other$roleCode != null : !this$roleCode.equals(other$roleCode)) return false;
        final java.lang.Object this$roleName = this.getRoleName();
        final java.lang.Object other$roleName = other.getRoleName();
        if (this$roleName == null ? other$roleName != null : !this$roleName.equals(other$roleName)) return false;
        final java.lang.Object this$roleDescription = this.getRoleDescription();
        final java.lang.Object other$roleDescription = other.getRoleDescription();
        if (this$roleDescription == null ? other$roleDescription != null : !this$roleDescription.equals(other$roleDescription)) return false;
        final java.lang.Object this$permissions = this.getPermissions();
        final java.lang.Object other$permissions = other.getPermissions();
        if (this$permissions == null ? other$permissions != null : !this$permissions.equals(other$permissions)) return false;
        final java.lang.Object this$parentId = this.getParentId();
        final java.lang.Object other$parentId = other.getParentId();
        if (this$parentId == null ? other$parentId != null : !this$parentId.equals(other$parentId)) return false;
        final java.lang.Object this$dataScope = this.getDataScope();
        final java.lang.Object other$dataScope = other.getDataScope();
        if (this$dataScope == null ? other$dataScope != null : !this$dataScope.equals(other$dataScope)) return false;
        final java.lang.Object this$accessibleStores = this.getAccessibleStores();
        final java.lang.Object other$accessibleStores = other.getAccessibleStores();
        if (this$accessibleStores == null ? other$accessibleStores != null : !this$accessibleStores.equals(other$accessibleStores)) return false;
        final java.lang.Object this$accessibleDepartments = this.getAccessibleDepartments();
        final java.lang.Object other$accessibleDepartments = other.getAccessibleDepartments();
        if (this$accessibleDepartments == null ? other$accessibleDepartments != null : !this$accessibleDepartments.equals(other$accessibleDepartments)) return false;
        final java.lang.Object this$createdTime = this.getCreatedTime();
        final java.lang.Object other$createdTime = other.getCreatedTime();
        if (this$createdTime == null ? other$createdTime != null : !this$createdTime.equals(other$createdTime)) return false;
        final java.lang.Object this$updatedTime = this.getUpdatedTime();
        final java.lang.Object other$updatedTime = other.getUpdatedTime();
        if (this$updatedTime == null ? other$updatedTime != null : !this$updatedTime.equals(other$updatedTime)) return false;
        final java.lang.Object this$extData = this.getExtData();
        final java.lang.Object other$extData = other.getExtData();
        if (this$extData == null ? other$extData != null : !this$extData.equals(other$extData)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof Role;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $roleLevel = this.getRoleLevel();
        result = result * PRIME + ($roleLevel == null ? 43 : $roleLevel.hashCode());
        final java.lang.Object $status = this.getStatus();
        result = result * PRIME + ($status == null ? 43 : $status.hashCode());
        final java.lang.Object $roleType = this.getRoleType();
        result = result * PRIME + ($roleType == null ? 43 : $roleType.hashCode());
        final java.lang.Object $systemBuilt = this.getSystemBuilt();
        result = result * PRIME + ($systemBuilt == null ? 43 : $systemBuilt.hashCode());
        final java.lang.Object $createdBy = this.getCreatedBy();
        result = result * PRIME + ($createdBy == null ? 43 : $createdBy.hashCode());
        final java.lang.Object $updatedBy = this.getUpdatedBy();
        result = result * PRIME + ($updatedBy == null ? 43 : $updatedBy.hashCode());
        final java.lang.Object $deleted = this.getDeleted();
        result = result * PRIME + ($deleted == null ? 43 : $deleted.hashCode());
        final java.lang.Object $version = this.getVersion();
        result = result * PRIME + ($version == null ? 43 : $version.hashCode());
        final java.lang.Object $id = this.getId();
        result = result * PRIME + ($id == null ? 43 : $id.hashCode());
        final java.lang.Object $roleIdStr = this.getRoleIdStr();
        result = result * PRIME + ($roleIdStr == null ? 43 : $roleIdStr.hashCode());
        final java.lang.Object $roleCode = this.getRoleCode();
        result = result * PRIME + ($roleCode == null ? 43 : $roleCode.hashCode());
        final java.lang.Object $roleName = this.getRoleName();
        result = result * PRIME + ($roleName == null ? 43 : $roleName.hashCode());
        final java.lang.Object $roleDescription = this.getRoleDescription();
        result = result * PRIME + ($roleDescription == null ? 43 : $roleDescription.hashCode());
        final java.lang.Object $permissions = this.getPermissions();
        result = result * PRIME + ($permissions == null ? 43 : $permissions.hashCode());
        final java.lang.Object $parentId = this.getParentId();
        result = result * PRIME + ($parentId == null ? 43 : $parentId.hashCode());
        final java.lang.Object $dataScope = this.getDataScope();
        result = result * PRIME + ($dataScope == null ? 43 : $dataScope.hashCode());
        final java.lang.Object $accessibleStores = this.getAccessibleStores();
        result = result * PRIME + ($accessibleStores == null ? 43 : $accessibleStores.hashCode());
        final java.lang.Object $accessibleDepartments = this.getAccessibleDepartments();
        result = result * PRIME + ($accessibleDepartments == null ? 43 : $accessibleDepartments.hashCode());
        final java.lang.Object $createdTime = this.getCreatedTime();
        result = result * PRIME + ($createdTime == null ? 43 : $createdTime.hashCode());
        final java.lang.Object $updatedTime = this.getUpdatedTime();
        result = result * PRIME + ($updatedTime == null ? 43 : $updatedTime.hashCode());
        final java.lang.Object $extData = this.getExtData();
        result = result * PRIME + ($extData == null ? 43 : $extData.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "Role(id=" + this.getId() + ", roleIdStr=" + this.getRoleIdStr() + ", roleCode=" + this.getRoleCode() + ", roleName=" + this.getRoleName() + ", roleDescription=" + this.getRoleDescription() + ", roleLevel=" + this.getRoleLevel() + ", status=" + this.getStatus() + ", permissions=" + this.getPermissions() + ", roleType=" + this.getRoleType() + ", systemBuilt=" + this.getSystemBuilt() + ", parentId=" + this.getParentId() + ", dataScope=" + this.getDataScope() + ", accessibleStores=" + this.getAccessibleStores() + ", accessibleDepartments=" + this.getAccessibleDepartments() + ", createdTime=" + this.getCreatedTime() + ", updatedTime=" + this.getUpdatedTime() + ", createdBy=" + this.getCreatedBy() + ", updatedBy=" + this.getUpdatedBy() + ", deleted=" + this.getDeleted() + ", version=" + this.getVersion() + ", extData=" + this.getExtData() + ")";
    }
}
