package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 系统权限Mapper接口
 */
@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    /**
     * 根据角色ID查询权限
     */
    @Select("SELECT p.* FROM sys_permissions p " +
            "INNER JOIN sys_role_permissions rp ON p.id = rp.permission_id " +
            "WHERE rp.role_id = #{roleId} AND p.is_enabled = 1 " +
            "ORDER BY p.sort_order")
    List<SysPermission> selectPermissionsByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据用户ID查询权限
     */
    @Select("SELECT DISTINCT p.* FROM sys_permissions p " +
            "INNER JOIN sys_role_permissions rp ON p.id = rp.permission_id " +
            "INNER JOIN sys_user_roles ur ON rp.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND p.is_enabled = 1 " +
            "ORDER BY p.sort_order")
    List<SysPermission> selectPermissionsByUserId(@Param("userId") String userId);

    /**
     * 根据模块查询权限
     */
    @Select("SELECT * FROM sys_permissions WHERE module_code = #{moduleCode} AND is_enabled = 1 ORDER BY sort_order")
    List<SysPermission> selectByModule(@Param("moduleCode") String moduleCode);
}
