package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.Role;
import com.foodtraceability.entity.RoleTypeCount;
import com.foodtraceability.entity.StatusCount;
import com.foodtraceability.mapper.RoleMapper;
import com.foodtraceability.service.PermissionVerifyService;
import com.foodtraceability.service.RoleService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 角色服务实现类
 */
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PermissionVerifyService permissionVerifyService;

    public RoleServiceImpl(PermissionVerifyService permissionVerifyService) {
        this.permissionVerifyService = permissionVerifyService;
    }

    @Override
    @Transactional
    public Role createRole(Role role) {
        if (checkRoleCodeExists(role.getRoleCode())) {
            throw new RuntimeException("角色编码已存在");
        }
        if (checkRoleNameExists(role.getRoleName())) {
            throw new RuntimeException("角色名称已存在");
        }
        
        if (role.getRoleLevel() == null) {
            role.setRoleLevel(0);
        }
        if (role.getStatus() == null) {
            role.setStatus("active");
        }
        if (role.getRoleType() == null) {
            role.setRoleType(2);
        }
        if (role.getSystemBuiltIn() == null) {
            role.setSystemBuiltIn(false);
        }
        if (role.getDataScope() == null) {
            role.setDataScope("self");
        }
        // 自动生成 roleIdStr（如 roleCode=TEST_REGIONAL_MGR → roleIdStr=ROLE_TEST_REGIONAL_MGR）
        if (role.getRoleIdStr() == null && role.getRoleCode() != null) {
            role.setRoleIdStr("ROLE_" + role.getRoleCode().toUpperCase());
        }
        
        save(role);
        return role;
    }

    @Override
    @Transactional
    public Role updateRole(Role role) {
        Role existingRole = getRoleById(role.getRoleId());
        if (existingRole == null) {
            throw new RuntimeException("角色不存在");
        }
        
        if (existingRole.getSystemBuiltIn() && role.getRoleCode() != null) {
            throw new RuntimeException("系统内置角色不能修改编码");
        }
        
        if (role.getRoleCode() != null && !role.getRoleCode().equals(existingRole.getRoleCode())) {
            if (checkRoleCodeExists(role.getRoleCode())) {
                throw new RuntimeException("角色编码已存在");
            }
        }
        
        if (role.getRoleName() != null && !role.getRoleName().equals(existingRole.getRoleName())) {
            if (checkRoleNameExists(role.getRoleName())) {
                throw new RuntimeException("角色名称已存在");
            }
        }
        
        updateById(role);
        
        // 安全修复：角色变更后刷新所有使用该角色的用户权限缓存
        try {
            refreshPermissionsForUsersWithRole(role.getRoleId());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(RoleServiceImpl.class)
                .warn("角色更新后权限缓存刷新失败: roleId={}", role.getRoleId(), e);
        }
        
        return getRoleById(role.getRoleId());
    }

    @Override
    @Transactional
    public boolean deleteRole(Long roleId) {
        Role role = getRoleById(roleId);
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }
        
        if (role.getSystemBuiltIn()) {
            throw new RuntimeException("系统内置角色不能删除");
        }
        
        boolean result = removeById(roleId);
        
        // 安全修复：角色删除后刷新所有使用该角色的用户权限缓存
        if (result) {
            try {
                refreshPermissionsForUsersWithRole(roleId);
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(RoleServiceImpl.class)
                    .warn("角色删除后权限缓存刷新失败: roleId={}", roleId, e);
            }
        }
        
        return result;
    }

    @Override
    public Role getRoleById(Long roleId) {
        return getById(roleId);
    }

    @Override
    public Role getRoleByCode(String roleCode) {
        return baseMapper.findByRoleCode(roleCode);
    }

    @Override
    public IPage<Role> getRolePage(Page<Role> page, String roleCode, String roleName, String status, Integer roleType) {
        return baseMapper.findRolesByConditionPage(page, roleCode, roleName, status, roleType, null);
    }

    @Override
    public List<Role> getAllRoles() {
        return list();
    }

    @Override
    public List<Role> getActiveRoles() {
        return baseMapper.findByStatus("active");
    }

    @Override
    public List<Role> getSystemRoles() {
        return baseMapper.findSystemBuiltRoles();
    }

    @Override
    public List<Role> getCustomRoles() {
        return baseMapper.findCustomRoles();
    }

    @Override
    public boolean checkRoleCodeExists(String roleCode) {
        return baseMapper.existsByRoleCode(roleCode);
    }

    @Override
    public boolean checkRoleNameExists(String roleName) {
        return baseMapper.existsByRoleName(roleName);
    }

    @Override
    public long countRoles() {
        return baseMapper.countRoles();
    }

    @Override
    public List<StatusCount> countByStatus() {
        return baseMapper.countByStatus();
    }

    @Override
    public List<RoleTypeCount> countByRoleType() {
        return baseMapper.countByRoleType();
    }

    @Override
    @Transactional
    public boolean toggleRoleStatus(Long roleId, String status) {
        Role role = getRoleById(roleId);
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }

        if (role.getSystemBuiltIn()) {
            throw new RuntimeException("系统内置角色不能禁用");
        }

        role.setStatus(status);
        return updateById(role);
    }

    @Override
    @Transactional
    public boolean assignPermissions(Long roleId, List<String> permissions) {
        Role role = getRoleById(roleId);
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }
        
        try {
            String permissionsJson = objectMapper.writeValueAsString(permissions);
            role.setPermissions(permissionsJson);
            return updateById(role);
        } catch (Exception e) {
            throw new RuntimeException("权限序列化失败", e);
        }
    }

    @Override
    public List<String> getRolePermissions(Long roleId) {
        Role role = getRoleById(roleId);
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }
        
        if (role.getPermissions() == null || role.getPermissions().isEmpty()) {
            return List.of();
        }
        
        try {
            return objectMapper.readValue(role.getPermissions(), new TypeReference<List<String>>() {});
        } catch (Exception e) {
            throw new RuntimeException("权限反序列化失败", e);
        }
    }

    @Override
    @Transactional
    public boolean setDataScope(Long roleId, String dataScope, List<String> accessibleStores, List<String> accessibleDepartments) {
        Role role = getRoleById(roleId);
        if (role == null) {
            throw new RuntimeException("角色不存在");
        }
        
        role.setDataScope(dataScope);
        
        try {
            if (accessibleStores != null && !accessibleStores.isEmpty()) {
                role.setAccessibleStores(objectMapper.writeValueAsString(accessibleStores));
            } else {
                role.setAccessibleStores(null);
            }
            
            if (accessibleDepartments != null && !accessibleDepartments.isEmpty()) {
                role.setAccessibleDepartments(objectMapper.writeValueAsString(accessibleDepartments));
            } else {
                role.setAccessibleDepartments(null);
            }
            
            return updateById(role);
        } catch (Exception e) {
            throw new RuntimeException("数据权限序列化失败", e);
        }
    }

    @Override
    public boolean isSystemRole(Long roleId) {
        Role role = getRoleById(roleId);
        return role != null && role.getSystemBuiltIn();
    }

    /**
     * 刷新所有使用指定角色的用户权限缓存
     * 安全修复：确保角色变更后权限立即生效，避免已降权用户在缓存TTL内仍保留高权限
     *
     * @param roleId 角色ID
     */
    private void refreshPermissionsForUsersWithRole(Long roleId) {
        try {
            List<String> userIds = baseMapper.findUserIdsByRoleId(roleId);
            if (userIds == null || userIds.isEmpty()) {
                return;
            }
            
            for (String userId : userIds) {
                try {
                    permissionVerifyService.refreshUserPermissionCache(userId);
                } catch (Exception e) {
                    org.slf4j.LoggerFactory.getLogger(RoleServiceImpl.class)
                        .debug("单个用户权限缓存刷新失败: userId={}, roleId={}", userId, roleId, e);
                }
            }
            
            org.slf4j.LoggerFactory.getLogger(RoleServiceImpl.class)
                .info("角色变更后权限缓存刷新完成: roleId={}, 影响用户数={}", roleId, userIds.size());
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(RoleServiceImpl.class)
                .error("查询角色用户列表失败: roleId={}", roleId, e);
        }
    }
}
