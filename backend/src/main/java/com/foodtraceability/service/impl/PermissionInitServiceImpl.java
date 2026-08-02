package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.dto.PermissionSyncResult;
import com.foodtraceability.dto.PermissionTreeNode;
import com.foodtraceability.entity.Permission;
import com.foodtraceability.entity.RolePermission;
import com.foodtraceability.mapper.PermissionMapper;
import com.foodtraceability.mapper.RolePermissionMapper;
import com.foodtraceability.service.PermissionInitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PermissionInitServiceImpl implements PermissionInitService {

    private static final Logger logger = LoggerFactory.getLogger(PermissionInitServiceImpl.class);


    public PermissionInitServiceImpl(PermissionMapper permissionMapper, RolePermissionMapper rolePermissionMapper) {
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
    }

    private final PermissionMapper permissionMapper;

    private final RolePermissionMapper rolePermissionMapper;

    @Override
    @Transactional
    public void initPermissions() {
        logger.info("开始初始化权限数据...");

        List<Permission> permissions = parsePermissionsFromRoutes();

        for (Permission permission : permissions) {
            LambdaQueryWrapper<Permission> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Permission::getPermissionCode, permission.getPermissionCode());
            Permission existing = permissionMapper.selectOne(wrapper);

            if (existing == null) {
                permissionMapper.insert(permission);
                logger.info("新增权限: {}", permission.getPermissionCode());
            }
        }

        logger.info("权限数据初始化完成，共处理 {} 条权限", permissions.size());
    }

    @Override
    @Transactional
    public PermissionSyncResult syncPermissions() {
        logger.info("开始同步权限数据...");

        PermissionSyncResult result = new PermissionSyncResult();
        result.setAdded(0);
        result.setUpdated(0);
        result.setDeleted(0);
        result.setUnchanged(0);
        result.setAddedPermissions(new ArrayList<>());
        result.setUpdatedPermissions(new ArrayList<>());
        result.setDeletedPermissions(new ArrayList<>());

        List<Permission> routePermissions = parsePermissionsFromRoutes();
        Map<String, Permission> routePermMap = routePermissions.stream()
            .collect(Collectors.toMap(Permission::getPermissionCode, p -> p));

        List<Permission> dbPermissions = permissionMapper.selectList(null);
        Map<String, Permission> dbPermMap = dbPermissions.stream()
            .collect(Collectors.toMap(Permission::getPermissionCode, p -> p));

        for (Permission routePerm : routePermissions) {
            Permission dbPerm = dbPermMap.get(routePerm.getPermissionCode());
            if (dbPerm == null) {
                permissionMapper.insert(routePerm);
                result.setAdded(result.getAdded() + 1);
                result.getAddedPermissions().add(routePerm.getPermissionCode());
            } else {
                boolean changed = false;
                if (!Objects.equals(routePerm.getPermissionName(), dbPerm.getPermissionName())) {
                    dbPerm.setPermissionName(routePerm.getPermissionName());
                    changed = true;
                }
                if (!Objects.equals(routePerm.getModule(), dbPerm.getModule())) {
                    dbPerm.setModule(routePerm.getModule());
                    changed = true;
                }
                if (!Objects.equals(routePerm.getParentId(), dbPerm.getParentId())) {
                    dbPerm.setParentId(routePerm.getParentId());
                    changed = true;
                }
                if (changed) {
                    permissionMapper.updateById(dbPerm);
                    result.setUpdated(result.getUpdated() + 1);
                    result.getUpdatedPermissions().add(dbPerm.getPermissionCode());
                } else {
                    result.setUnchanged(result.getUnchanged() + 1);
                }
            }
        }

        logger.info("权限同步完成: 新增={}, 更新={}, 删除={}, 未变化={}",
            result.getAdded(), result.getUpdated(), result.getDeleted(), result.getUnchanged());

        return result;
    }

    @Override
    public List<Permission> parsePermissionsFromRoutes() {
        List<Permission> permissions = new ArrayList<>();
        long idCounter = 1;

        permissions.add(createPermission(idCounter++, "system", "系统管理", 0, "system", null, 1));
        permissions.add(createPermission(idCounter++, "system:user", "用户管理", 1, "system", 1L, 2));
        permissions.add(createPermission(idCounter++, "system:user:add", "新增用户", 2, "system", 2L, 1));
        permissions.add(createPermission(idCounter++, "system:user:edit", "编辑用户", 2, "system", 2L, 2));
        permissions.add(createPermission(idCounter++, "system:user:delete", "删除用户", 2, "system", 2L, 3));
        permissions.add(createPermission(idCounter++, "system:user:view", "查看用户", 2, "system", 2L, 4));
        permissions.add(createPermission(idCounter++, "system:role", "角色管理", 1, "system", 1L, 3));
        permissions.add(createPermission(idCounter++, "system:role:add", "新增角色", 2, "system", 7L, 1));
        permissions.add(createPermission(idCounter++, "system:role:edit", "编辑角色", 2, "system", 7L, 2));
        permissions.add(createPermission(idCounter++, "system:role:delete", "删除角色", 2, "system", 7L, 3));
        permissions.add(createPermission(idCounter++, "system:role:view", "查看角色", 2, "system", 7L, 4));
        permissions.add(createPermission(idCounter++, "system:permission", "权限管理", 1, "system", 1L, 4));

        permissions.add(createPermission(idCounter++, "product", "产品管理", 0, "product", null, 2));
        permissions.add(createPermission(idCounter++, "product:manage", "菜品管理", 1, "product", 13L, 1));
        permissions.add(createPermission(idCounter++, "product:add", "新增菜品", 2, "product", 14L, 1));
        permissions.add(createPermission(idCounter++, "product:edit", "编辑菜品", 2, "product", 14L, 2));
        permissions.add(createPermission(idCounter++, "product:delete", "删除菜品", 2, "product", 14L, 3));
        permissions.add(createPermission(idCounter++, "product:view", "查看菜品", 2, "product", 14L, 4));

        permissions.add(createPermission(idCounter++, "order", "订单管理", 0, "order", null, 3));
        permissions.add(createPermission(idCounter++, "order:manage", "订单管理", 1, "order", 19L, 1));
        permissions.add(createPermission(idCounter++, "order:add", "新增订单", 2, "order", 20L, 1));
        permissions.add(createPermission(idCounter++, "order:edit", "编辑订单", 2, "order", 20L, 2));
        permissions.add(createPermission(idCounter++, "order:delete", "删除订单", 2, "order", 20L, 3));
        permissions.add(createPermission(idCounter++, "order:view", "查看订单", 2, "order", 20L, 4));

        permissions.add(createPermission(idCounter++, "warehouse", "仓储管理", 0, "warehouse", null, 4));
        permissions.add(createPermission(idCounter++, "warehouse:overview:view", "库存概览查看", 1, "warehouse", 25L, 1));
        permissions.add(createPermission(idCounter++, "warehouse:inventory:view", "库存管理查看", 1, "warehouse", 25L, 2));
        permissions.add(createPermission(idCounter++, "warehouse:inbound:manage", "入库管理", 1, "warehouse", 25L, 3));
        permissions.add(createPermission(idCounter++, "warehouse:outbound:manage", "出库管理", 1, "warehouse", 25L, 4));

        permissions.add(createPermission(idCounter++, "purchase", "采购管理", 0, "purchase", null, 5));
        permissions.add(createPermission(idCounter++, "purchase:overview:view", "采购概览查看", 1, "purchase", 30L, 1));
        permissions.add(createPermission(idCounter++, "purchase:order:manage", "采购订单管理", 1, "purchase", 30L, 2));
        permissions.add(createPermission(idCounter++, "purchase:supplier:manage", "供应商管理", 1, "purchase", 30L, 3));

        permissions.add(createPermission(idCounter++, "hr", "人事管理", 0, "hr", null, 6));
        permissions.add(createPermission(idCounter++, "hr:manage", "员工管理", 1, "hr", 35L, 1));
        permissions.add(createPermission(idCounter++, "hr:add", "新增员工", 2, "hr", 36L, 1));
        permissions.add(createPermission(idCounter++, "hr:edit", "编辑员工", 2, "hr", 36L, 2));
        permissions.add(createPermission(idCounter++, "hr:delete", "删除员工", 2, "hr", 36L, 3));
        permissions.add(createPermission(idCounter++, "hr:view", "查看员工", 2, "hr", 36L, 4));

        permissions.add(createPermission(idCounter++, "finance", "财务管理", 0, "finance", null, 7));
        permissions.add(createPermission(idCounter++, "finance:view", "财务查看", 1, "finance", 41L, 1));
        permissions.add(createPermission(idCounter++, "finance:manage", "财务管理", 1, "finance", 41L, 2));

        permissions.add(createPermission(idCounter++, "traceability", "食品追溯", 0, "traceability", null, 8));
        permissions.add(createPermission(idCounter++, "traceability:view", "追溯查看", 1, "traceability", 44L, 1));
        permissions.add(createPermission(idCounter++, "traceability:manage", "追溯管理", 1, "traceability", 44L, 2));

        return permissions;
    }

    @Override
    public boolean checkPermissionIntegrity() {
        List<Permission> permissions = permissionMapper.selectList(null);
        Map<Long, Permission> permMap = permissions.stream()
            .collect(Collectors.toMap(Permission::getId, p -> p));

        for (Permission perm : permissions) {
            if (perm.getParentId() != null && !permMap.containsKey(perm.getParentId())) {
                logger.warn("权限 {} 的父权限 {} 不存在", perm.getPermissionCode(), perm.getParentId());
                return false;
            }
        }

        return true;
    }

    @Override
    public List<PermissionTreeNode> getPermissionTree() {
        List<Permission> permissions = permissionMapper.selectList(null);
        return buildPermissionTree(permissions, null);
    }

    @Override
    public List<PermissionTreeNode> getRolePermissionTree(String roleId) {
        List<Permission> permissions = permissionMapper.selectList(null);

        LambdaQueryWrapper<RolePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RolePermission::getRoleId, roleId);
        List<RolePermission> rolePermissions = rolePermissionMapper.selectList(wrapper);

        Set<Long> assignedPermIds = rolePermissions.stream()
            .map(RolePermission::getPermissionId)
            .collect(Collectors.toSet());

        return buildPermissionTree(permissions, assignedPermIds);
    }

    private List<PermissionTreeNode> buildPermissionTree(List<Permission> permissions, Set<Long> checkedIds) {
        Map<Long, List<Permission>> childrenMap = permissions.stream()
            .filter(p -> p.getParentId() != null)
            .collect(Collectors.groupingBy(Permission::getParentId));

        List<Permission> rootPermissions = permissions.stream()
            .filter(p -> p.getParentId() == null)
            .sorted(Comparator.comparing(Permission::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())))
            .collect(Collectors.toList());

        return rootPermissions.stream()
            .map(p -> convertToTreeNode(p, childrenMap, checkedIds))
            .collect(Collectors.toList());
    }

    private PermissionTreeNode convertToTreeNode(Permission permission, 
            Map<Long, List<Permission>> childrenMap, Set<Long> checkedIds) {
        PermissionTreeNode node = new PermissionTreeNode();
        node.setId(permission.getId());
        node.setPermissionCode(permission.getPermissionCode());
        node.setPermissionName(permission.getPermissionName());
        node.setPermissionType(permission.getPermissionType());
        node.setModule(permission.getModule());
        node.setParentId(permission.getParentId());
        node.setSortOrder(permission.getSortOrder());
        node.setChecked(checkedIds != null && checkedIds.contains(permission.getId()));
        node.setDisabled(false);

        List<Permission> children = childrenMap.get(permission.getId());
        if (children != null && !children.isEmpty()) {
            List<PermissionTreeNode> childNodes = children.stream()
                .sorted(Comparator.comparing(Permission::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(p -> convertToTreeNode(p, childrenMap, checkedIds))
                .collect(Collectors.toList());
            node.setChildren(childNodes);
        }

        return node;
    }

    private Permission createPermission(Long id, String code, String name, 
            Integer type, String module, Long parentId, Integer sortOrder) {
        Permission permission = new Permission();
        permission.setId(id);
        permission.setPermissionCode(code);
        permission.setPermissionName(name);
        permission.setPermissionType(type);
        permission.setModule(module);
        permission.setParentId(parentId);
        permission.setSortOrder(sortOrder);
        permission.setStatus(1);
        return permission;
    }
}
