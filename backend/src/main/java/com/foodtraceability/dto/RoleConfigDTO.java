package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 角色配置DTO
 */
@Schema(description = "角色配置DTO")
public class RoleConfigDTO {
    @Schema(description = "角色ID（更新时必填）")
    private String roleId;
    @Schema(description = "角色名称")
    private String name;
    @Schema(description = "角色编码")
    private String code;
    @Schema(description = "角色描述")
    private String description;
    @Schema(description = "数据权限范围")
    private String dataScope;
    @Schema(description = "权限ID列表")
    private List<String> permissionIds;
    @Schema(description = "门店ID列表（当dataScope为stores时）")
    private List<String> storeIds;
    @Schema(description = "部门ID列表（当dataScope为departments时）")
    private List<String> departmentIds;
    @Schema(description = "是否启用")
    private Boolean enabled;

    public RoleConfigDTO() {
    }

    public String getRoleId() {
        return this.roleId;
    }

    public String getName() {
        return this.name;
    }

    public String getCode() {
        return this.code;
    }

    public String getDescription() {
        return this.description;
    }

    public String getDataScope() {
        return this.dataScope;
    }

    public List<String> getPermissionIds() {
        return this.permissionIds;
    }

    public List<String> getStoreIds() {
        return this.storeIds;
    }

    public List<String> getDepartmentIds() {
        return this.departmentIds;
    }

    public Boolean getEnabled() {
        return this.enabled;
    }

    public void setRoleId(final String roleId) {
        this.roleId = roleId;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public void setDataScope(final String dataScope) {
        this.dataScope = dataScope;
    }

    public void setPermissionIds(final List<String> permissionIds) {
        this.permissionIds = permissionIds;
    }

    public void setStoreIds(final List<String> storeIds) {
        this.storeIds = storeIds;
    }

    public void setDepartmentIds(final List<String> departmentIds) {
        this.departmentIds = departmentIds;
    }

    public void setEnabled(final Boolean enabled) {
        this.enabled = enabled;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof RoleConfigDTO)) return false;
        final RoleConfigDTO other = (RoleConfigDTO) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$enabled = this.getEnabled();
        final java.lang.Object other$enabled = other.getEnabled();
        if (this$enabled == null ? other$enabled != null : !this$enabled.equals(other$enabled)) return false;
        final java.lang.Object this$roleId = this.getRoleId();
        final java.lang.Object other$roleId = other.getRoleId();
        if (this$roleId == null ? other$roleId != null : !this$roleId.equals(other$roleId)) return false;
        final java.lang.Object this$name = this.getName();
        final java.lang.Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final java.lang.Object this$code = this.getCode();
        final java.lang.Object other$code = other.getCode();
        if (this$code == null ? other$code != null : !this$code.equals(other$code)) return false;
        final java.lang.Object this$description = this.getDescription();
        final java.lang.Object other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) return false;
        final java.lang.Object this$dataScope = this.getDataScope();
        final java.lang.Object other$dataScope = other.getDataScope();
        if (this$dataScope == null ? other$dataScope != null : !this$dataScope.equals(other$dataScope)) return false;
        final java.lang.Object this$permissionIds = this.getPermissionIds();
        final java.lang.Object other$permissionIds = other.getPermissionIds();
        if (this$permissionIds == null ? other$permissionIds != null : !this$permissionIds.equals(other$permissionIds)) return false;
        final java.lang.Object this$storeIds = this.getStoreIds();
        final java.lang.Object other$storeIds = other.getStoreIds();
        if (this$storeIds == null ? other$storeIds != null : !this$storeIds.equals(other$storeIds)) return false;
        final java.lang.Object this$departmentIds = this.getDepartmentIds();
        final java.lang.Object other$departmentIds = other.getDepartmentIds();
        if (this$departmentIds == null ? other$departmentIds != null : !this$departmentIds.equals(other$departmentIds)) return false;
        return true;
    }

    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof RoleConfigDTO;
    }

    @java.lang.Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $enabled = this.getEnabled();
        result = result * PRIME + ($enabled == null ? 43 : $enabled.hashCode());
        final java.lang.Object $roleId = this.getRoleId();
        result = result * PRIME + ($roleId == null ? 43 : $roleId.hashCode());
        final java.lang.Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final java.lang.Object $code = this.getCode();
        result = result * PRIME + ($code == null ? 43 : $code.hashCode());
        final java.lang.Object $description = this.getDescription();
        result = result * PRIME + ($description == null ? 43 : $description.hashCode());
        final java.lang.Object $dataScope = this.getDataScope();
        result = result * PRIME + ($dataScope == null ? 43 : $dataScope.hashCode());
        final java.lang.Object $permissionIds = this.getPermissionIds();
        result = result * PRIME + ($permissionIds == null ? 43 : $permissionIds.hashCode());
        final java.lang.Object $storeIds = this.getStoreIds();
        result = result * PRIME + ($storeIds == null ? 43 : $storeIds.hashCode());
        final java.lang.Object $departmentIds = this.getDepartmentIds();
        result = result * PRIME + ($departmentIds == null ? 43 : $departmentIds.hashCode());
        return result;
    }

    @java.lang.Override
    public java.lang.String toString() {
        return "RoleConfigDTO(roleId=" + this.getRoleId() + ", name=" + this.getName() + ", code=" + this.getCode() + ", description=" + this.getDescription() + ", dataScope=" + this.getDataScope() + ", permissionIds=" + this.getPermissionIds() + ", storeIds=" + this.getStoreIds() + ", departmentIds=" + this.getDepartmentIds() + ", enabled=" + this.getEnabled() + ")";
    }
}
