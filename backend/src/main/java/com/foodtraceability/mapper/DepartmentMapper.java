package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.Department;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 部门数据访问层
 */
@Mapper
public interface DepartmentMapper extends BaseMapper<Department> {

    /**
     * 获取所有部门（按层级排序）
     * 主键列名为 department_id（遵循 {table}_id 规范）
     *
     * 注意：
     * 1. 必须显式 SELECT type 列（组织类型），否则 DepartmentConverter
     *    无法从实体读取 type 字段，导致前端组织树显示类型不正确。
     * 2. 时间字段已统一为 create_time/update_time（见迁移 V20260717_020），
     *    此处需使用小写列名并通过别名映射到实体 createdAt/updatedAt。
     * 3. manager_name 为数据库列，需显式查询并映射到 managerName。
     */
    @Select("SELECT department_id AS departmentId, dept_code AS departmentCode, dept_name AS departmentName, parent_id AS parentId, level AS level, sort_order AS sortOrder, employee_count AS employeeCount, status AS status, description AS description, base_salary AS baseSalary, store_id AS storeId, type AS type, manager_name AS managerName, create_time AS createdAt, update_time AS updatedAt, created_by AS createdBy, updated_by AS updatedBy, deleted AS deleted FROM departments WHERE deleted = 0 ORDER BY level, COALESCE(sort_order, 0), department_id")
    List<Department> getAllDepartments();

    /**
     * 删除所有部门（软删除）
     */
    @Update("UPDATE departments SET DELETED = 1 WHERE DELETED = 0")
    void deleteAllDepartments();

    /**
     * 根据部门编码统计记录数（包含已逻辑删除的记录）
     * 用于生成唯一编码时避免与已删除数据冲突
     */
    @Select("SELECT COUNT(*) FROM departments WHERE dept_code = #{deptCode}")
    int countByDeptCode(@Param("deptCode") String deptCode);
}
