package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.entity.User;
import com.foodtraceability.entity.StatusCount;
import com.foodtraceability.entity.RoleCount;
import com.foodtraceability.entity.DepartmentCount;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户Mapper接口
 * 提供用户相关的数据库操作方法
 */
@Repository
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户ID查询用户角色
     * 注：user_roles.role_id 存储的是 roles.role_id_str（带 ROLE_ 前缀的字符串），
     *     与 role_permissions.role_id 保持一致；通过 role_id_str 关联可兼容权限查询。
     */
    @Select("SELECT r.ROLE_CODE FROM ROLES r INNER JOIN USER_ROLES ur ON r.role_id_str = ur.ROLE_ID WHERE ur.USER_ID::bigint = #{userId} AND ur.DELETED = 0 AND r.DELETED = 0 AND r.STATUS IN ('1', 'active')")
    List<String> getUserRoles(@Param("userId") Long userId);

    /**
     * 根据用户ID查询用户权限
     */
    List<String> getUserPermissions(@Param("userId") Long userId);

    /**
     * 根据用户ID查询用户角色权限
     */
    List<String> getUserRolePermissions(@Param("userId") Long userId);

    /**
     * 根据角色查询用户
     */
    @Select("SELECT * FROM users WHERE role = #{role} AND deleted = 0")
    List<User> findByRole(@Param("role") String role);

    /**
     * 根据状态查询用户
     */
    @Select("SELECT * FROM users WHERE status = #{status} AND deleted = 0")
    List<User> findByStatus(@Param("status") String status);

    /**
     * 根据部门查询用户
     */
    @Select("SELECT * FROM users WHERE department = #{department} AND deleted = 0")
    List<User> findByDepartment(@Param("department") String department);

    /**
     * 查询锁定用户
     */
    @Select("SELECT * FROM users WHERE status = 'locked' AND locked_until > NOW() AND deleted = 0")
    List<User> findLockedUsers();

    /**
     * 查询过期锁定用户（可以解锁的）
     */
    @Select("SELECT * FROM users WHERE status = 'locked' AND locked_until <= NOW() AND deleted = 0")
    List<User> findExpiredLockedUsers();

    /**
     * 查询需要修改密码的用户
     */
    @Select("SELECT * FROM users WHERE need_password_change = true AND deleted = 0")
    List<User> findUsersNeedPasswordChange();

    /**
     * 查询长时间未登录的用户
     */
    @Select("SELECT * FROM users WHERE last_login_time < #{lastLoginTime} AND deleted = 0")
    List<User> findInactiveUsers(@Param("lastLoginTime") LocalDateTime lastLoginTime);

    /**
     * 条件分页查询用户
     */
    IPage<User> findUsersByConditionPage(
            Page<User> page,
            @Param("username") String username,
            @Param("fullName") String fullName,
            @Param("email") String email,
            @Param("phone") String phone,
            @Param("role") String role,
            @Param("status") String status,
            @Param("department") String department,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 统计用户数量
     */
    @Select("SELECT COUNT(*) FROM users WHERE deleted = 0")
    long countUsers();

    /**
     * 统计各状态的用户数量
     */
    @Select("SELECT status, COUNT(*) as count FROM users WHERE deleted = 0 GROUP BY status")
    List<StatusCount> countByStatus();

    /**
     * 统计各角色的用户数量
     */
    @Select("SELECT role, COUNT(*) as count FROM users WHERE deleted = 0 GROUP BY role")
    List<RoleCount> countByRole();

    /**
     * 统计各部门的用户数量
     */
    @Select("SELECT department, COUNT(*) as count FROM users WHERE deleted = 0 GROUP BY department")
    List<DepartmentCount> countByDepartment();

    /**
     * 检查用户名是否已存在
     */
    @Select("SELECT COUNT(*) > 0 FROM users WHERE username = #{username} AND deleted = 0")
    boolean existsByUsername(@Param("username") String username);

    /**
     * 检查邮箱是否已存在
     */
    @Select("SELECT COUNT(*) > 0 FROM users WHERE email = #{email} AND deleted = 0")
    boolean existsByEmail(@Param("email") String email);

    /**
     * 检查手机号是否已存在
     */
    @Select("SELECT COUNT(*) > 0 FROM users WHERE phone = #{phone} AND deleted = 0")
    boolean existsByPhone(@Param("phone") String phone);

    /**
     * 取消用户门店分配（设置store_id为null）
     */
    @Update("UPDATE users SET store_id = NULL, updated_at = NOW() WHERE user_id = #{userId}")
    int unassignStore(@Param("userId") Long userId);

    /**
     * 按门店 ID + 角色编码查询用户 ID 列表（用于招聘链路事件接收人查询）。
     *
     * <p>JOIN user_roles + roles 表，按 r.role_code 过滤，按 u.store_id 过滤。
     * 仅返回启用状态的用户 ID。</p>
     *
     * @param storeId  门店 ID（users.store_id 为 VARCHAR，参数传入字符串形式）
     * @param roleCode 角色编码（大写，如 STORE_MANAGER）
     * @return 用户 ID 列表
     */
    @Select("SELECT u.user_id FROM users u " +
            "INNER JOIN user_roles ur ON u.user_id = ur.user_id::bigint " +
            "INNER JOIN roles r ON ur.role_id::bigint = r.role_id " +
            "WHERE u.store_id = #{storeId} " +
            "  AND r.role_code = #{roleCode} " +
            "  AND u.deleted = 0 " +
            "  AND r.deleted = 0")
    List<Long> findUserIdsByStoreIdAndRoleCode(@Param("storeId") String storeId,
                                               @Param("roleCode") String roleCode);

    /**
     * 按角色编码查询所有用户 ID 列表（用于招聘链路事件接收人查询 HR 角色）。
     *
     * @param roleCode 角色编码（大写，如 HR_RECRUITER / HR_MANAGER）
     * @return 用户 ID 列表
     */
    @Select("SELECT u.user_id FROM users u " +
            "INNER JOIN user_roles ur ON u.user_id = ur.user_id::bigint " +
            "INNER JOIN roles r ON ur.role_id::bigint = r.role_id " +
            "WHERE r.role_code = #{roleCode} " +
            "  AND u.deleted = 0 " +
            "  AND r.deleted = 0")
    List<Long> findUserIdsByRoleCode(@Param("roleCode") String roleCode);
}