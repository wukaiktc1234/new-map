package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.annotation.RequiresPermission;
import com.foodtraceability.common.Result;
import com.foodtraceability.entity.Role;
import com.foodtraceability.entity.StatusCount;
import com.foodtraceability.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import com.foodtraceability.dto.RoleDataScopeDTO;
import com.foodtraceability.dto.RolePermissionAssignDTO;
import com.foodtraceability.dto.RoleStatusUpdateDTO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 角色管理控制器
 * 处理角色管理相关的HTTP请求
 *
 * 安全修复说明：
 * - 原实现所有接口均无权限控制，存在严重安全风险（CVSS: 8.5）
 * - 已为所有接口添加 @RequiresPermission 注解
 * - 权限码格式：system:role:{action}
 */
@RestController
@RequestMapping("/v1/roles")
@Tag(name = "角色管理")
public class RoleController {


    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    private final RoleService roleService;

    /**
     * 创建角色
     * 权限要求：system:role:create
     */
    @PostMapping
    @Operation(summary = "创建角色")
    @RequiresPermission(value = "system:role:create", action = "create")
    public Result<Role> createRole(@RequestBody Role role) {
        try {
            Role createdRole = roleService.createRole(role);
            return Result.success(createdRole, "创建角色成功");
        } catch (Exception e) {
            return Result.error(500, "创建角色失败：" + e.getMessage());
        }
    }

    /**
     * 更新角色
     * 权限要求：system:role:update
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新角色")
    @RequiresPermission(value = "system:role:update", action = "update")
    public Result<Role> updateRole(@Parameter(description = "角色ID") @PathVariable Long id, @RequestBody Role role) {
        try {
            role.setRoleId(id);
            Role updatedRole = roleService.updateRole(role);
            return Result.success(updatedRole, "更新角色成功");
        } catch (Exception e) {
            return Result.error(500, "更新角色失败：" + e.getMessage());
        }
    }

    /**
     * 删除角色
     * 权限要求：system:role:delete
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除角色")
    @RequiresPermission(value = "system:role:delete", action = "delete")
    public Result<Void> deleteRole(@Parameter(description = "角色ID") @PathVariable Long id) {
        try {
            boolean success = roleService.deleteRole(id);
            if (!success) {
                return Result.error(404, "角色不存在");
            }
            return Result.success(null, "删除角色成功");
        } catch (Exception e) {
            return Result.error(500, "删除角色失败：" + e.getMessage());
        }
    }

    /**
     * 根据ID查询角色
     * 权限要求：system:role:read
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询角色")
    @RequiresPermission(value = "system:role:read", action = "read")
    public Result<Role> getRoleById(@Parameter(description = "角色ID") @PathVariable Long id) {
        try {
            Role role = roleService.getRoleById(id);
            if (role == null) {
                return Result.error(404, "角色不存在");
            }
            return Result.success(role, "查询角色成功");
        } catch (Exception e) {
            return Result.error(500, "查询角色失败：" + e.getMessage());
        }
    }

    /**
     * 根据角色编码查询角色
     * 权限要求：system:role:read
     */
    @GetMapping("/code/{code}")
    @Operation(summary = "根据角色编码查询角色")
    @RequiresPermission(value = "system:role:read", action = "read")
    public Result<Role> getRoleByCode(@Parameter(description = "角色编码") @PathVariable String code) {
        try {
            Role role = roleService.getRoleByCode(code);
            if (role == null) {
                return Result.error(404, "角色不存在");
            }
            return Result.success(role, "查询角色成功");
        } catch (Exception e) {
            return Result.error(500, "查询角色失败：" + e.getMessage());
        }
    }

