package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.EmployeeDataScope;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 员工数据权限Mapper接口
 */
@Mapper
public interface EmployeeDataScopeMapper extends BaseMapper<EmployeeDataScope> {

    /**
     * 根据员工ID查询数据权限列表
     * @param employeeId 员工ID
     * @return 数据权限列表
     */
    @Select("SELECT eds.*, e.EMPLOYEE_NAME as employee_name " +
            "FROM employee_data_scope eds " +
            "LEFT JOIN employees e ON eds.employee_id = e.EMPLOYEE_ID " +
            "WHERE eds.employee_id = #{employeeId} AND eds.deleted = 0 AND eds.status = 1")
    List<EmployeeDataScope> selectByEmployeeId(@Param("employeeId") String employeeId);

    /**
     * 根据员工ID和权限范围类型查询数据权限
     * @param employeeId 员工ID
     * @param scopeType 权限范围类型
     * @return 数据权限列表
     */
    @Select("SELECT eds.*, e.EMPLOYEE_NAME as employee_name " +
            "FROM employee_data_scope eds " +
            "LEFT JOIN employees e ON eds.employee_id = e.EMPLOYEE_ID " +
            "WHERE eds.employee_id = #{employeeId} AND eds.scope_type = #{scopeType} AND eds.deleted = 0 AND eds.status = 1")
    List<EmployeeDataScope> selectByEmployeeIdAndScopeType(@Param("employeeId") String employeeId, @Param("scopeType") String scopeType);

    /**
     * 根据权限范围ID查询关联的员工
     * @param scopeType 权限范围类型
     * @param scopeId 权限范围ID
     * @return 数据权限列表
     */
    @Select("SELECT eds.*, e.EMPLOYEE_NAME as employee_name " +
            "FROM employee_data_scope eds " +
            "LEFT JOIN employees e ON eds.employee_id = e.EMPLOYEE_ID " +
            "WHERE eds.scope_type = #{scopeType} AND eds.scope_id = #{scopeId} AND eds.deleted = 0 AND eds.status = 1")
    List<EmployeeDataScope> selectByScopeId(@Param("scopeType") String scopeType, @Param("scopeId") String scopeId);
}
