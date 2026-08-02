package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.PermissionAssignmentDTO;
import com.foodtraceability.dto.PermissionAssignmentResultDTO;
import com.foodtraceability.entity.EmployeeDataScope;
import com.foodtraceability.entity.PositionRoleMapping;
import com.foodtraceability.service.PermissionAutoAssignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 权限自动分配控制器
 */
@RestController
@RequestMapping("/v1/permission-auto-assign")
@Tag(name = "权限自动分配", description = "职位-角色权限自动分配管理接口")
public class PermissionAutoAssignController {


    public PermissionAutoAssignController(PermissionAutoAssignService permissionAutoAssignService) {
        this.permissionAutoAssignService = permissionAutoAssignService;
    }

    private final PermissionAutoAssignService permissionAutoAssignService;

    /**
     * 自动分配权限
     */
    @PostMapping("/assign")
    @PreAuthorize("hasAuthority('*') or hasAuthority('permission:assign')")
    @Operation(summary = "自动分配权限", description = "根据员工的职位、门店、部门信息自动分配角色和数据权限")
    public Result<PermissionAssignmentResultDTO> autoAssignPermission(
            @Valid @RequestBody PermissionAssignmentDTO dto) {
        PermissionAssignmentResultDTO result = permissionAutoAssignService.autoAssignPermission(dto);
        return Result.success(result);
    }

    /**
     * 员工入职权限分配
     */
    @PostMapping("/onboarding")
    @PreAuthorize("hasAuthority('*') or hasAuthority('permission:assign')")
    @Operation(summary = "员工入职权限分配", description = "员工入职时自动分配权限")
    public Result<PermissionAssignmentResultDTO> assignOnOnboarding(
            @Parameter(description = "员工ID", required = true) @RequestParam String employeeId,
            @Parameter(description = "职位ID", required = true) @RequestParam String positionId,
            @Parameter(description = "门店ID") @RequestParam(required = false) String storeId,
            @Parameter(description = "部门ID") @RequestParam(required = false) String departmentId,
            @Parameter(description = "操作人") @RequestParam(required = false) String operator) {
        PermissionAssignmentResultDTO result = permissionAutoAssignService.assignOnOnboarding(
                employeeId, positionId, storeId, departmentId, operator);
        return Result.success(result);
    }

    /**
     * 员工职位变更时自动调整权限
     */
    @PostMapping("/position-change")
    @PreAuthorize("hasAuthority('*') or hasAuthority('permission:assign')")
    @Operation(summary = "职位变更权限调整", description = "员工职位变更时自动调整权限")
    public Result<PermissionAssignmentResultDTO> assignOnPositionChange(
            @Parameter(description = "员工ID", required = true) @RequestParam String employeeId,
            @Parameter(description = "原职位ID", required = true) @RequestParam String oldPositionId,
            @Parameter(description = "新职位ID", required = true) @RequestParam String newPositionId,
            @Parameter(description = "操作人") @RequestParam(required = false) String operator) {
        PermissionAssignmentResultDTO result = permissionAutoAssignService.assignOnPositionChange(
                employeeId, oldPositionId, newPositionId, operator);
        return Result.success(result);
    }

    /**
     * 员工门店分配时自动调整数据权限
     */
    @PostMapping("/store-assign")
    @PreAuthorize("hasAuthority('*') or hasAuthority('permission:assign')")
    @Operation(summary = "门店分配权限调整", description = "员工门店分配时自动调整数据权限")
    public Result<PermissionAssignmentResultDTO> assignOnStoreAssignment(
            @Parameter(description = "员工ID", required = true) @RequestParam String employeeId,
            @Parameter(description = "门店ID", required = true) @RequestParam String storeId,
            @Parameter(description = "操作人") @RequestParam(required = false) String operator) {
        PermissionAssignmentResultDTO result = permissionAutoAssignService.assignOnStoreAssignment(
                employeeId, storeId, operator);
        return Result.success(result);
    }

    /**
     * 根据职位ID获取关联的角色列表
     */
    @GetMapping("/position/{positionId}/roles")
    @PreAuthorize("hasAuthority('*') or hasAuthority('permission:view')")
    @Operation(summary = "获取职位关联角色", description = "根据职位ID获取关联的角色列表")
    public Result<List<PositionRoleMapping>> getRolesByPositionId(
            @Parameter(description = "职位ID", required = true) @PathVariable Long positionId) {
        List<PositionRoleMapping> roles = permissionAutoAssignService.getRolesByPositionId(positionId);
        return Result.success(roles);
    }

    /**
     * 根据员工ID获取数据权限列表
     */
    @GetMapping("/employee/{employeeId}/data-scopes")
    @PreAuthorize("hasAuthority('*') or hasAuthority('permission:view')")
    @Operation(summary = "获取员工数据权限", description = "根据员工ID获取数据权限列表")
    public Result<List<EmployeeDataScope>> getDataScopesByEmployeeId(
            @Parameter(description = "员工ID", required = true) @PathVariable String employeeId) {
        List<EmployeeDataScope> dataScopes = permissionAutoAssignService.getDataScopesByEmployeeId(employeeId);
        return Result.success(dataScopes);
    }

    /**
     * 清除员工的自动分配权限
     */
    @DeleteMapping("/employee/{employeeId}/auto-permissions")
    @PreAuthorize("hasAuthority('*') or hasAuthority('permission:assign')")
    @Operation(summary = "清除自动分配权限", description = "清除员工的自动分配权限")
    public Result<Boolean> clearAutoAssignedPermissions(
            @Parameter(description = "员工ID", required = true) @PathVariable String employeeId,
            @Parameter(description = "操作人") @RequestParam(required = false) String operator) {
        boolean success = permissionAutoAssignService.clearAutoAssignedPermissions(employeeId, operator);
        return Result.success(success);
    }

    /**
     * 重新计算职位相关员工权限
     */
    @PostMapping("/position/{positionId}/recalculate")
    @PreAuthorize("hasAuthority('*') or hasAuthority('permission:assign')")
    @Operation(summary = "重新计算员工权限", description = "当职位-角色映射关系变更时，重新计算所有相关员工的权限")
    public Result<Integer> recalculatePermissionsByPosition(
            @Parameter(description = "职位ID", required = true) @PathVariable Long positionId,
            @Parameter(description = "操作人") @RequestParam(required = false) String operator) {
        int count = permissionAutoAssignService.recalculatePermissionsByPosition(positionId, operator);
        return Result.success(count);
    }
}
