package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.entity.UserPermissionOverride;
import com.foodtraceability.service.UserPermissionOverrideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户权限覆盖控制器
 *
 * <p>用于在角色权限之外，为特定用户临时添加或移除某项权限。
 * 所有写操作需要 system:permission:manage 权限或通配权限 '*'。</p>
 */
@RestController
@RequestMapping("/v1/user-permission-overrides")
@Tag(name = "用户权限覆盖")
public class UserPermissionOverrideController {

    private static final Logger log = LoggerFactory.getLogger(UserPermissionOverrideController.class);

    private final UserPermissionOverrideService userPermissionOverrideService;

    public UserPermissionOverrideController(UserPermissionOverrideService userPermissionOverrideService) {
        this.userPermissionOverrideService = userPermissionOverrideService;
    }

    /**
     * 分页查询用户权限覆盖
     */
    @GetMapping
    @Operation(summary = "分页查询用户权限覆盖")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @PreAuthorize("hasAuthority('system:permission:manage') or hasAuthority('system:permission:read') or hasAuthority('*')")
    public Result<IPage<UserPermissionOverride>> getPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "用户ID") @RequestParam(required = false) String userId,
            @Parameter(description = "用户姓名") @RequestParam(required = false) String userName,
            @Parameter(description = "权限码") @RequestParam(required = false) String permissionCode,
            @Parameter(description = "业务域") @RequestParam(required = false) String domainCode,
            @Parameter(description = "覆盖类型") @RequestParam(required = false) String overrideType,
            @Parameter(description = "状态") @RequestParam(required = false) String status) {
        try {
            Page<UserPermissionOverride> pageParam = new Page<>(page, size);
            IPage<UserPermissionOverride> result = userPermissionOverrideService.getPage(
                    pageParam, userId, userName, permissionCode, domainCode, overrideType, status);
            return Result.success(result, "分页查询用户权限覆盖成功");
        } catch (Exception e) {
            log.error("分页查询用户权限覆盖失败", e);
            return Result.error(500, "分页查询用户权限覆盖失败");
        }
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询用户权限覆盖")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @PreAuthorize("hasAuthority('system:permission:manage') or hasAuthority('system:permission:read') or hasAuthority('*')")
    public Result<UserPermissionOverride> getById(@Parameter(description = "覆盖ID") @PathVariable Long id) {
        try {
            UserPermissionOverride override = userPermissionOverrideService.getById(id);
            if (override == null) {
                return Result.error(404, "覆盖记录不存在");
            }
            return Result.success(override, "查询成功");
        } catch (Exception e) {
            log.error("查询用户权限覆盖失败: id={}", id, e);
            return Result.error(500, "查询用户权限覆盖失败");
        }
    }

    /**
     * 根据用户ID查询所有覆盖
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "根据用户ID查询所有覆盖")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @PreAuthorize("hasAuthority('system:permission:manage') or hasAuthority('system:permission:read') or hasAuthority('*')")
    public Result<List<UserPermissionOverride>> getByUserId(@Parameter(description = "用户ID") @PathVariable String userId) {
        try {
            List<UserPermissionOverride> list = userPermissionOverrideService.getByUserId(userId);
            return Result.success(list, "查询成功");
        } catch (Exception e) {
            log.error("根据用户ID查询覆盖失败: userId={}", userId, e);
            return Result.error(500, "查询用户覆盖失败");
        }
    }

    /**
     * 创建覆盖
     */
    @PostMapping
    @Operation(summary = "创建用户权限覆盖")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @PreAuthorize("hasAuthority('system:permission:manage') or hasAuthority('*')")
    public Result<UserPermissionOverride> create(@RequestBody UserPermissionOverride override) {
        try {
            if (override.getUserId() == null || override.getUserId().trim().isEmpty()) {
                return Result.error(400, "用户ID不能为空");
            }
            if (override.getPermissionCode() == null || override.getPermissionCode().trim().isEmpty()) {
                return Result.error(400, "权限码不能为空");
            }
            if (override.getOverrideType() == null || override.getOverrideType().trim().isEmpty()) {
                return Result.error(400, "覆盖类型不能为空");
            }
            UserPermissionOverride created = userPermissionOverrideService.create(override);
            return Result.success(created, "创建成功，等待审批");
        } catch (Exception e) {
            log.error("创建用户权限覆盖失败", e);
            return Result.error(500, "创建用户权限覆盖失败");
        }
    }

    /**
     * 更新覆盖
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新用户权限覆盖")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @PreAuthorize("hasAuthority('system:permission:manage') or hasAuthority('*')")
    public Result<UserPermissionOverride> update(
            @Parameter(description = "覆盖ID") @PathVariable Long id,
            @RequestBody UserPermissionOverride override) {
        try {
            UserPermissionOverride updated = userPermissionOverrideService.update(id, override);
            return Result.success(updated, "更新成功");
        } catch (IllegalArgumentException e) {
            return Result.error(404, e.getMessage());
        } catch (Exception e) {
            log.error("更新用户权限覆盖失败: id={}", id, e);
            return Result.error(500, "更新用户权限覆盖失败");
        }
    }

    /**
     * 删除覆盖（逻辑删除）
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户权限覆盖")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @PreAuthorize("hasAuthority('system:permission:manage') or hasAuthority('*')")
    public Result<Void> delete(@Parameter(description = "覆盖ID") @PathVariable Long id) {
        try {
            boolean ok = userPermissionOverrideService.delete(id);
            if (ok) {
                return Result.success(null, "删除成功");
            }
            return Result.error(500, "删除失败");
        } catch (Exception e) {
            log.error("删除用户权限覆盖失败: id={}", id, e);
            return Result.error(500, "删除用户权限覆盖失败");
        }
    }

    /**
     * 审批覆盖
     */
    @PostMapping("/{id}/approve")
    @Operation(summary = "审批用户权限覆盖")
    @ApiResponse(responseCode = "200", description = "审批成功")
    @PreAuthorize("hasAuthority('system:permission:manage') or hasAuthority('*')")
    public Result<UserPermissionOverride> approve(
            @Parameter(description = "覆盖ID") @PathVariable Long id,
            @Parameter(description = "审批人ID") @RequestParam String approverId,
            @Parameter(description = "审批人姓名") @RequestParam String approverName,
            @Parameter(description = "是否通过：true-通过，false-拒绝") @RequestParam boolean approved) {
        try {
            UserPermissionOverride result = userPermissionOverrideService.approve(id, approverId, approverName, approved);
            return Result.success(result, approved ? "审批通过" : "已拒绝");
        } catch (IllegalArgumentException e) {
            return Result.error(404, e.getMessage());
        } catch (IllegalStateException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("审批用户权限覆盖失败: id={}", id, e);
            return Result.error(500, "审批失败");
        }
    }

    /**
     * 撤销覆盖
     */
    @PostMapping("/{id}/revoke")
    @Operation(summary = "撤销用户权限覆盖")
    @ApiResponse(responseCode = "200", description = "撤销成功")
    @PreAuthorize("hasAuthority('system:permission:manage') or hasAuthority('*')")
    public Result<UserPermissionOverride> revoke(@Parameter(description = "覆盖ID") @PathVariable Long id) {
        try {
            UserPermissionOverride result = userPermissionOverrideService.revoke(id);
            return Result.success(result, "撤销成功");
        } catch (IllegalArgumentException e) {
            return Result.error(404, e.getMessage());
        } catch (IllegalStateException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            log.error("撤销用户权限覆盖失败: id={}", id, e);
            return Result.error(500, "撤销失败");
        }
    }

    /**
     * 刷新过期状态（admin only）
     */
    @PostMapping("/refresh-expired")
    @Operation(summary = "刷新过期状态")
    @ApiResponse(responseCode = "200", description = "刷新成功")
    @PreAuthorize("hasAuthority('*')")
    public Result<Boolean> refreshExpired() {
        try {
            boolean ok = userPermissionOverrideService.refreshExpired();
            return Result.success(ok, "刷新过期状态成功");
        } catch (Exception e) {
            log.error("刷新过期状态失败", e);
            return Result.error(500, "刷新过期状态失败");
        }
    }
}
