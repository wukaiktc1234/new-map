package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 权限分配请求DTO
 */
@Schema(description = "权限分配请求DTO")
public class PermissionAssignmentDTO {

    /**
     * 员工ID
     */
    @NotBlank(message = "员工ID不能为空")
    @Schema(description = "员工ID", example = "1234567890", required = true)
    private String employeeId;

    /**
     * 职位ID
     */
    @Schema(description = "职位ID", example = "1234567890")
    private String positionId;

    /**
     * 门店ID
     */
    @Schema(description = "门店ID", example = "1234567890")
    private String storeId;

    /**
     * 部门ID
     */
    @Schema(description = "部门ID", example = "1234567890")
    private String departmentId;

    /**
     * 操作类型
     */
    @NotBlank(message = "操作类型不能为空")
    @Schema(description = "操作类型（onboard:入职, position_change:职位变更, store_assign:门店分配, manual:手动调整）", example = "onboard", required = true)
    private String operationType;

    /**
     * 是否强制覆盖手动分配的权限
     */
    @Schema(description = "是否强制覆盖手动分配的权限", example = "false")
    private Boolean forceOverride = false;

    /**
     * 备注
     */
    @Schema(description = "备注", example = "员工入职自动分配权限")
    private String remark;

    // Getter and Setter methods
    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getPositionId() {
        return positionId;
    }

    public void setPositionId(String positionId) {
        this.positionId = positionId;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public Boolean getForceOverride() {
        return forceOverride;
    }

    public void setForceOverride(Boolean forceOverride) {
        this.forceOverride = forceOverride;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
