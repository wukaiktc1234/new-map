package com.foodtraceability.service;

import com.foodtraceability.entity.User;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据导出服务接口
 */
public interface DataExportService {

    /**
     * 查询用户数据（带分页限制，用于导出）
     * @param role 角色过滤
     * @param status 状态过滤
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 用户列表
     */
    List<User> queryUsersForExport(String role, String status, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 获取用户角色字符串
     * @param userId 用户ID
     * @return 角色字符串（逗号分隔）
     */
    String getRolesString(Long userId);

    /**
     * 邮箱脱敏
     * @param email 原始邮箱
     * @return 脱敏后的邮箱
     */
    String maskEmail(String email);

    /**
     * 手机号脱敏
     * @param phone 原始手机号
     * @return 脱敏后的手机号
     */
    String maskPhone(String phone);

    /**
     * 格式化日期时间
     * @param dt 日期时间
     * @return 格式化后的字符串
     */
    String formatDateTime(LocalDateTime dt);

    /**
     * CSV字段转义
     * @param value 原始值
     * @return 转义后的值
     */
    String escapeCsv(String value);

    /**
     * 检查用户是否拥有指定角色
     * @param userId 用户ID
     * @param role 角色编码
     * @return 是否拥有该角色
     */
    boolean hasRole(Long userId, String role);
}
