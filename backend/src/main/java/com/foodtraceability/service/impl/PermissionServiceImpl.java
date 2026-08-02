package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.LogUtil;
import com.foodtraceability.entity.Permission;
import com.foodtraceability.entity.PermissionTypeCount;
import com.foodtraceability.entity.ModuleCount;
import com.foodtraceability.entity.StatusCount;
import com.foodtraceability.mapper.PermissionMapper;
import com.foodtraceability.service.PermissionService;
import com.foodtraceability.service.RoleService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 权限服务实现类
 * 提供权限相关的业务逻辑实现
 *
 * 安全修复说明：
 * - 所有删除操作已改为逻辑删除，符合项目规范
 * - 使用MyBatis Plus的removeById()方法，自动设置deleted=1
 *
 * 重构说明：
 * - 已移除 Redis 依赖（RedisTemplate）
 * - 菜单缓存刷新改为日志记录，由调用方直接查询数据库
 */
@Service
public class PermissionServiceImpl implements PermissionService {

    private static final Logger logger = LoggerFactory.getLogger(PermissionServiceImpl.class);


    public PermissionServiceImpl(PermissionMapper permissionMapper, RoleService roleService) {
        this.permissionMapper = permissionMapper;
        this.roleService = roleService;
    }

    private final PermissionMapper permissionMapper;

    private final RoleService roleService;

    @Override
    public List<Permission> getUserPermissions(String levelType, Long levelId, Long departmentId, String positionCode) {
        // 安全修复（P0-1）：原实现无视入参直接返回全表权限，导致越权漏洞
        // 该方法被 PermissionInterceptor.checkPermission() 作为回退路径调用，
        // 但方法签名缺少 userId，无法按用户角色正确过滤权限。
        // 正确的权限校验应走 PermissionAspect → PermissionVerifyService.getUserPermissions(userId)
        // 此处返回空列表强制回退路径失败，避免越权风险。
        logger.warn("getUserPermissions(levelType, levelId, departmentId, positionCode) 已废弃，" +
                "该方法无法按用户过滤权限。请使用 PermissionVerifyServiceImpl.getUserPermissions(userId)。" +
                "入参: levelType={}, levelId={}, departmentId={}, positionCode={}",
                levelType, levelId, departmentId, positionCode);
        return new ArrayList<>();
    }

    @Override
    public List<Permission> getAllPermissions() {
        try {
            QueryWrapper<Permission> queryWrapper = new QueryWrapper<>();
            queryWrapper.orderByAsc("sort_order");
            return permissionMapper.selectList(queryWrapper);
        } catch (Exception e) {
            LogUtil.logApiError("GET", "/permissions/all", "获取所有权限列表", e.getMessage(), LogUtil.generateRequestId(), "");
            return new ArrayList<>();
        }
    }

    @Override
    public Permission getPermissionById(Long id) {
        try {
            return permissionMapper.selectById(id);
        } catch (Exception e) {
            // 使用LogUtil记录错误
            LogUtil.logApiError("GET", "/permissions/" + id, "根据ID获取权限", e.getMessage(), LogUtil.generateRequestId(), "id=" + id);
            return null;
        }
    }

    @Override
    public Permission createPermission(Permission permission) {
        try {
            permissionMapper.insert(permission);
            
            // 如果是菜单类型的权限，刷新菜单缓存
            if ("menu".equals(permission.getPermissionType())) {
                refreshMenuCache();
            }
            
            return permission;
        } catch (Exception e) {
            // 使用LogUtil记录错误
            LogUtil.logApiError("POST", "/permissions", "创建权限", e.getMessage(), LogUtil.generateRequestId(), permission.toString());
            return null;
        }
    }

    @Override
    public Permission updatePermission(Long id, Permission permission) {
        try {
            permission.setId(id);
            permissionMapper.updateById(permission);
            
            // 如果是菜单类型的权限，刷新菜单缓存
            if ("menu".equals(permission.getPermissionType())) {
                refreshMenuCache();
            }
            
            return permission;
        } catch (Exception e) {
            // 使用LogUtil记录错误
            LogUtil.logApiError("PUT", "/permissions/" + id, "更新权限", e.getMessage(), LogUtil.generateRequestId(), permission.toString());
            return null;
        }
    }

