package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.Role;
import com.foodtraceability.entity.StatusCount;
import com.foodtraceability.entity.RoleTypeCount;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色Mapper接口
 * 提供角色相关的数据库操作方法
 * 注意：实体类已配置@TableLogic注解，MyBatis-Plus自动处理逻辑删除，无需手动添加deleted条件
 */
public interface RoleMapper extends BaseMapper<Role> {

    /**
     * 根据角色编码查询角色
     */
    @Select("SELECT * FROM roles WHERE ROLE_CODE = #{roleCode}")
    Role findByRoleCode(@Param("roleCode") String roleCode);

    /**
     * 根据角色名称查询角色
     */
    @Select("SELECT * FROM roles WHERE ROLE_NAME = #{roleName}")
    Role findByRoleName(@Param("roleName") String roleName);

    /**
     * 根据角色状态查询角色
     * @param status 状态值（active/inactive）
     */
    @Select("SELECT * FROM roles WHERE STATUS = #{status}")
    List<Role> findByStatus(@Param("status") String status);

    /**
     * 根据角色类型查询角色
     */
    @Select("SELECT * FROM roles WHERE ROLE_TYPE = #{roleType}")
    List<Role> findByRoleType(@Param("roleType") Integer roleType);

    /**
     * 查询系统内置角色
     */
    @Select("SELECT * FROM roles WHERE IS_SYSTEM = true")
    List<Role> findSystemBuiltRoles();

    /**
     * 查询自定义角色
     */
    @Select("SELECT * FROM roles WHERE IS_SYSTEM = false")
    List<Role> findCustomRoles();

    /**
     * 查询指定级别的角色
     */
    @Select("SELECT * FROM roles WHERE LEVEL >= #{minLevel} AND LEVEL <= #{maxLevel}")
    List<Role> findByRoleLevelRange(@Param("minLevel") Integer minLevel, @Param("maxLevel") Integer maxLevel);

    /**
     * 条件分页查询角色
     */
    IPage<Role> findRolesByConditionPage(
            Page<Role> page,
            @Param("roleCode") String roleCode,
            @Param("roleName") String roleName,
            @Param("status") String status,
            @Param("roleType") Integer roleType,
            @Param("systemBuilt") Boolean systemBuilt
    );

    /**
     * 统计角色数量
     */
    @Select("SELECT COUNT(*) FROM roles")
    long countRoles();

    /**
     * 统计各状态的角色数量
     */
    @Select("SELECT STATUS, COUNT(*) as count FROM roles GROUP BY STATUS")
    List<StatusCount> countByStatus();

    /**
     * 统计各类型角色的数量
     */
    @Select("SELECT ROLE_TYPE, COUNT(*) as count FROM roles GROUP BY ROLE_TYPE")
    List<RoleTypeCount> countByRoleType();

    /**
     * 检查角色编码是否已存在
     */
    @Select("SELECT COUNT(*) FROM roles WHERE ROLE_CODE = #{roleCode}")
    boolean existsByRoleCode(@Param("roleCode") String roleCode);

    /**
     * 检查角色名称是否已存在
     */
    @Select("SELECT COUNT(*) FROM roles WHERE ROLE_NAME = #{roleName}")
    boolean existsByRoleName(@Param("roleName") String roleName);

    /**
     * 检查角色名称是否已存在（排除指定ID）
     */
    @Select("SELECT COUNT(*) FROM roles WHERE ROLE_NAME = #{roleName} AND ID != #{roleId}")
    boolean existsByNameAndNotId(@Param("roleName") String roleName, @Param("roleId") String roleId);

    /**
     * 获取最大角色级别
     */
    @Select("SELECT MAX(LEVEL) FROM roles")
    Integer getMaxRoleLevel();

    /**
     * 获取最小角色级别
     */
    @Select("SELECT MIN(LEVEL) FROM roles")
    Integer getMinRoleLevel();

    /**
     * 查询所有使用指定角色的用户ID列表
     * 用于权限缓存刷新：角色变更时需要清除相关用户的权限缓存
     *
     * @param roleId 角色ID
     * @return 用户ID列表
     */
    @Select("SELECT USER_ID FROM user_roles WHERE ROLE_ID = #{roleId}")
    List<String> findUserIdsByRoleId(@Param("roleId") Long roleId);
}
