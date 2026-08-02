package com.foodtraceability.service;

import com.foodtraceability.service.impl.DataPermissionServiceImpl.DataScopeCondition;
import java.util.List;

/**
 * 数据权限服务接口
 * 处理数据权限过滤逻辑，支持门店、部门等多种数据权限范围
 *
 * 安全特性说明（v2.0）：
 * ✅ 所有方法均使用参数化查询，彻底避免SQL注入风险
 * ✅ 使用DataScopeCondition对象封装过滤条件，不直接拼接SQL
 * ✅ 严格的输入验证和白名单机制
 * ✅ 已移除所有不安全的SQL构建方法
 *
 * @see DataPermissionServiceImpl.DataScopeCondition 参数化条件对象
 */
public interface DataPermissionService {

    /**
     * 构建数据权限条件（唯一推荐的安全方法）
     *
     * 返回参数化的条件对象，由调用方在MyBatis Mapper中使用
     * 该方法不直接构建SQL字符串，而是返回包含参数的条件容器
     *
     * 安全机制：
     * - 使用DataScopeCondition封装参数，避免SQL注入
     * - 条件类型限定为预定义枚举值
     * - 所有输入值经过白名单验证
     *
     * @param userId 用户ID（必须经过身份验证）
     * @param tableName 表名（保留兼容性，当前未使用）
     * @param alias 表别名（用于字段前缀）
     * @return DataScopeCondition 参数化条件对象，null表示无限制（管理员）
     */
    DataScopeCondition buildDataScopeCondition(String userId, String tableName, String alias);

    /**
     * 获取用户可访问的门店ID列表
     * @param userId 用户ID
     * @return 门店ID列表
     */
    List<String> getAccessibleStoreIds(String userId);

    /**
     * 获取用户可访问的部门ID列表
     * @param userId 用户ID
     * @return 部门ID列表
     */
    List<String> getAccessibleDepartmentIds(String userId);

    /**
     * 检查用户是否有数据访问权限
     * @param userId 用户ID
     * @param tableName 表名
     * @param dataId 数据ID
     * @return 是否有权限
     */
    boolean checkDataAccess(String userId, String tableName, String dataId);

    /**
     * 获取用户的数据权限范围
     * @param userId 用户ID
     * @return 数据权限范围
     */
    String getUserDataScope(String userId);

    /**
     * 检查用户是否可以访问指定门店
     * @param userId 用户ID
     * @param storeId 门店ID
     * @return 是否可以访问
     */
    boolean canAccessStore(String userId, String storeId);

    /**
     * 检查用户是否可以访问指定部门
     * @param userId 用户ID
     * @param departmentId 部门ID
     * @return 是否可以访问
     */
    boolean canAccessDepartment(String userId, String departmentId);

    /**
     * 获取用户可访问的门店ID列表（基于角色）
     * @param userId 用户ID
     * @return 门店ID列表
     */
    List<String> getRoleAccessibleStoreIds(String userId);

    /**
     * 获取用户可访问的部门ID列表（基于角色）
     * @param userId 用户ID
     * @return 部门ID列表
     */
    List<String> getRoleAccessibleDepartmentIds(String userId);
}
