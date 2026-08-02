package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.Role;
import com.foodtraceability.entity.RoleTypeCount;
import com.foodtraceability.entity.StatusCount;

import java.util.List;

/**
 * 角色服务接口
 * 提供角色管理的业务逻辑方法
 */
public interface RoleService {

    /**
     * 创建角色
     * @param role 角色信息
     * @return 创建后的角色
     */
    Role createRole(Role role);

    /**
     * 更新角色
     * @param role 角色信息
     * @return 更新后的角色
     */
    Role updateRole(Role role);

    /**
     * 删除角色
     * @param roleId 角色ID
     * @return 是否删除成功
     */
    boolean deleteRole(Long roleId);

    /**
     * 根据ID查询角色
     * @param roleId 角色ID
     * @return 角色信息
     */
    Role getRoleById(Long roleId);

    /**
     * 根据角色编码查询角色
     * @param roleCode 角色编码
     * @return 角色信息
     */
    Role getRoleByCode(String roleCode);

    /**
     * 分页查询角色列表
     * @param page 分页参数
     * @param roleCode 角色编码
     * @param roleName 角色名称
     * @param status 状态（active-启用，inactive-禁用）
     * @param roleType 角色类型（1-系统角色，2-自定义角色）
     * @return 角色分页列表
     */
    IPage<Role> getRolePage(Page<Role> page, String roleCode, String roleName, String status, Integer roleType);

    /**
     * 获取所有角色列表
     * @return 角色列表
     */
    List<Role> getAllRoles();

    /**
     * 获取活跃角色列表
     * @return 活跃角色列表
     */
    List<Role> getActiveRoles();

    /**
     * 获取系统内置角色
     * @return 系统内置角色列表
     */
    List<Role> getSystemRoles();

    /**
     * 获取自定义角色
     * @return 自定义角色列表
     */
    List<Role> getCustomRoles();

    /**
     * 检查角色编码是否已存在
     * @param roleCode 角色编码
     * @return 是否存在
     */
    boolean checkRoleCodeExists(String roleCode);

    /**
     * 检查角色名称是否已存在
     * @param roleName 角色名称
     * @return 是否存在
     */
    boolean checkRoleNameExists(String roleName);

    /**
     * 统计角色总数
     * @return 角色总数
     */
    long countRoles();

    /**
     * 统计各状态的角色数量
     * @return 状态统计列表
     */
    List<StatusCount> countByStatus();

    /**
     * 统计各类型角色的数量
     * @return 类型统计列表
     */
    List<RoleTypeCount> countByRoleType();

    /**
     * 切换角色状态
     * @param roleId 角色ID
     * @param status 状态（active-启用，inactive-禁用）
     * @return 是否切换成功
     */
    boolean toggleRoleStatus(Long roleId, String status);

    /**
     * 为角色分配权限
     * @param roleId 角色ID
     * @param permissions 权限列表
     * @return 是否分配成功
     */
    boolean assignPermissions(Long roleId, List<String> permissions);

    /**
     * 获取角色的权限列表
     * @param roleId 角色ID
     * @return 权限列表
     */
    List<String> getRolePermissions(Long roleId);

    /**
     * 设置角色的数据权限范围
     * @param roleId 角色ID
     * @param dataScope 数据权限范围
     * @param accessibleStores 可访问的门店列表
     * @param accessibleDepartments 可访问的部门列表
     * @return 是否设置成功
     */
    boolean setDataScope(Long roleId, String dataScope, List<String> accessibleStores, List<String> accessibleDepartments);

    /**
     * 检查角色是否为系统内置角色
     * @param roleId 角色ID
     * @return 是否为系统内置角色
     */
    boolean isSystemRole(Long roleId);
}
