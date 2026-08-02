package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

/**
 * 权限搜索DTO
 * 用于权限条件分页查询的请求参数
 */
@Schema(description = "权限搜索DTO")
public class PermissionSearchDTO {

    @Min(value = 1, message = "页码必须大于0")
    @Schema(description = "页码", example = "1")
    private int page = 1;

    @Min(value = 1, message = "每页条数必须大于0")
    @Schema(description = "每页条数", example = "10")
    private int size = 10;

    @Schema(description = "权限编码")
    private String permissionCode;

    @Schema(description = "权限名称")
    private String permissionName;

    @Schema(description = "权限类型")
    private String permissionType;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "所属模块")
    private String module;

    public PermissionSearchDTO() {
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getPermissionCode() {
        return permissionCode;
    }

    public void setPermissionCode(String permissionCode) {
        this.permissionCode = permissionCode;
    }

    public String getPermissionName() {
        return permissionName;
    }

    public void setPermissionName(String permissionName) {
        this.permissionName = permissionName;
    }

    public String getPermissionType() {
        return permissionType;
    }

    public void setPermissionType(String permissionType) {
        this.permissionType = permissionType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }
}
