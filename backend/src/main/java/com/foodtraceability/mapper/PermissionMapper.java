package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.Permission;
import com.foodtraceability.entity.PermissionTypeCount;
import com.foodtraceability.entity.ModuleCount;
import com.foodtraceability.entity.StatusCount;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 权限Mapper接口
 * 提供权限相关的数据库操作方法
 * 注意：实体类已配置@TableLogic注解，MyBatis-Plus自动处理逻辑删除，无需手动添加deleted条件
 */
public interface PermissionMapper extends BaseMapper<Permission> {

    /**
     * 根据权限编码查询
     */
    @Select("SELECT * FROM permissions WHERE permission_code = #{permissionCode}")
    Permission findByPermissionCode(String permissionCode);

    /**
     * 根据权限名称查询权限
     */
    @Select("SELECT * FROM permissions WHERE permission_name = #{permissionName}")
    Permission findByPermissionName(String permissionName);

    /**
     * 根据权限类型查询权限
     */
    @Select("SELECT * FROM permissions WHERE permission_type = #{permissionType}")
    List<Permission> findByPermissionType(String permissionType);

    /**
     * 根据权限状态查询权限
     */
    @Select("SELECT * FROM permissions WHERE status = #{status}")
    List<Permission> findByStatus(String status);

    /**
     * 根据所属模块查询权限
     */
    @Select("SELECT * FROM permissions WHERE module = #{module}")
    List<Permission> findByModule(String module);

    /**
     * 根据父权限ID查询子权限
     */
    @Select("SELECT * FROM permissions WHERE parent_id = #{parentId} ORDER BY sort_order")
    List<Permission> findByParentId(String parentId);

    /**
     * 查询根权限（无父权限）
     */
    @Select("SELECT * FROM permissions WHERE parent_id IS NULL OR parent_id = '' ORDER BY sort_order")
    List<Permission> findRootPermissions();

    /**
     * 查询启用的权限
     */
    @Select("SELECT * FROM permissions WHERE enabled = true ORDER BY sort_order")
    List<Permission> findEnabledPermissions();

    /**
     * 查询需要授权的权限
     */
    @Select("SELECT * FROM permissions WHERE require_auth = true ORDER BY sort_order")
    List<Permission> findRequireAuthPermissions();

    /**
     * 查询菜单权限
     */
    @Select("SELECT * FROM permissions WHERE permission_type = 'menu' ORDER BY sort_order")
    List<Permission> findMenuPermissions();

    /**
     * 查询操作权限
     */
    @Select("SELECT * FROM permissions WHERE permission_type = 'button' ORDER BY sort_order")
    List<Permission> findOperationPermissions();

    /**
     * 根据权限级别查询权限
     */
    @Select("SELECT * FROM permissions WHERE permission_level = #{permissionLevel}")
    List<Permission> findByPermissionLevel(@Param("permissionLevel") Integer permissionLevel);

    /**
     * 查询指定级别范围的权限
     */
    @Select("SELECT * FROM permissions WHERE permission_level >= #{minLevel} AND permission_level <= #{maxLevel}")
    List<Permission> findByPermissionLevelRange(@Param("minLevel") Integer minLevel, @Param("maxLevel") Integer maxLevel);

    /**
     * 条件分页查询权限
     */
    IPage<Permission> findPermissionsByConditionPage(
            Page<Permission> page,
            String permissionCode,
            String permissionName,
            String permissionType,
            String status,
            String module,
            String parentId,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    /**
     * 统计权限数量
     */
    @Select("SELECT COUNT(*) FROM permissions")
    long countPermissions();

    /**
     * 统计各类型的权限数量
     */
    @Select("SELECT permission_type AS permissionType, COUNT(*) AS count FROM permissions GROUP BY permission_type ORDER BY count DESC")
    List<PermissionTypeCount> countByPermissionType();

    /**
     * 统计各模块的权限数量
     */
    @Select("SELECT module, COUNT(*) AS count FROM permissions GROUP BY module ORDER BY count DESC")
    List<ModuleCount> countByModule();

    /**
     * 按类型统计权限数量
     */
    @Select("SELECT permission_type AS type, COUNT(*) AS count FROM permissions GROUP BY permission_type ORDER BY count DESC")
    List<PermissionTypeCount> countByType();

    /**
     * 按状态统计权限数量
     */
    @Select("SELECT status, COUNT(*) AS count FROM permissions GROUP BY status ORDER BY count DESC")
    List<StatusCount> countByStatus();

    /**
     * 查询数据权限
     */
    @Select("SELECT * FROM permissions WHERE permission_type = 'data' ORDER BY sort_order")
    List<Permission> findDataPermissions();

    /**
     * 检查权限编码是否已存在
     */
    @Select("SELECT COUNT(*) FROM permissions WHERE permission_code = #{permissionCode}")
    boolean existsByPermissionCode(String permissionCode);

    /**
     * 检查权限名称是否已存在
     */
    @Select("SELECT COUNT(*) FROM permissions WHERE permission_name = #{permissionName}")
    boolean existsByPermissionName(@Param("permissionName") String permissionName);



    /**
     * 获取最大排序号
     */
    @Select("SELECT MAX(sort_order) FROM permissions")
    Integer getMaxSortOrder();

    /**
     * 获取指定模块的最大排序号
     */
    @Select("SELECT MAX(sort_order) FROM permissions WHERE module = #{module}")
    Integer getMaxSortOrderByModule(@Param("module") String module);
}
