package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.annotation.RequiresPermission;
import com.foodtraceability.common.LogUtil;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.PermissionSearchDTO;
import com.foodtraceability.dto.PermissionSyncResult;
import com.foodtraceability.dto.PermissionTreeNode;
import com.foodtraceability.entity.Permission;
import com.foodtraceability.entity.StatusCount;
import com.foodtraceability.service.PermissionInitService;
import com.foodtraceability.service.PermissionService;
import com.foodtraceability.service.PermissionVerifyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 权限管理控制器
 *
 * 安全修复说明：
 * - 原实现写操作接口无权限控制，存在安全风险（CVSS: 7.2）
 * - 已为所有写操作添加 @RequiresPermission 注解
 * - 已为所有读操作添加 @RequiresPermission 注解（权限数据属于敏感安全配置）
 * - 权限码：system:permission:manage / system:permission:read
 */
@RestController
@RequestMapping("/v1/permissions")
@Tag(name = "权限管理")
public class PermissionController {

    private static final Logger log = LoggerFactory.getLogger(PermissionController.class);


    public PermissionController(PermissionService permissionService, PermissionInitService permissionInitService, PermissionVerifyService permissionVerifyService) {
        this.permissionService = permissionService;
        this.permissionInitService = permissionInitService;
        this.permissionVerifyService = permissionVerifyService;
    }

    private final PermissionService permissionService;

    private final PermissionInitService permissionInitService;

    private final PermissionVerifyService permissionVerifyService;

    /**
     * 获取权限列表
     */
    @GetMapping
    @Operation(summary = "获取权限列表")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @RequiresPermission(value = "system:permission:read", action = "read")
    public Result<List<Permission>> getPermissions() {
        try {
            List<Permission> permissions = permissionService.getByStatus("active");
            return Result.success(permissions, "获取权限列表成功");
        } catch (Exception e) {
            log.error("获取权限列表失败", e);
            return Result.error(500, "获取权限列表失败");
        }
    }

    /**
     * 统计各状态的权限数量
     */
    @GetMapping("/count-by-status")
    @Operation(summary = "统计各状态的权限数量")
    @ApiResponse(responseCode = "200", description = "统计成功")
    @RequiresPermission(value = "system:permission:read", action = "read")
    public Result<List<StatusCount>> countByStatus() {
        try {
            List<StatusCount> statusCounts = permissionService.getStatusCount();
            return Result.success(statusCounts, "统计各状态的权限数量成功");
        } catch (Exception e) {
            log.error("统计各状态的权限数量失败", e);
            return Result.error(500, "统计各状态的权限数量失败");
        }
    }

