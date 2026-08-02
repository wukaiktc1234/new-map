package com.foodtraceability.service;

import java.util.List;
import java.util.Set;

/**
 * 权限验证服务
 * 统一的权限验证逻辑
 */
public interface PermissionVerifyService {

    /**
     * 验证用户是否具有指定权限
     * @param userId 用户ID
     * @param permission 权限标识（格式：resource:action）
     * @return 是否有权限
     */
    boolean hasPermission(String userId, String permission);

    /**
     * 验证用户是否具有指定资源和操作的权限
     * @param userId 用户ID
     * @param resource 资源标识
     * @param action 操作类型
     * @return 是否有权限
     */
    boolean hasPermission(String userId, String resource, String action);

    /**
     * 验证用户是否具有任意一个权限
     * @param userId 用户ID
     * @param permissions 权限列表
     * @return 是否有任意一个权限
     */
    boolean hasAnyPermission(String userId, List<String> permissions);

    /**
     * 验证用户是否具有所有权限
     * @param userId 用户ID
     * @param permissions 权限列表
     * @return 是否有所有权限
     */
    boolean hasAllPermissions(String userId, List<String> permissions);

    /**
     * 获取用户的所有权限
     * @param userId 用户ID
     * @return 权限集合
     */
    Set<String> getUserPermissions(String userId);

    /**
     * 获取用户的所有角色
     * @param userId 用户ID
     * @return 角色集合
     */
    Set<String> getUserRoles(String userId);

    /**
     * 刷新用户权限缓存
     * @param userId 用户ID
     */
    void refreshUserPermissionCache(String userId);

    /**
     * 验证用户是否具有指定角色
     * @param userId 用户ID
     * @param roleCode 角色编码
     * @return 是否有该角色
     */
    boolean hasRole(String userId, String roleCode);

    /**
     * 验证用户是否具有任意一个角色
     * @param userId 用户ID
     * @param roleCodes 角色编码列表
     * @return 是否有任意一个角色
     */
    boolean hasAnyRole(String userId, List<String> roleCodes);

    /**
     * 验证用户是否具有所有角色
     * @param userId 用户ID
     * @param roleCodes 角色编码列表
     * @return 是否有所有角色
     */
    boolean hasAllRoles(String userId, List<String> roleCodes);

    /**
     * 检查用户是否为管理员
     * @param userId 用户ID
     * @return 是否为管理员
     */
    boolean isAdmin(String userId);

    /**
     * 获取用户的数据权限范围
     * @param userId 用户ID
     * @return 数据权限范围
     */
    String getUserDataScope(String userId);
}
