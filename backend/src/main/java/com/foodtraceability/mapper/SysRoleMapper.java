package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 系统角色Mapper接口
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 根据用户ID查询角色
     */
    @Select("SELECT r.* FROM sys_roles r " +
            "INNER JOIN sys_user_roles ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.is_enabled = 1")
    List<SysRole> selectRolesByUserId(@Param("userId") String userId);

    /**
     * 查询系统预设角色
     */
    @Select("SELECT * FROM sys_roles WHERE is_system = 1 AND is_enabled = 1 ORDER BY sort_order")
    List<SysRole> selectSystemRoles();
}