    /**
     * 分页查询权限
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询权限")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @RequiresPermission(value = "system:permission:read", action = "read")
    public Result<IPage<Permission>> getPermissionPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        try {
            Page<Permission> pageParam = new Page<>(page, size);
            IPage<Permission> permissionPage = permissionService.getPermissionPage(pageParam);
            return Result.success(permissionPage, "分页查询权限成功");
        } catch (Exception e) {
            log.error("分页查询权限失败", e);
            return Result.error(500, "分页查询权限失败");
        }
    }

    /**
     * 条件分页查询权限 - GET方式
     */
    @GetMapping("/search")
    @Operation(summary = "条件分页查询权限")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @RequiresPermission(value = "system:permission:read", action = "read")
    public Result<IPage<Permission>> searchPermissionsByGet(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "权限编码") @RequestParam(required = false) String permissionCode,
            @Parameter(description = "权限名称") @RequestParam(required = false) String permissionName,
            @Parameter(description = "权限类型") @RequestParam(required = false) String permissionType,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "所属模块") @RequestParam(required = false) String module) {
        try {
            // 记录API调用日志
            LogUtil.logApi("GET", "/permissions/search", "条件分页查询权限", page, size, permissionCode, permissionName, permissionType, status, module);

            Page<Permission> pageParam = new Page<>(page, size);
            IPage<Permission> permissionPage = permissionService.getPermissionPageByCondition(pageParam, permissionCode,
                                                                                             permissionName, permissionType, status, module, null, null, null);
            return Result.success(permissionPage, "条件分页查询权限成功");
        } catch (Exception e) {
            // 使用LogUtil记录错误，而不是简单日志记录
            String requestId = LogUtil.generateRequestId();
            LogUtil.logApiError("GET", "/permissions/search", "条件分页查询权限", e.getMessage(), requestId, page, size, permissionCode, permissionName, permissionType, status, module);
            // 按照要求，只提示失败，不做额外处理
            return Result.error(500, "API调用失败，请稍后重试");
        }
    }

    /**
     * 条件分页查询权限 - POST方式
     */
    @PostMapping("/search")
    @Operation(summary = "条件分页查询权限")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @RequiresPermission(value = "system:permission:read", action = "read")
    public Result<IPage<Permission>> searchPermissionsByPost(@Valid @RequestBody PermissionSearchDTO request) {
        try {
            // 记录API调用日志
            LogUtil.logApi("POST", "/permissions/search", "条件分页查询权限",
                         request.getPage(), request.getSize(),
                         request.getPermissionCode(), request.getPermissionName(),
                         request.getPermissionType(), request.getStatus(), request.getModule());

            Page<Permission> pageParam = new Page<>(request.getPage(), request.getSize());
            IPage<Permission> permissionPage = permissionService.getPermissionPageByCondition(pageParam,
                         request.getPermissionCode(), request.getPermissionName(),
                         request.getPermissionType(), request.getStatus(),
                         request.getModule(), null, null, null);
            return Result.success(permissionPage, "条件分页查询权限成功");
        } catch (Exception e) {
            // 使用LogUtil记录错误
            String requestId = LogUtil.generateRequestId();
            LogUtil.logApiError("POST", "/permissions/search", "条件分页查询权限",
                          e.getMessage(), requestId,
                          request.getPage(), request.getSize(),
                          request.getPermissionCode(), request.getPermissionName(),
                          request.getPermissionType(), request.getStatus(), request.getModule());
            // 按照要求，只提示失败，不做额外处理
            return Result.error(500, "API调用失败，请稍后重试");
        }
    }

    /**
     * 获取权限树结构
     */
    @GetMapping("/tree")
    @Operation(summary = "获取权限树结构")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @RequiresPermission(value = "system:permission:read", action = "read")
    public Result<List<Permission>> getPermissionTree() {
        try {
            List<Permission> permissions = permissionService.getPermissionTree();
            return Result.success(permissions, "获取权限树结构成功");
        } catch (Exception e) {
            log.error("获取权限树结构失败", e);
            return Result.error(500, "获取权限树结构失败");
        }
    }

    /**
     * 创建权限
     * 权限要求：system:permission:manage
     */
    @PostMapping
    @Operation(summary = "创建权限")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @RequiresPermission(value = "system:permission:create", action = "create")
    public Result<Permission> createPermission(@RequestBody Permission permission) {
        try {
            boolean success = permissionService.saveOrUpdatePermission(permission);
            if (success) {
                return Result.success(permission, "创建权限成功");
            } else {
                return Result.error(500, "创建权限失败");
            }
        } catch (Exception e) {
            log.error("创建权限失败", e);
            return Result.error(500, "创建权限失败");
        }
    }

    /**
     * 更新权限
     * 权限要求：system:permission:manage
     */
    @PutMapping("/{permissionId}")
    @Operation(summary = "更新权限")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @RequiresPermission(value = "system:permission:update", action = "update")
    public Result<Permission> updatePermission(
            @Parameter(description = "权限ID") @PathVariable String permissionId,
            @RequestBody Permission permission) {
        try {
            permission.setId(Long.valueOf(permissionId));
            boolean success = permissionService.saveOrUpdatePermission(permission);
            if (success) {
                return Result.success(permission, "更新权限成功");
            } else {
                return Result.error(500, "更新权限失败");
            }
        } catch (Exception e) {
            log.error("更新权限失败", e);
            return Result.error(500, "更新权限失败");
        }
    }

    /**
     * 删除权限
     * 权限要求：system:permission:manage
     */
    @DeleteMapping("/{permissionId}")
    @Operation(summary = "删除权限")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @RequiresPermission(value = "system:permission:delete", action = "delete")
    public Result<Void> deletePermission(@Parameter(description = "权限ID") @PathVariable String permissionId) {
        try {
            boolean success = permissionService.deletePermission(permissionId);
            if (success) {
                return Result.success(null, "删除权限成功");
            } else {
                return Result.error(500, "删除权限失败");
            }
        } catch (Exception e) {
            log.error("删除权限失败", e);
            return Result.error(500, "删除权限失败");
        }
    }

    /**
     * 更新权限状态
     * 权限要求：system:permission:manage
     */
    @PutMapping("/{permissionId}/status")
    @Operation(summary = "更新权限状态")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @RequiresPermission(value = "system:permission:update-status", action = "update-status")
    public Result<Void> updatePermissionStatus(
            @Parameter(description = "权限ID") @PathVariable String permissionId,
            @Parameter(description = "状态") @RequestParam String status) {
        try {
            Permission permission = permissionService.getById(permissionId);
            if (permission != null) {
                permission.setStatus(Integer.valueOf(status));
                boolean success = permissionService.saveOrUpdatePermission(permission);
                if (success) {
                    return Result.success(null, "更新权限状态成功");
                }
            }
            return Result.error(500, "更新权限状态失败");
        } catch (Exception e) {
            log.error("更新权限状态失败", e);
            return Result.error(500, "更新权限状态失败");
        }
    }

    /**
     * 更新权限排序
     * 权限要求：system:permission:manage
     */
    @PutMapping("/{permissionId}/sort-order")
    @Operation(summary = "更新权限排序")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @RequiresPermission(value = "system:permission:update-sort", action = "update-sort")
    public Result<Void> updatePermissionSort(
            @Parameter(description = "权限ID") @PathVariable String permissionId,
            @Parameter(description = "排序值") @RequestBody Map<String, Integer> sortOrder) {
        try {
            Permission permission = permissionService.getById(permissionId);
            if (permission != null) {
                permission.setSortOrder(sortOrder.get("sortOrder"));
                boolean success = permissionService.saveOrUpdatePermission(permission);
                if (success) {
                    return Result.success(null, "更新权限排序成功");
                }
            }
            return Result.error(500, "更新权限排序失败");
        } catch (Exception e) {
            log.error("更新权限排序失败", e);
            return Result.error(500, "更新权限排序失败");
        }
    }

    /**
     * 初始化权限数据
     */
    @PostMapping("/init")
    @Operation(summary = "初始化权限数据")
    @PreAuthorize("hasRole('admin')")
    public Result<Void> initPermissions() {
        try {
            permissionInitService.initPermissions();
            return Result.success(null, "权限初始化成功");
        } catch (Exception e) {
            log.error("权限初始化失败", e);
            return Result.error(500, "权限初始化失败：" + e.getMessage());
        }
    }

    /**
     * 同步权限数据
     */
    @PostMapping("/sync")
    @Operation(summary = "同步权限数据")
    @PreAuthorize("hasRole('admin')")
    public Result<PermissionSyncResult> syncPermissions() {
        try {
            PermissionSyncResult result = permissionInitService.syncPermissions();
            return Result.success(result, "权限同步成功");
        } catch (Exception e) {
            log.error("权限同步失败", e);
            return Result.error(500, "权限同步失败：" + e.getMessage());
        }
    }

    /**
     * 获取权限树（用于权限分配）
     */
    @GetMapping("/tree-nodes")
    @Operation(summary = "获取权限树节点")
    @RequiresPermission(value = "system:permission:read", action = "read")
    public Result<List<PermissionTreeNode>> getPermissionTreeNodes() {
        try {
            List<PermissionTreeNode> tree = permissionInitService.getPermissionTree();
            return Result.success(tree, "获取权限树成功");
        } catch (Exception e) {
            log.error("获取权限树失败", e);
            return Result.error(500, "获取权限树失败");
        }
    }

    /**
     * 获取角色权限树（含选中状态）
     */
    @GetMapping("/role/{roleId}/tree")
    @Operation(summary = "获取角色权限树")
    @RequiresPermission(value = "system:permission:read", action = "read")
    public Result<List<PermissionTreeNode>> getRolePermissionTree(
            @Parameter(description = "角色ID") @PathVariable String roleId) {
        try {
            List<PermissionTreeNode> tree = permissionInitService.getRolePermissionTree(roleId);
            return Result.success(tree, "获取角色权限树成功");
        } catch (Exception e) {
            log.error("获取角色权限树失败", e);
            return Result.error(500, "获取角色权限树失败");
        }
    }

    /**
     * 检查权限完整性
     */
    @GetMapping("/check-integrity")
    @Operation(summary = "检查权限完整性")
    @RequiresPermission(value = "system:permission:read", action = "read")
    public Result<Boolean> checkPermissionIntegrity() {
        try {
            boolean result = permissionInitService.checkPermissionIntegrity();
            return Result.success(result, result ? "权限数据完整" : "权限数据不完整");
        } catch (Exception e) {
            log.error("检查权限完整性失败", e);
            return Result.error(500, "检查权限完整性失败");
        }
    }

    /**
     * 刷新用户权限缓存
     */
    @PostMapping("/refresh-cache/{userId}")
    @Operation(summary = "刷新用户权限缓存")
    @PreAuthorize("hasRole('admin')")
    public Result<Void> refreshUserPermissionCache(
            @Parameter(description = "用户ID") @PathVariable String userId) {
        try {
            permissionVerifyService.refreshUserPermissionCache(userId);
            return Result.success(null, "刷新权限缓存成功");
        } catch (Exception e) {
            log.error("刷新权限缓存失败", e);
            return Result.error(500, "刷新权限缓存失败：" + e.getMessage());
        }
    }
}
