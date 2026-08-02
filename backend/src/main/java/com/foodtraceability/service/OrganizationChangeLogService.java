package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.OrganizationChangeLog;

import java.util.List;

/**
 * 组织架构变更日志服务接口
 */
public interface OrganizationChangeLogService extends IService<OrganizationChangeLog> {

    /**
     * 根据变更类型获取变更日志
     * @param changeType 变更类型
     * @return 变更日志列表
     */
    List<OrganizationChangeLog> getLogsByChangeType(Integer changeType);

    /**
     * 根据变更对象ID获取变更日志
     * @param objectId 变更对象ID
     * @return 变更日志列表
     */
    List<OrganizationChangeLog> getLogsByObjectId(Long objectId);

    /**
     * 记录部门变更日志
     * @param departmentId 部门ID
     * @param departmentName 部门名称
     * @param beforeChange 变更前状态
     * @param afterChange 变更后状态
     * @param changeReason 变更原因
     * @param operator 操作人
     * @return 变更日志ID
     */
    Long recordDepartmentChange(Long departmentId, String departmentName, String beforeChange, String afterChange, String changeReason, String operator);

    /**
     * 记录职位变更日志
     * @param positionId 职位ID
     * @param positionName 职位名称
     * @param beforeChange 变更前状态
     * @param afterChange 变更后状态
     * @param changeReason 变更原因
     * @param operator 操作人
     * @return 变更日志ID
     */
    Long recordPositionChange(Long positionId, String positionName, String beforeChange, String afterChange, String changeReason, String operator);

    /**
     * 记录员工变更日志
     * @param employeeId 员工ID
     * @param employeeName 员工姓名
     * @param beforeChange 变更前状态
     * @param afterChange 变更后状态
     * @param changeReason 变更原因
     * @param operator 操作人
     * @return 变更日志ID
     */
    Long recordEmployeeChange(Long employeeId, String employeeName, String beforeChange, String afterChange, String changeReason, String operator);

    /**
     * 获取最近的变更日志
     * @param limit 限制数量
     * @return 变更日志列表
     */
    List<OrganizationChangeLog> getRecentLogs(Integer limit);

    /**
     * 获取变更统计信息
     * @return 变更统计信息
     */
    Object getChangeStatistics();

    /**
     * 记录职位创建日志
     * @param positionId 职位ID
     * @param positionName 职位名称
     * @param departmentId 部门ID
     * @param operator 操作人
     * @param ipAddress IP地址
     */
    void logPositionCreate(String positionId, String positionName, String departmentId, String operator, String ipAddress);

    /**
     * 记录职位更新日志
     * @param positionId 职位ID
     * @param beforeValue 变更前状态
     * @param afterValue 变更后状态
     * @param operator 操作人
     * @param reason 变更原因
     * @param ipAddress IP地址
     */
    void logPositionUpdate(String positionId, String beforeValue, String afterValue, String operator, String reason, String ipAddress);

    /**
     * 记录职位删除日志
     * @param positionId 职位ID
     * @param beforeValue 变更前状态
     * @param operator 操作人
     * @param ipAddress IP地址
     */
    void logPositionDelete(String positionId, String beforeValue, String operator, String ipAddress);

    /**
     * 记录部门创建日志
     * @param departmentId 部门ID
     * @param departmentName 部门名称
     * @param operator 操作人
     * @param ipAddress IP地址
     */
    void logDepartmentCreate(String departmentId, String departmentName, String operator, String ipAddress);

    /**
     * 记录部门更新日志
     * @param departmentId 部门ID
     * @param beforeValue 变更前状态
     * @param afterValue 变更后状态
     * @param operator 操作人
     * @param reason 变更原因
     * @param ipAddress IP地址
     */
    void logDepartmentUpdate(String departmentId, String beforeValue, String afterValue, String operator, String reason, String ipAddress);

    /**
     * 记录部门删除日志
     * @param departmentId 部门ID
     * @param beforeValue 变更前状态
     * @param operator 操作人
     * @param ipAddress IP地址
     */
    void logDepartmentDelete(String departmentId, String beforeValue, String operator, String ipAddress);
}
