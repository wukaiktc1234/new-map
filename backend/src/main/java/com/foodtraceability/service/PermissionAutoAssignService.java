package com.foodtraceability.service;

import com.foodtraceability.dto.PermissionAssignmentDTO;
import com.foodtraceability.dto.PermissionAssignmentResultDTO;
import com.foodtraceability.entity.EmployeeDataScope;
import com.foodtraceability.entity.PositionRoleMapping;

import java.util.List;

/**
 * 权限自动分配服务接口
 */
public interface PermissionAutoAssignService {

    /**
     * 自动分配权限（核心方法）
     * 根据员工的职位、门店、部门信息自动分配角色和数据权限
     * 
     * @param dto 权限分配请求DTO
     * @return 权限分配结果
     */
    PermissionAssignmentResultDTO autoAssignPermission(PermissionAssignmentDTO dto);

    /**
     * 员工入职时自动分配权限
     * 
     * @param employeeId 员工ID
     * @param positionId 职位ID
     * @param storeId 门店ID（可选）
     * @param departmentId 部门ID（可选）
     * @param operator 操作人
     * @return 权限分配结果
     */
    PermissionAssignmentResultDTO assignOnOnboarding(String employeeId, String positionId, 
            String storeId, String departmentId, String operator);

    /**
     * 员工职位变更时自动调整权限
     * 
     * @param employeeId 员工ID
     * @param oldPositionId 原职位ID
     * @param newPositionId 新职位ID
     * @param operator 操作人
     * @return 权限分配结果
     */
    PermissionAssignmentResultDTO assignOnPositionChange(String employeeId, String oldPositionId, 
            String newPositionId, String operator);

    /**
     * 员工门店分配时自动调整数据权限
     * 
     * @param employeeId 员工ID
     * @param storeId 门店ID
     * @param operator 操作人
     * @return 权限分配结果
     */
    PermissionAssignmentResultDTO assignOnStoreAssignment(String employeeId, String storeId, String operator);

    /**
     * 根据职位ID获取关联的角色列表
     * 
     * @param positionId 职位ID
     * @return 角色映射列表
     */
    List<PositionRoleMapping> getRolesByPositionId(Long positionId);

    /**
     * 根据员工ID获取数据权限列表
     * 
     * @param employeeId 员工ID
     * @return 数据权限列表
     */
    List<EmployeeDataScope> getDataScopesByEmployeeId(String employeeId);

    /**
     * 清除员工的自动分配权限
     * 
     * @param employeeId 员工ID
     * @param operator 操作人
     * @return 是否成功
     */
    boolean clearAutoAssignedPermissions(String employeeId, String operator);

    /**
     * 重新计算员工权限
     * 当职位-角色映射关系变更时，重新计算所有相关员工的权限
     * 
     * @param positionId 职位ID
     * @param operator 操作人
     * @return 影响的员工数量
     */
    int recalculatePermissionsByPosition(Long positionId, String operator);
}
