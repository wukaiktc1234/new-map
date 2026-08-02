package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.Position;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 职位数据访问层
 */
@Mapper
public interface PositionMapper extends BaseMapper<Position> {

    /**
     * 获取所有职位
     * 注意：position_id 必须显式 AS id，否则自定义 @Select 无法映射到 Position.id Java 字段
     * （@TableId 注解只对 BaseMapper 内置方法生效）
     */
    @Select("SELECT p.position_id AS id, p.position_name, p.position_code, p.department_id, d.dept_name AS department, p.description, p.employee_count, p.head_count, p.status, p.create_time, p.update_time, p.deleted " +
            "FROM positions p " +
            "LEFT JOIN departments d ON p.department_id = d.department_id " +
            "WHERE p.deleted = 0 " +
            "ORDER BY p.position_code, p.create_time")
    List<Position> getAllPositions();

    /**
     * 获取职位列表（带部门排序信息）
     * 按部门排序号、职位编码排序
     * <p>
     * 注意：departments 表主键为 department_id（不是 id），JOIN 条件使用 d.department_id
     * 注意：position_id 必须显式 AS id，否则自定义 @Select 无法映射到 Position.id Java 字段
     */
    @Select("SELECT p.position_id AS id, p.position_name, p.position_code, p.department_id, d.dept_name AS department, p.description, p.employee_count, p.head_count, p.status, p.create_time, p.update_time, p.created_by, p.updated_by, p.deleted, COALESCE(d.sort_order, 999) as department_sort_order " +
            "FROM positions p " +
            "LEFT JOIN departments d ON p.department_id = d.department_id " +
            "WHERE p.deleted = 0 " +
            "ORDER BY COALESCE(d.sort_order, 999), p.position_code")
    Page<Position> selectPositionsWithDepartmentSort(Page<Position> page);

    /**
     * 获取所有职位（带部门排序信息）
     * 按部门排序号、职位编码排序
     * <p>
     * 注意：departments 表主键为 department_id（不是 id），JOIN 条件使用 d.department_id
     * 注意：position_id 必须显式 AS id，否则自定义 @Select 无法映射到 Position.id Java 字段
     */
    @Select("SELECT p.position_id AS id, p.position_name, p.position_code, p.department_id, d.dept_name AS department, p.description, p.employee_count, p.head_count, p.status, p.create_time, p.update_time, p.created_by, p.updated_by, p.deleted, COALESCE(d.sort_order, 999) as department_sort_order " +
            "FROM positions p " +
            "LEFT JOIN departments d ON p.department_id = d.department_id " +
            "WHERE p.deleted = 0 " +
            "ORDER BY COALESCE(d.sort_order, 999), p.position_code")
    List<Position> selectAllPositionsWithDepartmentSort();

    /**
     * 根据部门获取职位列表（带部门排序信息）
     * 按职位编码排序
     * <p>
     * 注意：departments 表主键为 department_id（不是 id），JOIN 条件使用 d.department_id
     * 注意：position_id 必须显式 AS id，否则自定义 @Select 无法映射到 Position.id Java 字段
     */
    @Select("SELECT p.position_id AS id, p.position_name, p.position_code, p.department_id, d.dept_name AS department, p.description, p.employee_count, p.head_count, p.status, p.create_time, p.update_time, p.created_by, p.updated_by, p.deleted, COALESCE(d.sort_order, 999) as department_sort_order " +
            "FROM positions p " +
            "LEFT JOIN departments d ON p.department_id = d.department_id " +
            "WHERE p.deleted = 0 AND p.department_id = #{departmentId} " +
            "ORDER BY p.position_code")
    List<Position> selectPositionsByDepartmentWithSort(@Param("departmentId") Long departmentId);

    /**
     * 分页查询职位（支持部门、状态、关键词筛选）
     */
    Page<Position> selectPositionsWithFilters(
            Page<Position> page,
            @Param("departmentId") Long departmentId,
            @Param("status") Integer status,
            @Param("keyword") String keyword);
}