    @Override
    public void deletePermission(Long id) {
        try {
            // 先获取权限信息，判断是否为菜单类型
            Permission permission = permissionMapper.selectById(id);

            // ✅ 安全修复：使用逻辑删除而非物理删除
            // 原代码使用 deleteById() 会永久删除数据，违反项目规范
            // 使用UpdateWrapper进行逻辑删除，设置deleted=1
            UpdateWrapper<Permission> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", id);
            updateWrapper.set("deleted", 1);
            boolean removed = permissionMapper.update(null, updateWrapper) > 0;

            if (!removed) {
                logger.warn("权限逻辑删除失败或记录不存在: id={}", id);
            }

            // 如果是菜单类型的权限，刷新菜单缓存
            if (permission != null && "menu".equals(permission.getPermissionType())) {
                refreshMenuCache();
            }
        } catch (Exception e) {
            // 使用LogUtil记录错误
            LogUtil.logApiError("DELETE", "/permissions/" + id, "删除权限", e.getMessage(), LogUtil.generateRequestId(), "id=" + id);
        }
    }

    @Override
    public List<Permission> getPermissionsByModule(String module) {
        try {
            QueryWrapper<Permission> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("module", module);
            queryWrapper.orderByAsc("sort_order");
            return permissionMapper.selectList(queryWrapper);
        } catch (Exception e) {
            // 使用LogUtil记录错误
            LogUtil.logApiError("GET", "/permissions/module/" + module, "根据模块获取权限列表", e.getMessage(), LogUtil.generateRequestId(), "module=" + module);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Permission> getPermissionsByType(String permissionType) {
        try {
            QueryWrapper<Permission> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("permission_type", permissionType);
            queryWrapper.orderByAsc("sort_order");
            return permissionMapper.selectList(queryWrapper);
        } catch (Exception e) {
            // 使用LogUtil记录错误
            LogUtil.logApiError("GET", "/permissions/type/" + permissionType, "根据权限类型获取权限列表", e.getMessage(), LogUtil.generateRequestId(), "permissionType=" + permissionType);
            return new ArrayList<>();
        }
    }

    @Override
    public List<Permission> getPermissionTree() {
        try {
            QueryWrapper<Permission> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("status", 1);
            queryWrapper.orderByAsc("parent_id").orderByAsc("sort_order");
            List<Permission> allPermissions = permissionMapper.selectList(queryWrapper);
            return buildPermissionTree(allPermissions);
        } catch (Exception e) {
            LogUtil.logApiError("GET", "/permissions/tree", "获取权限树结构", e.getMessage(), LogUtil.generateRequestId(), "");
            return new ArrayList<>();
        }
    }

    private List<Permission> buildPermissionTree(List<Permission> permissions) {
        List<Permission> tree = new ArrayList<>();
        for (Permission permission : permissions) {
            if (permission.getParentId() == null || permission.getParentId() == 0) {
                tree.add(findChildren(permission, permissions));
            }
        }
        return tree;
    }

    private Permission findChildren(Permission parent, List<Permission> permissions) {
        List<Permission> children = new ArrayList<>();
        for (Permission permission : permissions) {
            if (parent.getId().equals(permission.getParentId())) {
                children.add(findChildren(permission, permissions));
            }
        }
        parent.setChildren(children);
        return parent;
    }
    
    /**
     * 刷新菜单缓存
     * 重构说明：已移除 Redis 缓存，菜单数据由调用方直接查询数据库获取，无需刷新缓存
     * 保留方法仅为兼容现有调用点，调用时记录日志
     */
    private void refreshMenuCache() {
        logger.debug("菜单数据已变更，调用方将直接查询数据库获取最新菜单");
    }

    @Override
    public List<Permission> getByStatus(String status) {
        try {
            QueryWrapper<Permission> queryWrapper = new QueryWrapper<>();
            if (status != null) {
                queryWrapper.eq("status", status);
            }
            queryWrapper.orderByAsc("sort_order");
            return permissionMapper.selectList(queryWrapper);
        } catch (Exception e) {
            // 使用LogUtil记录错误，而不是简单打印
            LogUtil.logApiError("GET", "/permissions", "根据状态获取权限列表", e.getMessage(), LogUtil.generateRequestId(), "status=" + status);
            return new ArrayList<>();
        }
    }

    @Override
    public List<StatusCount> getStatusCount() {
        try {
            // 这里需要实现统计逻辑，暂时返回空列表
            return new ArrayList<>();
        } catch (Exception e) {
            // 使用LogUtil记录错误
            LogUtil.logApiError("GET", "/permissions/count-by-status", "获取状态统计", e.getMessage(), LogUtil.generateRequestId(), "");
            return new ArrayList<>();
        }
    }

    @Override
    public IPage<Permission> getPermissionPage(Page<Permission> page) {
        try {
            return permissionMapper.selectPage(page, null);
        } catch (Exception e) {
            // 使用LogUtil记录错误
            LogUtil.logApiError("GET", "/permissions/page", "分页查询权限", e.getMessage(), LogUtil.generateRequestId(), "page=" + page.getCurrent() + ",size=" + page.getSize());
            return new Page<>();
        }
    }

    @Override
    public IPage<Permission> getPermissionPageByCondition(Page<Permission> page, String permissionCode,
                                                         String permissionName, String permissionType, String status, String module,
                                                         String startDate, String endDate, String parentId) {
        try {
            QueryWrapper<Permission> queryWrapper = new QueryWrapper<>();
            
            if (permissionCode != null && !permissionCode.isEmpty()) {
                queryWrapper.like("permission_code", permissionCode);
            }
            
            if (permissionName != null && !permissionName.isEmpty()) {
                queryWrapper.like("permission_name", permissionName);
            }
            
            if (permissionType != null && !permissionType.isEmpty()) {
                queryWrapper.eq("permission_type", permissionType);
            }
            
            if (status != null && !status.isEmpty()) {
                queryWrapper.eq("status", status);
            }
            
            if (module != null && !module.isEmpty()) {
                queryWrapper.eq("module", module);
            }
            
            if (parentId != null && !parentId.isEmpty()) {
                queryWrapper.eq("parent_id", parentId);
            } else if (parentId != null && parentId.isEmpty()) {
                // 处理空字符串，查询顶级权限
                queryWrapper.eq("parent_id", "");
            }
            
            // 时间范围查询
            if (startDate != null && !startDate.isEmpty()) {
                queryWrapper.ge("created_at", startDate);
            }
            
            if (endDate != null && !endDate.isEmpty()) {
                queryWrapper.le("created_at", endDate);
            }
            
            queryWrapper.orderByAsc("sort_order");
            
            return permissionMapper.selectPage(page, queryWrapper);
        } catch (Exception e) {
            // 使用LogUtil记录错误
            LogUtil.logApiError("GET", "/permissions/page/condition", "条件分页查询权限", e.getMessage(), LogUtil.generateRequestId(), "condition query error");
            return new Page<>();
        }
    }

    @Override
    public List<Permission> getMenuPermissions() {
        QueryWrapper<Permission> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("permission_type", 1);
        queryWrapper.eq("status", 1);
        queryWrapper.orderByAsc("sort_order");
        return permissionMapper.selectList(queryWrapper);
    }

    @Override
    public boolean saveOrUpdatePermission(Permission permission) {
        try {
            if (permission.getId() == null) {
                // 新增权限
                permissionMapper.insert(permission);
            } else {
                // 更新权限
                permissionMapper.updateById(permission);
            }
            
            // 如果是菜单类型的权限，刷新菜单缓存
            if ("menu".equals(permission.getPermissionType())) {
                refreshMenuCache();
            }
            
            return true;
        } catch (Exception e) {
            // 使用LogUtil记录错误
            LogUtil.logApiError("POST/PUT", "/permissions", "保存或更新权限", e.getMessage(), LogUtil.generateRequestId(), "permissionId=" + (permission.getId() != null ? permission.getId() : "new"));
            return false;
        }
    }

    @Override
    public Permission getById(String id) {
        try {
            return permissionMapper.selectById(Long.parseLong(id));
        } catch (Exception e) {
            // 使用LogUtil记录错误
            LogUtil.logApiError("GET", "/permissions/" + id, "根据ID获取权限", e.getMessage(), LogUtil.generateRequestId(), "id=" + id);
            return null;
        }
    }

    @Override
    public boolean deletePermission(String id) {
        try {
            // 先获取权限信息，判断是否为菜单类型
            Permission permission = permissionMapper.selectById(Long.parseLong(id));

            // ✅ 安全修复：使用逻辑删除而非物理删除
            // 原代码使用 deleteById() 会永久删除数据，违反项目规范
            // 使用UpdateWrapper进行逻辑删除，设置deleted=1
            UpdateWrapper<Permission> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", Long.parseLong(id));
            updateWrapper.set("deleted", 1);
            boolean result = permissionMapper.update(null, updateWrapper) > 0;

            if (!result) {
                logger.warn("权限逻辑删除失败或记录不存在: id={}", id);
            }

            // 如果是菜单类型的权限，刷新菜单缓存
            if (result && permission != null && "menu".equals(permission.getPermissionType())) {
                refreshMenuCache();
            }

            return result;
        } catch (Exception e) {
            // 使用LogUtil记录错误
            LogUtil.logApiError("DELETE", "/permissions/" + id, "删除权限", e.getMessage(), LogUtil.generateRequestId(), "id=" + id);
            return false;
        }
    }
}
