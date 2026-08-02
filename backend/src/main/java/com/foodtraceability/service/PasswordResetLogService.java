package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.PasswordResetLog;
import com.foodtraceability.dto.PasswordResetLogQueryRequest;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 密码重置操作日志服务接口
 */
public interface PasswordResetLogService extends IService<PasswordResetLog> {

    /**
     * 记录验证码发送日志
     */
    void logSendCode(Long userId, String username, String codeType, String clientIp, String deviceType, boolean success, String failureReason);

    /**
     * 记录验证码验证日志
     */
    void logVerifyCode(Long userId, String username, String codeType, String clientIp, String deviceType, boolean success, String failureReason);

    /**
     * 记录密码重置日志
     */
    void logResetPassword(Long userId, String username, String clientIp, String deviceType, boolean success, String failureReason);

    /**
     * 记录异常操作日志
     */
    void logAbnormalOperation(Long userId, String username, String operationType, String abnormalType, String clientIp, String failureReason);

    /**
     * 查询用户的最近操作记录
     */
    List<PasswordResetLog> getUserRecentOperations(Long userId, int limit);

    /**
     * 查询异常操作记录
     */
    List<PasswordResetLog> getAbnormalOperations(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计用户操作次数
     */
    int countUserOperations(Long userId, String operationType, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计IP地址的操作次数
     */
    int countIpOperations(String clientIp, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 分页查询操作日志
     */
    IPage<PasswordResetLog> getPageList(PasswordResetLogQueryRequest request);

    /**
     * 检测异常行为
     */
    boolean detectAbnormalBehavior(Long userId, String operationType, String clientIp);
}