package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.foodtraceability.dto.PermissionAssignmentDTO;
import com.foodtraceability.dto.PermissionAssignmentResultDTO;
import com.foodtraceability.entity.*;
import com.foodtraceability.mapper.*;
import com.foodtraceability.service.PermissionAutoAssignService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 权限自动分配服务实现类
 */
@Service
public class PermissionAutoAssignServiceImpl implements PermissionAutoAssignService {

    private static final Logger logger = LoggerFactory.getLogger(PermissionAutoAssignServiceImpl.class);


    public PermissionAutoAssignServiceImpl(PositionRoleMappingMapper positionRoleMappingMapper, EmployeeDataScopeMapper employeeDataScopeMapper, PermissionAssignmentLogMapper permissionAssignmentLogMapper, EmployeeMapper employeeMapper, UserMapper userMapper, UserRoleMapper userRoleMapper, ObjectMapper objectMapper) {
        this.positionRoleMappingMapper = positionRoleMappingMapper;
        this.employeeDataScopeMapper = employeeDataScopeMapper;
        this.permissionAssignmentLogMapper = permissionAssignmentLogMapper;
        this.employeeMapper = employeeMapper;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.objectMapper = objectMapper;
    }

    private final PositionRoleMappingMapper positionRoleMappingMapper;

    private final EmployeeDataScopeMapper employeeDataScopeMapper;

    private final PermissionAssignmentLogMapper permissionAssignmentLogMapper;

    private final EmployeeMapper employeeMapper;

    private final UserMapper userMapper;

    private final UserRoleMapper userRoleMapper;

    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PermissionAssignmentResultDTO autoAssignPermission(PermissionAssignmentDTO dto) {
        logger.info("开始自动分配权限，员工ID: {}, 操作类型: {}", dto.getEmployeeId(), dto.getOperationType());

        PermissionAssignmentResultDTO result = new PermissionAssignmentResultDTO();
        result.setEmployeeId(dto.getEmployeeId());
        result.setOperationType(dto.getOperationType());

        try {
            // 查询员工信息
            Employee employee = employeeMapper.selectById(dto.getEmployeeId());
            if (employee == null) {
                result.setSuccess(false);
                result.setMessage("员工不存在");
                return result;
            }

            result.setEmployeeName(employee.getName());

            // 获取当前权限（用于日志记录）
            List<PermissionAssignmentResultDTO.RoleInfo> oldRoles = getCurrentRoles(dto.getEmployeeId());
            List<PermissionAssignmentResultDTO.DataScopeInfo> oldDataScopes = getCurrentDataScopes(dto.getEmployeeId());

            // 1. 分配角色权限
            List<PermissionAssignmentResultDTO.RoleInfo> assignedRoles = assignRoles(dto);

            // 2. 分配数据权限
            List<PermissionAssignmentResultDTO.DataScopeInfo> assignedDataScopes = assignDataScopes(dto, employee);

            // 3. 记录权限分配日志
            savePermissionLog(dto, employee, oldRoles, oldDataScopes, assignedRoles, assignedDataScopes);

            result.setAssignedRoles(assignedRoles);
            result.setAssignedDataScopes(assignedDataScopes);
            result.setSuccess(true);
            result.setMessage("权限分配成功");

            logger.info("权限分配成功，员工ID: {}, 角色: {}, 数据权限: {}", 
                    dto.getEmployeeId(), assignedRoles.size(), assignedDataScopes.size());

        } catch (Exception e) {
            logger.error("权限分配失败，员工ID: {}", dto.getEmployeeId(), e);
            result.setSuccess(false);
            result.setMessage("权限分配失败：" + e.getMessage());
            throw new RuntimeException("权限分配失败", e);
        }

        return result;
    }

    @Override
    public PermissionAssignmentResultDTO assignOnOnboarding(String employeeId, String positionId, 
            String storeId, String departmentId, String operator) {
        PermissionAssignmentDTO dto = new PermissionAssignmentDTO();
        dto.setEmployeeId(employeeId);
        dto.setPositionId(positionId);
        dto.setStoreId(storeId);
        dto.setDepartmentId(departmentId);
        dto.setOperationType("onboard");
        dto.setRemark("员工入职自动分配权限");
        return autoAssignPermission(dto);
    }

