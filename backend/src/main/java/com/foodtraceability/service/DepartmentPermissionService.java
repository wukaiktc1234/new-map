package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.DepartmentPermission;

import java.util.List;

/**
 * 部门权限服务接口
 */
public interface DepartmentPermissionService extends IService<DepartmentPermission> {

    /**
     * 根据部门ID获取权限列表
     * @param departmentId 部门ID
     * @return 权限列表
     */
    List<DepartmentPermission> getPermissionsByDepartmentId(Long departmentId);

    /**
     * 根据角色ID获取权限列表
     * @param roleId 角色ID
     * @return 权限列表
     */
    List<DepartmentPermission> getPermissionsByRoleId(Long roleId);

    /**
     * 根据用户ID获取权限列表
     * @param userId 用户ID
     * @return 权限列表
     */
    List<DepartmentPermission> getPermissionsByUserId(Long userId);

    /**
     * 检查用户是否有部门权限
     * @param userId 用户ID
     * @param departmentId 部门ID
     * @param permissionType 权限类型
     * @return 是否有权限
     */
    boolean checkUserPermission(Long userId, Long departmentId, Integer permissionType);

    /**
     * 为部门分配角色权限
     * @param departmentId 部门ID
     * @param roleId 角色ID
     * @param permissionType 权限类型
     * @return 操作结果
     */
    boolean assignRolePermission(Long departmentId, Long roleId, Integer permissionType);

    /**
     * 为用户分配部门权限
     * @param userId 用户ID
     * @param departmentId 部门ID
     * @param permissionType 权限类型
     * @return 操作结果
     */
    boolean assignUserPermission(Long userId, Long departmentId, Integer permissionType);

    /**
     * 移除部门权限
     * @param id 权限ID
     * @return 操作结果
     */
    boolean removePermission(Long id);

    /**
     * 获取用户可访问的部门列表
     * @param userId 用户ID
     * @param permissionType 权限类型
     * @return 部门ID列表
     */
    List<Long> getUserAccessibleDepartments(Long userId, Integer permissionType);
}
