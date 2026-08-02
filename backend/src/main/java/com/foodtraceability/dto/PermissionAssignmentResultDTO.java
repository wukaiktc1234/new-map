package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 权限分配结果DTO
 */
@Schema(description = "权限分配结果DTO")
public class PermissionAssignmentResultDTO {

    /**
     * 员工ID
     */
    @Schema(description = "员工ID", example = "1234567890")
    private String employeeId;

    /**
     * 员工姓名
     */
    @Schema(description = "员工姓名", example = "张三")
    private String employeeName;

    /**
     * 分配的角色列表
     */
    @Schema(description = "分配的角色列表")
    private List<RoleInfo> assignedRoles;

    /**
     * 分配的数据权限列表
     */
    @Schema(description = "分配的数据权限列表")
    private List<DataScopeInfo> assignedDataScopes;

    /**
     * 操作类型
     */
    @Schema(description = "操作类型", example = "onboard")
    private String operationType;

    /**
     * 是否成功
     */
    @Schema(description = "是否成功", example = "true")
    private Boolean success;

    /**
     * 消息
     */
    @Schema(description = "消息", example = "权限分配成功")
    private String message;

    /**
     * 角色信息
     */
    @Schema(description = "角色信息")
    public static class RoleInfo {
        @Schema(description = "角色ID", example = "1234567890")
        private String roleId;

        @Schema(description = "角色编码", example = "STORE_MANAGER")
        private String roleCode;

        @Schema(description = "角色名称", example = "店长角色")
        private String roleName;

        @Schema(description = "是否主角色", example = "true")
        private Boolean isPrimary;

        // Getter and Setter
        public String getRoleId() {
            return roleId;
        }

        public void setRoleId(String roleId) {
            this.roleId = roleId;
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

        public Boolean getIsPrimary() {
            return isPrimary;
        }

        public void setIsPrimary(Boolean isPrimary) {
            this.isPrimary = isPrimary;
        }
    }

    /**
     * 数据权限信息
     */
    @Schema(description = "数据权限信息")
    public static class DataScopeInfo {
        @Schema(description = "权限范围类型", example = "store")
        private String scopeType;

        @Schema(description = "权限范围ID", example = "1234567890")
        private String scopeId;

        @Schema(description = "权限范围名称", example = "中心店")
        private String scopeName;

        @Schema(description = "权限来源", example = "auto")
        private String source;

        // Getter and Setter
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
    }

    // Getter and Setter methods
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

    public List<RoleInfo> getAssignedRoles() {
        return assignedRoles;
    }

    public void setAssignedRoles(List<RoleInfo> assignedRoles) {
        this.assignedRoles = assignedRoles;
    }

    public List<DataScopeInfo> getAssignedDataScopes() {
        return assignedDataScopes;
    }

    public void setAssignedDataScopes(List<DataScopeInfo> assignedDataScopes) {
        this.assignedDataScopes = assignedDataScopes;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