    @Override
    public PermissionAssignmentResultDTO assignOnPositionChange(String employeeId, String oldPositionId, 
            String newPositionId, String operator) {
        PermissionAssignmentDTO dto = new PermissionAssignmentDTO();
        dto.setEmployeeId(employeeId);
        dto.setPositionId(newPositionId);
        dto.setOperationType("position_change");
        dto.setRemark("职位变更自动调整权限");
        return autoAssignPermission(dto);
    }

    @Override
    public PermissionAssignmentResultDTO assignOnStoreAssignment(String employeeId, String storeId, String operator) {
        PermissionAssignmentDTO dto = new PermissionAssignmentDTO();
        dto.setEmployeeId(employeeId);
        dto.setStoreId(storeId);
        dto.setOperationType("store_assign");
        dto.setRemark("门店分配自动调整数据权限");
        return autoAssignPermission(dto);
    }

    @Override
    public List<PositionRoleMapping> getRolesByPositionId(Long positionId) {
        return positionRoleMappingMapper.selectByPositionId(positionId);
    }

    @Override
    public List<EmployeeDataScope> getDataScopesByEmployeeId(String employeeId) {
        return employeeDataScopeMapper.selectByEmployeeId(employeeId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean clearAutoAssignedPermissions(String employeeId, String operator) {
        logger.info("清除员工自动分配权限，员工ID: {}", employeeId);

        try {
            LambdaQueryWrapper<EmployeeDataScope> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(EmployeeDataScope::getEmployeeId, employeeId)
                    .eq(EmployeeDataScope::getSource, "auto");
            
            employeeDataScopeMapper.delete(queryWrapper);

            logger.info("清除员工自动分配权限成功，员工ID: {}", employeeId);
            return true;
        } catch (Exception e) {
            logger.error("清除员工自动分配权限失败，员工ID: {}", employeeId, e);
            throw new RuntimeException("清除权限失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int recalculatePermissionsByPosition(Long positionId, String operator) {
        logger.info("重新计算职位相关员工权限，职位ID: {}", positionId);

        // 查询该职位下的所有员工
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Employee::getPositionId, positionId.toString())
                .eq(Employee::getDeleted, 0);
        List<Employee> employees = employeeMapper.selectList(queryWrapper);

        int count = 0;
        for (Employee employee : employees) {
            try {
                PermissionAssignmentDTO dto = new PermissionAssignmentDTO();
                dto.setEmployeeId(String.valueOf(employee.getId()));
                dto.setPositionId(positionId.toString());
                dto.setStoreId(employee.getStoreId() != null ? employee.getStoreId().toString() : null);
                dto.setDepartmentId(employee.getDepartmentId() != null ? String.valueOf(employee.getDepartmentId()) : null);
                dto.setOperationType("position_change");
                dto.setRemark("职位-角色映射变更，重新计算权限");
                autoAssignPermission(dto);
                count++;
            } catch (Exception e) {
                logger.error("重新计算员工权限失败，员工ID: {}", employee.getId(), e);
            }
        }

        logger.info("重新计算职位相关员工权限完成，职位ID: {}, 影响员工数: {}", positionId, count);
        return count;
    }

    /**
     * 分配角色权限
     */
    private List<PermissionAssignmentResultDTO.RoleInfo> assignRoles(PermissionAssignmentDTO dto) {
        List<PermissionAssignmentResultDTO.RoleInfo> assignedRoles = new ArrayList<>();

        if (dto.getPositionId() == null || dto.getPositionId().isEmpty()) {
            logger.warn("职位ID为空，跳过角色分配");
            return assignedRoles;
        }

        // 查询职位关联的角色
        List<PositionRoleMapping> roleMappings = positionRoleMappingMapper.selectByPositionId(Long.parseLong(dto.getPositionId()));
        
        if (roleMappings.isEmpty()) {
            logger.warn("职位未配置角色映射，职位ID: {}", dto.getPositionId());
            return assignedRoles;
        }

        // 查询用户信息
        Employee employee = employeeMapper.selectById(dto.getEmployeeId());
        if (employee == null || employee.getEmployeeCode() == null) {
            logger.warn("员工不存在或员工编码为空，员工ID: {}", dto.getEmployeeId());
            return assignedRoles;
        }

        LambdaQueryWrapper<User> userQuery = new LambdaQueryWrapper<>();
        userQuery.eq(User::getEmployeeCode, employee.getEmployeeCode())
                .eq(User::getDeleted, 0);
        User user = userMapper.selectOne(userQuery);

        if (user == null) {
            logger.warn("用户不存在，员工编码: {}", employee.getEmployeeCode());
            return assignedRoles;
        }

        // 删除旧的用户角色关联（如果是强制覆盖）
        if (dto.getForceOverride() != null && dto.getForceOverride()) {
            LambdaUpdateWrapper<UserRole> deleteWrapper = new LambdaUpdateWrapper<>();
            deleteWrapper.eq(UserRole::getUserId, user.getId());
            userRoleMapper.delete(deleteWrapper);
        }

        // 分配新角色
        List<Long> roleIds = new ArrayList<>();
        for (PositionRoleMapping mapping : roleMappings) {
            // 检查是否已存在该角色
            LambdaQueryWrapper<UserRole> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(UserRole::getUserId, user.getId())
                    .eq(UserRole::getRoleId, mapping.getRoleId());
            Long count = userRoleMapper.selectCount(queryWrapper);
            
            if (count == 0) {
                UserRole userRole = new UserRole();
                userRole.setUserId(user.getId());
                userRole.setRoleId(mapping.getRoleId());
                userRole.setCreatedAt(LocalDateTime.now());
                userRoleMapper.insert(userRole);
            }

            roleIds.add(mapping.getRoleId());

            // 构建返回结果
            PermissionAssignmentResultDTO.RoleInfo roleInfo = new PermissionAssignmentResultDTO.RoleInfo();
            roleInfo.setRoleId(mapping.getRoleId().toString());
            roleInfo.setRoleCode(mapping.getRoleCode());
            roleInfo.setRoleName(mapping.getRoleName());
            roleInfo.setIsPrimary(mapping.getIsPrimary() == 1);
            assignedRoles.add(roleInfo);
        }

        // 更新用户的roles字段（JSON格式）
        try {
            user.setRoles(objectMapper.writeValueAsString(roleIds));
            userMapper.updateById(user);
        } catch (JsonProcessingException e) {
            logger.error("序列化角色ID列表失败", e);
        }

        return assignedRoles;
    }

    /**
     * 分配数据权限
     */
    private List<PermissionAssignmentResultDTO.DataScopeInfo> assignDataScopes(PermissionAssignmentDTO dto, Employee employee) {
        List<PermissionAssignmentResultDTO.DataScopeInfo> assignedDataScopes = new ArrayList<>();

        // 1. 如果指定了门店ID，添加门店数据权限
        if (dto.getStoreId() != null && !dto.getStoreId().isEmpty()) {
            EmployeeDataScope storeScope = createOrUpdateDataScope(
                    dto.getEmployeeId(), "store", dto.getStoreId(), "auto", dto.getEmployeeId());
            
            if (storeScope != null) {
                PermissionAssignmentResultDTO.DataScopeInfo scopeInfo = new PermissionAssignmentResultDTO.DataScopeInfo();
                scopeInfo.setScopeType("store");
                scopeInfo.setScopeId(dto.getStoreId());
                scopeInfo.setScopeName(storeScope.getScopeName());
                scopeInfo.setSource("auto");
                assignedDataScopes.add(scopeInfo);
            }
        }

        // 2. 如果指定了部门ID，添加部门数据权限
        if (dto.getDepartmentId() != null && !dto.getDepartmentId().isEmpty()) {
            EmployeeDataScope deptScope = createOrUpdateDataScope(
                    dto.getEmployeeId(), "department", dto.getDepartmentId(), "auto", dto.getEmployeeId());
            
            if (deptScope != null) {
                PermissionAssignmentResultDTO.DataScopeInfo scopeInfo = new PermissionAssignmentResultDTO.DataScopeInfo();
                scopeInfo.setScopeType("department");
                scopeInfo.setScopeId(dto.getDepartmentId());
                scopeInfo.setScopeName(deptScope.getScopeName());
                scopeInfo.setSource("auto");
                assignedDataScopes.add(scopeInfo);
            }
        }

        return assignedDataScopes;
    }

    /**
     * 创建或更新数据权限
     */
    private EmployeeDataScope createOrUpdateDataScope(String employeeId, String scopeType, 
            String scopeId, String source, String operator) {
        // 查询是否已存在
        LambdaQueryWrapper<EmployeeDataScope> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EmployeeDataScope::getEmployeeId, employeeId)
                .eq(EmployeeDataScope::getScopeType, scopeType)
                .eq(EmployeeDataScope::getScopeId, scopeId)
                .eq(EmployeeDataScope::getDeleted, 0);
        EmployeeDataScope existingScope = employeeDataScopeMapper.selectOne(queryWrapper);

        if (existingScope != null) {
            // 已存在，更新状态
            existingScope.setStatus(1);
            existingScope.setSource(source);
            existingScope.setUpdatedBy(operator);
            existingScope.setUpdatedAt(LocalDateTime.now());
            employeeDataScopeMapper.updateById(existingScope);
            return existingScope;
        }

        // 创建新的数据权限
        EmployeeDataScope dataScope = new EmployeeDataScope();
        dataScope.setEmployeeId(employeeId);
        dataScope.setScopeType(scopeType);
        dataScope.setScopeId(scopeId);
        dataScope.setSource(source);
        dataScope.setStatus(1);
        dataScope.setCreatedBy(operator);
        dataScope.setCreatedAt(LocalDateTime.now());

        // 查询权限范围名称
        String scopeName = getScopeName(scopeType, scopeId);
        dataScope.setScopeName(scopeName);

        employeeDataScopeMapper.insert(dataScope);
        return dataScope;
    }

    /**
     * 获取权限范围名称
     */
    private String getScopeName(String scopeType, String scopeId) {
        // TODO: 根据scopeType查询对应的门店或部门名称
        // 这里需要注入StoreMapper和DepartmentMapper
        return scopeId;
    }

    /**
     * 获取当前角色列表
     */
    private List<PermissionAssignmentResultDTO.RoleInfo> getCurrentRoles(String employeeId) {
        List<PermissionAssignmentResultDTO.RoleInfo> roles = new ArrayList<>();
        // TODO: 实现查询当前角色的逻辑
        return roles;
    }

    /**
     * 获取当前数据权限列表
     */
    private List<PermissionAssignmentResultDTO.DataScopeInfo> getCurrentDataScopes(String employeeId) {
        List<EmployeeDataScope> dataScopes = employeeDataScopeMapper.selectByEmployeeId(employeeId);
        return dataScopes.stream().map(scope -> {
            PermissionAssignmentResultDTO.DataScopeInfo info = new PermissionAssignmentResultDTO.DataScopeInfo();
            info.setScopeType(scope.getScopeType());
            info.setScopeId(scope.getScopeId());
            info.setScopeName(scope.getScopeName());
            info.setSource(scope.getSource());
            return info;
        }).collect(Collectors.toList());
    }

    /**
     * 保存权限分配日志
     */
    private void savePermissionLog(PermissionAssignmentDTO dto, Employee employee,
            List<PermissionAssignmentResultDTO.RoleInfo> oldRoles,
            List<PermissionAssignmentResultDTO.DataScopeInfo> oldDataScopes,
            List<PermissionAssignmentResultDTO.RoleInfo> newRoles,
            List<PermissionAssignmentResultDTO.DataScopeInfo> newDataScopes) {
        try {
            PermissionAssignmentLog log = new PermissionAssignmentLog();
            log.setEmployeeId(dto.getEmployeeId());
            log.setOperationType(dto.getOperationType());
            log.setNewPositionId(dto.getPositionId());
            log.setOldRoles(objectMapper.writeValueAsString(oldRoles));
            log.setNewRoles(objectMapper.writeValueAsString(newRoles));
            log.setOldDataScopes(objectMapper.writeValueAsString(oldDataScopes));
            log.setNewDataScopes(objectMapper.writeValueAsString(newDataScopes));
            log.setOperator(dto.getEmployeeId());
            log.setOperatedAt(LocalDateTime.now());
            log.setRemark(dto.getRemark());
            
            permissionAssignmentLogMapper.insert(log);
        } catch (JsonProcessingException e) {
            logger.error("保存权限分配日志失败", e);
        }
    }
}
