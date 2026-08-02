package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.Permission;
import com.foodtraceability.entity.StatusCount;
import java.util.List;

/**
 * 权限服务接口
 */
public interface PermissionService {

    /**
     * 获取用户权限列表
     * 三维度权限模型：职位等级类型 + 职位等级ID + 部门ID + 职位编码
     */
    List<Permission> getUserPermissions(String levelType, Long levelId, Long departmentId, String positionCode);

    /**
     * 获取所有权限列表
     */
    List<Permission> getAllPermissions();

    /**
     * 根据ID获取权限
     */
    Permission getPermissionById(Long id);

    /**
     * 创建权限
     */
    Permission createPermission(Permission permission);

    /**
     * 更新权限
     */
    Permission updatePermission(Long id, Permission permission);

    /**
     * 删除权限
     */
    void deletePermission(Long id);

    /**
     * 根据模块获取权限列表
     */
    List<Permission> getPermissionsByModule(String module);

    /**
     * 根据权限类型获取权限列表
     */
    List<Permission> getPermissionsByType(String permissionType);

    /**
     * 获取权限树结构
     */
    List<Permission> getPermissionTree();

    /**
     * 根据状态获取权限列表
     */
    List<Permission> getByStatus(String status);
    
    /**
     * 获取各状态权限数量统计
     */
    List<StatusCount> getStatusCount();
    
    /**
     * 分页获取权限
     */
    IPage<Permission> getPermissionPage(Page<Permission> page);
    
    /**
     * 条件分页查询权限
     */
    IPage<Permission> getPermissionPageByCondition(Page<Permission> page, String permissionCode,
                                                 String permissionName, String permissionType, String status, String module,
                                                 String startDate, String endDate, String parentId);
    
    /**
     * 获取菜单权限列表
     */
    List<Permission> getMenuPermissions();
    
    /**
     * 保存或更新权限
     */
    boolean saveOrUpdatePermission(Permission permission);
    
    /**
     * 根据ID获取权限（兼容字符串ID）
     */
    Permission getById(String id);
    
    /**
     * 根据字符串ID删除权限
     */
    boolean deletePermission(String id);
}