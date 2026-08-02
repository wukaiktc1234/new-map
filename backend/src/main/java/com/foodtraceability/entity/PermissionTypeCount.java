package com.foodtraceability.entity;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 权限类型统计实体类
 */
@Schema(description = "权限类型统计")
public class PermissionTypeCount {
    
    @Schema(description = "权限类型", example = "menu")
    private String permissionType;
    
    @Schema(description = "数量", example = "10")
    private Long count;

    public PermissionTypeCount() {}

    public PermissionTypeCount(String permissionType, Long count) {
        this.permissionType = permissionType;
        this.count = count;
    }

    public String getPermissionType() {
        return permissionType;
    }

    public void setPermissionType(String permissionType) {
        this.permissionType = permissionType;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}