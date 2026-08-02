package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.DepartmentPermission;
import com.foodtraceability.mapper.DepartmentPermissionMapper;
import com.foodtraceability.service.DepartmentPermissionService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 部门权限服务实现类
 */
@Service
public class DepartmentPermissionServiceImpl extends ServiceImpl<DepartmentPermissionMapper, DepartmentPermission> implements DepartmentPermissionService {

    @Override
    public List<DepartmentPermission> getPermissionsByDepartmentId(Long departmentId) {
        QueryWrapper<DepartmentPermission> wrapper = new QueryWrapper<>();
        wrapper.eq("department_id", departmentId);
        wrapper.eq("status", 1);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public List<DepartmentPermission> getPermissionsByRoleId(Long roleId) {
        QueryWrapper<DepartmentPermission> wrapper = new QueryWrapper<>();
        wrapper.eq("role_id", roleId);
        wrapper.eq("status", 1);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public List<DepartmentPermission> getPermissionsByUserId(Long userId) {
        QueryWrapper<DepartmentPermission> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("status", 1);
        return baseMapper.selectList(wrapper);
    }

    @Override
    public boolean checkUserPermission(Long userId, Long departmentId, Integer permissionType) {
        QueryWrapper<DepartmentPermission> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("department_id", departmentId);
        wrapper.eq("permission_type", permissionType);
        wrapper.eq("status", 1);
        return baseMapper.selectCount(wrapper) > 0;
    }

    @Override
    public boolean assignRolePermission(Long departmentId, Long roleId, Integer permissionType) {
        // 检查是否已存在相同权限
        QueryWrapper<DepartmentPermission> existingWrapper = new QueryWrapper<>();
        existingWrapper.eq("department_id", departmentId);
        existingWrapper.eq("role_id", roleId);
        existingWrapper.eq("permission_type", permissionType);
        
        if (baseMapper.selectCount(existingWrapper) > 0) {
            return true; // 已存在，直接返回成功
        }

        // 创建新权限
        DepartmentPermission permission = new DepartmentPermission();
        permission.setDepartmentId(departmentId);
        permission.setRoleId(roleId);
        permission.setPermissionType(permissionType);
        permission.setStatus(1);

        return baseMapper.insert(permission) > 0;
    }

    @Override
    public boolean assignUserPermission(Long userId, Long departmentId, Integer permissionType) {
        // 检查是否已存在相同权限
        QueryWrapper<DepartmentPermission> existingWrapper = new QueryWrapper<>();
        existingWrapper.eq("user_id", userId);
        existingWrapper.eq("department_id", departmentId);
        existingWrapper.eq("permission_type", permissionType);
        
        if (baseMapper.selectCount(existingWrapper) > 0) {
            return true; // 已存在，直接返回成功
        }

        // 创建新权限
        DepartmentPermission permission = new DepartmentPermission();
        permission.setUserId(userId);
        permission.setDepartmentId(departmentId);
        permission.setPermissionType(permissionType);
        permission.setStatus(1);

        return baseMapper.insert(permission) > 0;
    }

    @Override
    public boolean removePermission(Long id) {
        DepartmentPermission permission = new DepartmentPermission();
        permission.setId(id);
        permission.setStatus(0); // 禁用权限
        return baseMapper.updateById(permission) > 0;
    }

    @Override
    public List<Long> getUserAccessibleDepartments(Long userId, Integer permissionType) {
        QueryWrapper<DepartmentPermission> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        wrapper.eq("permission_type", permissionType);
        wrapper.eq("status", 1);
        
        List<DepartmentPermission> permissions = baseMapper.selectList(wrapper);
        List<Long> departmentIds = new ArrayList<>();
        
        for (DepartmentPermission permission : permissions) {
            departmentIds.add(permission.getDepartmentId());
        }
        
        return departmentIds;
    }
}
