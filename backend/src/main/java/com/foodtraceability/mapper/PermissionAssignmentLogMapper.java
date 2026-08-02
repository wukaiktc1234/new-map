package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.PermissionAssignmentLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 权限分配日志Mapper接口
 */
@Mapper
public interface PermissionAssignmentLogMapper extends BaseMapper<PermissionAssignmentLog> {

    /**
     * 根据员工ID查询权限分配日志
     * @param employeeId 员工ID
     * @return 日志列表
     */
    @Select("SELECT pal.*, e.EMPLOYEE_NAME as employee_name, " +
            "p1.position_name as old_position_name, " +
            "p2.position_name as new_position_name " +
            "FROM permission_assignment_log pal " +
            "LEFT JOIN employees e ON pal.employee_id = e.EMPLOYEE_ID " +
            "LEFT JOIN positions p1 ON pal.old_position_id = p1.id " +
            "LEFT JOIN positions p2 ON pal.new_position_id = p2.id " +
            "WHERE pal.employee_id = #{employeeId} " +
            "ORDER BY pal.operated_at DESC")
    List<PermissionAssignmentLog> selectByEmployeeId(@Param("employeeId") String employeeId);

    /**
     * 根据操作类型查询权限分配日志
     * @param operationType 操作类型
     * @param limit 限制条数
     * @return 日志列表
     */
    @Select("SELECT pal.*, e.EMPLOYEE_NAME as employee_name, " +
            "p1.position_name as old_position_name, " +
            "p2.position_name as new_position_name " +
            "FROM permission_assignment_log pal " +
            "LEFT JOIN employees e ON pal.employee_id = e.EMPLOYEE_ID " +
            "LEFT JOIN positions p1 ON pal.old_position_id = p1.id " +
            "LEFT JOIN positions p2 ON pal.new_position_id = p2.id " +
            "WHERE pal.operation_type = #{operationType} " +
            "ORDER BY pal.operated_at DESC " +
            "LIMIT #{limit}")
    List<PermissionAssignmentLog> selectByOperationType(@Param("operationType") String operationType, @Param("limit") int limit);
}
