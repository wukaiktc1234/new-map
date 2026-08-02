package com.foodtraceability.service;

import com.foodtraceability.dto.PermissionSyncResult;
import com.foodtraceability.dto.PermissionTreeNode;
import com.foodtraceability.entity.Permission;
import java.util.List;

/**
 * 权限初始化服务
 * 从路由配置自动生成权限数据
 */
public interface PermissionInitService {

    /**
     * 初始化权限数据
     */
    void initPermissions();

    /**
     * 同步权限数据
     */
    PermissionSyncResult syncPermissions();

    /**
     * 从路由配置解析权限列表
     */
    List<Permission> parsePermissionsFromRoutes();

    /**
     * 检查权限完整性
     */
    boolean checkPermissionIntegrity();

    /**
     * 获取权限树
     */
    List<PermissionTreeNode> getPermissionTree();

    /**
     * 获取角色权限树（含选中状态）
     */
    List<PermissionTreeNode> getRolePermissionTree(String roleId);
}