    /**
     * 分页查询角色列表
     * 权限要求：system:role:read
     */
    @GetMapping("/list")
    @Operation(summary = "分页查询角色列表")
    @RequiresPermission(value = "system:role:read", action = "read")
    public Result<IPage<Role>> getRoleList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "角色编码") @RequestParam(required = false) String roleCode,
            @Parameter(description = "角色名称") @RequestParam(required = false) String roleName,
            @Parameter(description = "状态（active-启用，inactive-禁用）") @RequestParam(required = false) String status,
            @Parameter(description = "角色类型（1-系统角色，2-自定义角色）") @RequestParam(required = false) Integer roleType) {
        try {
            Page<Role> pageParam = new Page<>(page, pageSize);
            IPage<Role> rolePage = roleService.getRolePage(pageParam, roleCode, roleName, status, roleType);
            return Result.success(rolePage, "查询角色列表成功");
        } catch (Exception e) {
            return Result.error(500, "查询角色列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取所有角色列表
     * 权限要求：system:role:read
     */
    @GetMapping("/all")
    @Operation(summary = "获取所有角色列表")
    @RequiresPermission(value = "system:role:read", action = "read")
    public Result<List<Role>> getAllRoles() {
        try {
            List<Role> roles = roleService.getAllRoles();
            return Result.success(roles, "获取角色列表成功");
        } catch (Exception e) {
            return Result.error(500, "获取角色列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取活跃角色列表
     * 权限要求：system:role:read
     */
    @GetMapping("/active")
    @Operation(summary = "获取活跃角色列表")
    @RequiresPermission(value = "system:role:read", action = "read")
    public Result<List<Role>> getActiveRoles() {
        try {
            List<Role> roles = roleService.getActiveRoles();
            return Result.success(roles, "获取活跃角色列表成功");
        } catch (Exception e) {
            return Result.error(500, "获取活跃角色列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取系统内置角色
     * 权限要求：system:role:read
     */
    @GetMapping("/system")
    @Operation(summary = "获取系统内置角色")
    @RequiresPermission(value = "system:role:read", action = "read")
    public Result<List<Role>> getSystemRoles() {
        try {
            List<Role> roles = roleService.getSystemRoles();
            return Result.success(roles, "获取系统角色列表成功");
        } catch (Exception e) {
            return Result.error(500, "获取系统角色列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取自定义角色
     * 权限要求：system:role:read
     */
    @GetMapping("/custom")
    @Operation(summary = "获取自定义角色")
    @RequiresPermission(value = "system:role:read", action = "read")
    public Result<List<Role>> getCustomRoles() {
        try {
            List<Role> roles = roleService.getCustomRoles();
            return Result.success(roles, "获取自定义角色列表成功");
        } catch (Exception e) {
            return Result.error(500, "获取自定义角色列表失败：" + e.getMessage());
        }
    }

    /**
     * 切换角色状态
     * 权限要求：system:role:update-status
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "切换角色状态")
    @RequiresPermission(value = "system:role:update-status", action = "update-status")
    public Result<Void> toggleRoleStatus(@Parameter(description = "角色ID") @PathVariable Long id,
                                          @Valid @RequestBody RoleStatusUpdateDTO dto) {
        try {
            Object statusObj = dto.getStatus();
            if (statusObj == null) {
                return Result.error(400, "状态不能为空");
            }

            // 统一转换为 active/inactive 字符串，匹配数据库 varchar 列
            String status;
            if (statusObj instanceof Integer) {
                status = ((Integer) statusObj) == 1 ? "active" : "inactive";
            } else if (statusObj instanceof String) {
                String statusStr = (String) statusObj;
                if ("active".equalsIgnoreCase(statusStr) || "inactive".equalsIgnoreCase(statusStr)) {
                    status = statusStr.toLowerCase();
                } else if ("1".equals(statusStr)) {
                    status = "active";
                } else if ("0".equals(statusStr)) {
                    status = "inactive";
                } else {
                    return Result.error(400, "状态值格式错误");
                }
            } else {
                return Result.error(400, "状态值类型错误");
            }

            boolean success = roleService.toggleRoleStatus(id, status);
            if (!success) {
                return Result.error(404, "角色不存在");
            }
            return Result.success(null, "切换角色状态成功");
        } catch (Exception e) {
            return Result.error(500, "切换角色状态失败：" + e.getMessage());
        }
    }

    /**
     * 为角色分配权限
     * 权限要求：system:role:assign-permissions
     */
    @PutMapping("/{id}/permissions")
    @Operation(summary = "为角色分配权限")
    @RequiresPermission(value = "system:role:assign-permissions", action = "assign-permissions")
    public Result<Void> assignPermissions(@Parameter(description = "角色ID") @PathVariable Long id,
                                           @Valid @RequestBody RolePermissionAssignDTO dto) {
        try {
            List<String> permissions = dto.getPermissions();
            if (permissions == null) {
                return Result.error(400, "权限列表不能为空");
            }
            boolean success = roleService.assignPermissions(id, permissions);
            if (!success) {
                return Result.error(404, "角色不存在");
            }
            return Result.success(null, "分配权限成功");
        } catch (Exception e) {
            return Result.error(500, "分配权限失败：" + e.getMessage());
        }
    }

    /**
     * 获取角色的权限列表
     * 权限要求：system:role:read
     */
    @GetMapping("/{id}/permissions")
    @Operation(summary = "获取角色的权限列表")
    @RequiresPermission(value = "system:role:read", action = "read")
    public Result<List<String>> getRolePermissions(@Parameter(description = "角色ID") @PathVariable Long id) {
        try {
            List<String> permissions = roleService.getRolePermissions(id);
            return Result.success(permissions, "获取角色权限成功");
        } catch (Exception e) {
            return Result.error(500, "获取角色权限失败：" + e.getMessage());
        }
    }

    /**
     * 设置角色的数据权限范围
     * 权限要求：system:role:set-data-scope
     */
    @PutMapping("/{id}/data-scope")
    @Operation(summary = "设置角色的数据权限范围")
    @RequiresPermission(value = "system:role:set-data-scope", action = "set-data-scope")
    public Result<Void> setDataScope(@Parameter(description = "角色ID") @PathVariable Long id,
                                      @Valid @RequestBody RoleDataScopeDTO dto) {
        try {
            String dataScope = dto.getDataScope();
            List<String> accessibleStores = dto.getAccessibleStores();
            List<String> accessibleDepartments = dto.getAccessibleDepartments();

            boolean success = roleService.setDataScope(id, dataScope, accessibleStores, accessibleDepartments);
            if (!success) {
                return Result.error(404, "角色不存在");
            }
            return Result.success(null, "设置数据权限范围成功");
        } catch (Exception e) {
            return Result.error(500, "设置数据权限范围失败：" + e.getMessage());
        }
    }

    /**
     * 获取角色统计信息
     * 权限要求：system:role:read
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取角色统计信息")
    @RequiresPermission(value = "system:role:read", action = "read")
    public Result<Map<String, Object>> getStatistics() {
        try {
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("total", roleService.countRoles());
            statistics.put("byStatus", roleService.countByStatus());
            statistics.put("byRoleType", roleService.countByRoleType());
            return Result.success(statistics, "获取统计信息成功");
        } catch (Exception e) {
            return Result.error(500, "获取统计信息失败：" + e.getMessage());
        }
    }

    /**
     * 检查角色编码是否已存在
     * 权限要求：system:role:read
     */
    @GetMapping("/check-code")
    @Operation(summary = "检查角色编码是否已存在")
    @RequiresPermission(value = "system:role:read", action = "read")
    public Result<Boolean> checkRoleCode(@Parameter(description = "角色编码") @RequestParam String roleCode) {
        try {
            boolean exists = roleService.checkRoleCodeExists(roleCode);
            return Result.success(!exists, exists ? "角色编码已存在" : "角色编码可用");
        } catch (Exception e) {
            return Result.error(500, "检查角色编码失败：" + e.getMessage());
        }
    }

    /**
     * 检查角色名称是否已存在
     * 权限要求：system:role:read
     */
    @GetMapping("/check-name")
    @Operation(summary = "检查角色名称是否已存在")
    @RequiresPermission(value = "system:role:read", action = "read")
    public Result<Boolean> checkRoleName(@Parameter(description = "角色名称") @RequestParam String roleName) {
        try {
            boolean exists = roleService.checkRoleNameExists(roleName);
            return Result.success(!exists, exists ? "角色名称已存在" : "角色名称可用");
        } catch (Exception e) {
            return Result.error(500, "检查角色名称失败：" + e.getMessage());
        }
    }
}
