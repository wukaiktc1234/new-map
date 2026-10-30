package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.Role;
import com.foodtraceability.entity.StatusCount;
import com.foodtraceability.entity.RoleTypeCount;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
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

    /**
     * P0 fix (Lead 2026-10-30): real role permissions live in role_permissions,
     * keyed by role_id_str (e.g. 'ROLE_CASHIER'), not by numeric role_id.
     */
    @Select("SELECT p.permission_code FROM role_permissions rp "
            + "JOIN permissions p ON p.permission_id_str = rp.permission_id AND p.deleted = 0 "
            + "WHERE rp.role_id = #{roleIdStr} AND rp.deleted = 0 ORDER BY p.permission_code")
    List<String> selectPermissionCodesByRoleIdStr(@Param("roleIdStr") String roleIdStr);

    /** P0 fix: 校验权限码有效数量（用于 M<N 回滚判断） */
    @Select("SELECT COUNT(*) FROM permissions WHERE permission_code = ANY(#{codes}) AND deleted = 0")
    int countValidCodes(@Param("codes") String[] codes);

    /** P0 fix: 软删目标角色的全部旧授权（仅动目标 role_id） */
    @Update("UPDATE role_permissions SET deleted = 1 WHERE role_id = #{roleIdStr} AND deleted = 0")
    int softDeleteByRoleIdStr(@Param("roleIdStr") String roleIdStr);

    /** P0 fix: 一条 INSERT...SELECT 完成 code -> PERM_* 转换 + upsert */
    @Insert("INSERT INTO role_permissions (role_id, permission_id, deleted, create_time) "
            + "SELECT #{roleIdStr}, p.permission_id_str, 0, NOW() FROM permissions p "
            + "WHERE p.permission_code = ANY(#{codes}) AND p.deleted = 0 "
            + "ON CONFLICT (role_id, permission_id) DO UPDATE SET deleted = 0")
    int upsertRolePermissions(@Param("roleIdStr") String roleIdStr, @Param("codes") String[] codes);
}
