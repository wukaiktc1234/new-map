package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 用户分配角色DTO
 * 用于将角色分配给用户的请求参数
 */
@Schema(description = "用户分配角色DTO")
public class UserAssignRolesDTO {

    @NotNull(message = "角色ID列表不能为空")
    @Schema(description = "角色ID列表", example = "[\"1\", \"2\"]")
    private List<String> roleIds;

    public UserAssignRolesDTO() {
    }

    public List<String> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<String> roleIds) {
        this.roleIds = roleIds;
    }
}
