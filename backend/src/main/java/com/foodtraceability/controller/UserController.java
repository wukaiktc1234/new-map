package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.annotation.RequiresPermission;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.UserAssignRolesDTO;
import com.foodtraceability.dto.UserAssignStoreDTO;
import com.foodtraceability.dto.UserBasicInfo;
import com.foodtraceability.dto.UserCreateDTO;
import com.foodtraceability.dto.UserResetPasswordDTO;
import com.foodtraceability.dto.UserStatusToggleDTO;
import com.foodtraceability.dto.UserUpdateDTO;
import com.foodtraceability.entity.Store;
import com.foodtraceability.entity.User;
import com.foodtraceability.service.StoreService;
import com.foodtraceability.service.UserDataService;
import com.foodtraceability.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户管理控制器
 *
 * 安全修复说明：
 * - 原实现部分接口缺少权限控制
 * - 已统一使用 @RequiresPermission 注解保护所有敏感接口
 * - 权限码格式：system:user:{action}
 * - 公开接口：用户名/邮箱/手机号唯一性检查
 */
@RestController
@RequestMapping("/v1/users")
@Tag(name = "用户管理")
public class UserController {


    public UserController(UserService userService, StoreService storeService, UserDataService userDataService) {
        this.userService = userService;
        this.storeService = storeService;
        this.userDataService = userDataService;
    }

    private final UserService userService;

    private final StoreService storeService;

    private final UserDataService userDataService;

    @PostMapping
    @Operation(summary = "创建用户")
    @RequiresPermission(value = "system:user:create", action = "create")
    public Result<User> createUser(@Valid @RequestBody UserCreateDTO userDTO) {
        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setPassword(userDTO.getPassword());
        user.setFullName(userDTO.getFullName());
        user.setEmail(userDTO.getEmail());
        user.setPhone(userDTO.getPhone());
        user.setStatus(userDTO.getStatus());
        user.setDepartmentId(userDTO.getDepartmentId());
        user.setStoreId(userDTO.getStoreId());
        user.setEmployeeCode(userDTO.getEmployeeCode());
        user.setAvatar(userDTO.getAvatar());
        User createdUser = userService.createUser(user);
        return Result.success(createdUser, "创建用户成功");
    }

