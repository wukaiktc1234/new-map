package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 角色权限分配DTO
 * 用于为角色分配权限列表的请求参数
 */
@Schema(description = "角色权限分配DTO")
public class RolePermissionAssignDTO {

    @NotNull(message = "权限列表不能为空")
    @Schema(description = "权限编码列表", example = "[\"system:user:read\", \"system:user:create\"]")
    private List<String> permissions;

    public RolePermissionAssignDTO() {
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
}
