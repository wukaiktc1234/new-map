package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.Employee;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 员工Mapper接口
 */
public interface EmployeeMapper extends BaseMapper<Employee> {

    /**
     * 统计指定部门下的在职员工数量
     * <p>
     * 实现说明：
     * 1. employees.department_id 列当前为 BIGINT 类型
     * 2. employees.status 列为 INTEGER 类型：1-在职，0-离职，2-试用期
     * 3. 使用 CAST 将参数转为 BIGINT，兼容 department_id 的数值比较
     * 4. 统计时包含在职(1)和试用期(2)员工，确保删除校验准确
     *
     * @param departmentId 部门ID（字符串形式）
     * @return 该部门下的在职员工数量
     */
    @Select("SELECT COUNT(*) FROM employees WHERE department_id = CAST(#{departmentId} AS BIGINT) AND status IN (1, 2) AND deleted = 0")
    int countActiveByDepartmentId(@Param("departmentId") String departmentId);
}
