package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.User;

import java.util.List;
import java.util.Map;

/**
 * 用户服务接口
 * 提供用户相关的业务逻辑
 */
public interface UserService extends IService<User> {

    /**
     * 分页查询用户列表
     * @param page 分页参数
     * @param username 用户名
     * @param fullName 姓名
     * @param email 邮箱
     * @param phone 手机号
     * @param role 角色
     * @param status 状态
     * @param department 部门
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 用户分页数据
     */
    IPage<User> getUserPage(Page<User> page, String username, String fullName, String email,
                           String phone, String role, String status, String department,
                           String startTime, String endTime);

    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户信息
     */
    User getUserByUsername(String username);

    /**
     * 根据邮箱查询用户
     * @param email 邮箱
     * @return 用户信息
     */
    User getUserByEmail(String email);

    /**
     * 根据手机号查询用户
     * @param phone 手机号
     * @return 用户信息
     */
    User getUserByPhone(String phone);

    /**
     * 创建用户
     * @param user 用户信息
     * @return 创建的用户
     */
    User createUser(User user);

    /**
     * 更新用户信息
     * @param user 用户信息
     * @return 更新后的用户
     */
    User updateUser(User user);

    /**
     * 删除用户
     * @param userId 用户ID
     * @return 是否删除成功
     */
    boolean deleteUser(Long userId);

    /**
     * 重置用户密码
     * @param userId 用户ID
     * @param newPassword 新密码
     * @return 是否重置成功
     */
    boolean resetPassword(Long userId, String newPassword);

    /**
     * 切换用户状态
     * @param userId 用户ID
     * @param status 新状态
     * @return 是否切换成功
     */
    boolean toggleUserStatus(Long userId, Integer status);

    /**
     * 分配门店给用户
     * @param userId 用户ID
     * @param storeId 门店ID
     * @return 是否分配成功
     */
    boolean assignStore(Long userId, Long storeId);

    /**
     * 取消用户的门店分配
     * @param userId 用户ID
     * @return 是否取消成功
     */
    boolean unassignStore(Long userId);

    /**
     * 获取用户的门店信息
     * @param userId 用户ID
     * @return 门店信息
     */
    Map<String, Object> getUserStoreInfo(Long userId);

    /**
     * 检查用户名是否已存在
     * @param username 用户名
     * @return 是否存在
     */
    boolean checkUsernameExists(String username);

    /**
     * 检查邮箱是否已存在
     * @param email 邮箱
     * @return 是否存在
     */
    boolean checkEmailExists(String email);

    /**
     * 检查手机号是否已存在
     * @param phone 手机号
     * @return 是否存在
     */
    boolean checkPhoneExists(String phone);

    /**
     * 统计用户数量
     * @return 用户总数
     */
    long countUsers();

    /**
     * 统计各状态的用户数量
     * @return 状态统计
     */
    List<Map<String, Object>> countByStatus();

    /**
     * 统计各角色的用户数量
     * @return 角色统计
     */
    List<Map<String, Object>> countByRole();

    /**
     * 分配角色给用户
     * @param userId 用户ID
     * @param roleIds 角色ID列表
     * @return 是否分配成功
     */
    boolean assignRoles(Long userId, List<String> roleIds);

    /**
     * 取消用户的角色分配
     * @param userId 用户ID
     * @return 是否取消成功
     */
    boolean unassignRoles(Long userId);

    /**
     * 获取用户的角色列表
     * @param userId 用户ID
     * @return 角色ID列表
     */
    List<String> getUserRoles(Long userId);

    /**
     * 获取用户的角色名称列表
     * @param userId 用户ID
     * @return 角色名称列表
     */
    List<String> getUserRoleNames(Long userId);

    /**
     * 检查用户是否拥有指定角色
     * @param userId 用户ID
     * @param roleCode 角色编码
     * @return 是否拥有该角色
     */
    boolean hasRole(Long userId, String roleCode);

    /**
     * 检查用户是否拥有指定权限
     * @param userId 用户ID
     * @param permissionCode 权限编码
     * @return 是否拥有该权限
     */
    boolean hasPermission(Long userId, String permissionCode);

    /**
     * 统计各部门的用户数量
     * @return 部门统计
     */
    List<Map<String, Object>> countByDepartment();

    // ============ Sprint 2 招聘链路事件接收人查询（新增方法，不修改现有方法） ============

    /**
     * 按门店 ID 查询店长用户 ID 列表（招聘链路事件接收人）。
     *
     * <p>Sprint 2 招聘链路使用：通知门店店长名额下发/确认/拒绝/关闭/即将用完/已用完等事件。</p>
     *
     * @param storeId 门店 ID
     * @return 店长用户 ID 列表（无则返回空列表）
     */
    List<Long> getStoreManagersByStoreId(Long storeId);

    /**
     * 查询所有 HR 招聘员用户 ID 列表（招聘链路事件接收人）。
     *
     * <p>Sprint 2 招聘链路使用：通知 HR 招聘员门店提报需求/重新提交需求/门店面评提交等事件。</p>
     *
     * @return HR 招聘员用户 ID 列表（无则返回空列表）
     */
    List<Long> getHrRecruiters();

    /**
     * 查询所有 HR 经理用户 ID 列表（招聘链路事件接收人）。
     *
     * <p>Sprint 2 招聘链路使用：通知 HR 经理门店申请名额追加/名额即将用完/已用完等事件。</p>
     *
     * @return HR 经理用户 ID 列表（无则返回空列表）
     */
    List<Long> getHrManagers();
}
