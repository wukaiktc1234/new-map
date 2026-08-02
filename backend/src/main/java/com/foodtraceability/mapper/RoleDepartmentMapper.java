package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.RoleDepartment;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色部门关联Mapper接口
 * 提供角色部门关联的数据库操作方法
 */
@Mapper
public interface RoleDepartmentMapper extends BaseMapper<RoleDepartment> {

    /**
     * 根据角色ID查询部门ID列表
     * @param roleId 角色ID
     * @return 部门ID列表
     */
    @Select("SELECT department_id FROM role_departments WHERE role_id = #{roleId}")
    List<String> selectDepartmentIdsByRoleId(@Param("roleId") String roleId);

    /**
     * 根据角色ID删除所有部门关联
     * @param roleId 角色ID
     * @return 删除数量
     */
    @Delete("DELETE FROM role_departments WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 批量插入角色部门关联
     * @param roleDepartments 角色部门关联列表
     * @return 插入数量
     */
    int batchInsert(@Param("list") List<RoleDepartment> roleDepartments);

    /**
     * 根据部门ID查询角色ID列表
     * @param departmentId 部门ID
     * @return 角色ID列表
     */
    @Select("SELECT role_id FROM role_departments WHERE department_id = #{departmentId}")
    List<String> selectRoleIdsByDepartmentId(@Param("departmentId") String departmentId);

    /**
     * 批量查询多个角色关联的部门ID列表（性能优化）
     * 使用IN查询替代循环查询，解决N+1问题
     *
     * @param roleIds 角色ID列表
     * @return 部门ID列表（可能包含重复，调用方需去重）
     */
    @Select("<script>" +
            "SELECT department_id FROM role_departments " +
            "WHERE role_id IN " +
            "<foreach item='roleId' collection='roleIds' open='(' separator=',' close=')'>" +
            "#{roleId}" +
            "</foreach>" +
            "</script>")
    List<String> selectDepartmentIdsByRoleIds(@Param("roleIds") List<String> roleIds);
}
