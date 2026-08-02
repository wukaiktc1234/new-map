package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * 角色数据权限范围DTO
 * 用于设置角色可访问的数据范围的请求参数
 */
@Schema(description = "角色数据权限范围DTO")
public class RoleDataScopeDTO {

    @NotBlank(message = "数据权限范围不能为空")
    @Schema(description = "数据权限范围", example = "store")
    private String dataScope;

    @Schema(description = "可访问的门店ID列表", example = "[\"1\", \"2\"]")
    private List<String> accessibleStores;

    @Schema(description = "可访问的部门ID列表", example = "[\"1\", \"2\"]")
    private List<String> accessibleDepartments;

    public RoleDataScopeDTO() {
    }

    public String getDataScope() {
        return dataScope;
    }

    public void setDataScope(String dataScope) {
        this.dataScope = dataScope;
    }

    public List<String> getAccessibleStores() {
        return accessibleStores;
    }

    public void setAccessibleStores(List<String> accessibleStores) {
        this.accessibleStores = accessibleStores;
    }

    public List<String> getAccessibleDepartments() {
        return accessibleDepartments;
    }

    public void setAccessibleDepartments(List<String> accessibleDepartments) {
        this.accessibleDepartments = accessibleDepartments;
    }
}