    @GetMapping("/list")
    @Operation(summary = "分页查询用户列表")
    @RequiresPermission(value = "system:user:read", action = "read")
    public Result<IPage<User>> getUserList(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "用户名") @RequestParam(required = false) String username,
            @Parameter(description = "姓名") @RequestParam(required = false) String fullName,
            @Parameter(description = "邮箱") @RequestParam(required = false) String email,
            @Parameter(description = "手机号") @RequestParam(required = false) String phone,
            @Parameter(description = "角色") @RequestParam(required = false) String role,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "部门") @RequestParam(required = false) String department,
            @Parameter(description = "开始时间") @RequestParam(required = false) String startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) String endTime) {
        Page<User> pageParam = new Page<>(page, pageSize);
        IPage<User> userPage = userService.getUserPage(pageParam, username, fullName, email, phone,
                role, status, department, startTime, endTime);
        return Result.success(userPage, "查询用户列表成功");
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询用户")
    @RequiresPermission(value = "system:user:read", action = "read")
    public Result<User> getUserById(@Parameter(description = "用户ID") @PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        return Result.success(user, "查询用户成功");
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新用户信息")
    @RequiresPermission(value = "system:user:update", action = "update")
    public Result<User> updateUser(@Parameter(description = "用户ID") @PathVariable Long id, @Valid @RequestBody UserUpdateDTO userDTO) {
        User existingUser = userService.getById(id);
        if (existingUser == null) {
            return Result.error(404, "用户不存在");
        }
        User user = new User();
        user.setId(id);
        if (userDTO.getFullName() != null) {
            user.setFullName(userDTO.getFullName());
        }
        if (userDTO.getEmail() != null) {
            user.setEmail(userDTO.getEmail());
        }
        if (userDTO.getPhone() != null) {
            user.setPhone(userDTO.getPhone());
        }
        if (userDTO.getStatus() != null) {
            user.setStatus(userDTO.getStatus());
        }
        if (userDTO.getDepartmentId() != null) {
            user.setDepartmentId(userDTO.getDepartmentId());
        }
        if (userDTO.getStoreId() != null) {
            user.setStoreId(userDTO.getStoreId());
        }
        if (userDTO.getEmployeeCode() != null) {
            user.setEmployeeCode(userDTO.getEmployeeCode());
        }
        if (userDTO.getAvatar() != null) {
            user.setAvatar(userDTO.getAvatar());
        }
        User updatedUser = userService.updateUser(user);
        return Result.success(updatedUser, "更新用户成功");
    }

    @PutMapping("/{id}/reset-password")
    @Operation(summary = "重置用户密码")
    @RequiresPermission(value = "system:user:reset-password", action = "reset-password")
    public Result<Void> resetPassword(@Parameter(description = "用户ID") @PathVariable Long id,
                                       @Valid @RequestBody UserResetPasswordDTO request) {
        String newPassword = request.getNewPassword();
        boolean success = userService.resetPassword(id, newPassword);
        if (!success) {
            return Result.error(404, "用户不存在");
        }
        return Result.success(null, "重置密码成功");
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "切换用户状态")
    @RequiresPermission(value = "system:user:update-status", action = "update-status")
    public Result<Void> toggleUserStatus(@Parameter(description = "用户ID") @PathVariable Long id,
                                           @Valid @RequestBody UserStatusToggleDTO request) {
        Integer status = request.getStatus();
        boolean success = userService.toggleUserStatus(id, status);
        if (!success) {
            return Result.error(404, "用户不存在");
        }
        return Result.success(null, "切换用户状态成功");
    }

    @PutMapping("/{id}/assign-store")
    @Operation(summary = "分配门店给用户")
    @RequiresPermission(value = "system:user:assign-store", action = "assign-store")
    public Result<Void> assignStore(@Parameter(description = "用户ID") @PathVariable Long id,
                                     @Valid @RequestBody UserAssignStoreDTO request) {
        Long storeId = request.getStoreId();
        boolean success = userService.assignStore(id, storeId);
        if (!success) {
            return Result.error(404, "用户不存在");
        }
        return Result.success(null, "分配门店成功");
    }

    @DeleteMapping("/{id}/assign-store")
    @Operation(summary = "取消用户的门店分配")
    @RequiresPermission(value = "system:user:unassign-store", action = "unassign-store")
    public Result<Void> unassignStore(@Parameter(description = "用户ID") @PathVariable Long id) {
        boolean success = userService.unassignStore(id);
        if (!success) {
            return Result.error(404, "用户不存在");
        }
        return Result.success(null, "取消门店分配成功");
    }

    @GetMapping("/{id}/store-info")
    @Operation(summary = "获取用户的门店信息")
    @RequiresPermission(value = "system:user:read", action = "read")
    public Result<Map<String, Object>> getUserStoreInfo(@Parameter(description = "用户ID") @PathVariable Long id) {
        Map<String, Object> storeInfo = userService.getUserStoreInfo(id);
        if (storeInfo == null) {
            return Result.error(404, "用户不存在");
        }
        return Result.success(storeInfo, "获取用户门店信息成功");
    }

    @GetMapping("/stores")
    @Operation(summary = "获取所有活跃门店列表")
    @RequiresPermission(value = "system:user:read", action = "read")
    public Result<List<Store>> getActiveStores() {
        List<Store> stores = storeService.getActiveStores();
        return Result.success(stores, "获取门店列表成功");
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取用户统计信息")
    @RequiresPermission(value = "system:user:read", action = "read")
    public Result<Map<String, Object>> getStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("total", userService.countUsers());
        statistics.put("byStatus", userService.countByStatus());
        statistics.put("byRole", userService.countByRole());
        statistics.put("byDepartment", userService.countByDepartment());
        return Result.success(statistics, "获取统计信息成功");
    }

    /**
     * 检查用户名是否已存在（公开接口，无需权限）
     * 用于前端表单验证，允许未登录用户调用
     */
    @GetMapping("/check-username")
    @Operation(summary = "检查用户名是否已存在")
    public Result<Boolean> checkUsername(@Parameter(description = "用户名") @RequestParam String username) {
        boolean exists = userService.checkUsernameExists(username);
        return Result.success(!exists, exists ? "用户名已存在" : "用户名可用");
    }

    /**
     * 检查邮箱是否已存在（公开接口，无需权限）
     * 用于前端表单验证，允许未登录用户调用
     */
    @GetMapping("/check-email")
    @Operation(summary = "检查邮箱是否已存在")
    public Result<Boolean> checkEmail(@Parameter(description = "邮箱") @RequestParam String email) {
        boolean exists = userService.checkEmailExists(email);
        return Result.success(!exists, exists ? "邮箱已存在" : "邮箱可用");
    }

    /**
     * 检查手机号是否已存在（公开接口，无需权限）
     * 用于前端表单验证，允许未登录用户调用
     */
    @GetMapping("/check-phone")
    @Operation(summary = "检查手机号是否已存在")
    public Result<Boolean> checkPhone(@Parameter(description = "手机号") @RequestParam String phone) {
        boolean exists = userService.checkPhoneExists(phone);
        return Result.success(!exists, exists ? "手机号已存在" : "手机号可用");
    }

    @PutMapping("/{id}/assign-roles")
    @Operation(summary = "分配角色给用户")
    @RequiresPermission(value = "system:user:assign-roles", action = "assign-roles")
    public Result<Void> assignRoles(@Parameter(description = "用户ID") @PathVariable Long id,
                                     @Valid @RequestBody UserAssignRolesDTO request) {
        List<String> roleIds = request.getRoleIds();
        boolean success = userService.assignRoles(id, roleIds);
        if (!success) {
            return Result.error(404, "用户不存在");
        }
        return Result.success(null, "分配角色成功");
    }

    @DeleteMapping("/{id}/assign-roles")
    @Operation(summary = "取消用户的角色分配")
    @RequiresPermission(value = "system:user:unassign-roles", action = "unassign-roles")
    public Result<Void> unassignRoles(@Parameter(description = "用户ID") @PathVariable Long id) {
        boolean success = userService.unassignRoles(id);
        if (!success) {
            return Result.error(404, "用户不存在");
        }
        return Result.success(null, "取消角色分配成功");
    }

    @GetMapping("/{id}/roles")
    @Operation(summary = "获取用户的角色列表")
    @RequiresPermission(value = "system:user:read", action = "read")
    public Result<List<String>> getUserRoles(@Parameter(description = "用户ID") @PathVariable Long id) {
        List<String> roleIds = userService.getUserRoles(id);
        return Result.success(roleIds, "获取用户角色成功");
    }

    @GetMapping("/{id}/role-names")
    @Operation(summary = "获取用户的角色名称列表")
    @RequiresPermission(value = "system:user:read", action = "read")
    public Result<List<String>> getUserRoleNames(@Parameter(description = "用户ID") @PathVariable Long id) {
        List<String> roleNames = userService.getUserRoleNames(id);
        return Result.success(roleNames, "获取用户角色名称成功");
    }

    @GetMapping("/{id}/has-role")
    @Operation(summary = "检查用户是否拥有指定角色")
    @RequiresPermission(value = "system:user:read", action = "read")
    public Result<Boolean> hasRole(@Parameter(description = "用户ID") @PathVariable Long id,
                                    @Parameter(description = "角色编码") @RequestParam String roleCode) {
        boolean hasRole = userService.hasRole(id, roleCode);
        return Result.success(hasRole, hasRole ? "用户拥有该角色" : "用户不拥有该角色");
    }

    @GetMapping("/{id}/has-permission")
    @Operation(summary = "检查用户是否拥有指定权限")
    @RequiresPermission(value = "system:user:read", action = "read")
    public Result<Boolean> hasPermission(@Parameter(description = "用户ID") @PathVariable Long id,
                                           @Parameter(description = "权限编码") @RequestParam String permissionCode) {
        boolean hasPermission = userService.hasPermission(id, permissionCode);
        return Result.success(hasPermission, hasPermission ? "用户拥有该权限" : "用户不拥有该权限");
    }

    @PostMapping("/batch/basic-info")
    @Operation(summary = "批量获取用户基本信息")
    @RequiresPermission(value = "system:user:read", action = "read")
    public Result<Map<String, UserBasicInfo>> batchGetUserBasicInfo(@RequestBody List<String> userIds) {
        Map<String, UserBasicInfo> basicInfoMap = userDataService.batchGetUserBasicInfo(userIds);
        return Result.success(basicInfoMap, "批量获取用户基本信息成功");
    }

    @GetMapping("/basic-info/{userId}")
    @Operation(summary = "获取单个用户基本信息")
    @RequiresPermission(value = "system:user:read", action = "read")
    public Result<UserBasicInfo> getUserBasicInfo(@PathVariable String userId) {
        UserBasicInfo basicInfo = userDataService.getUserBasicInfo(userId);
        if (basicInfo == null) {
            return Result.error(404, "用户不存在");
        }
        return Result.success(basicInfo, "获取用户基本信息成功");
    }
}
