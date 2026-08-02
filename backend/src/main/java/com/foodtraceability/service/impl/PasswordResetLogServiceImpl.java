package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.PasswordResetLogQueryRequest;
import com.foodtraceability.entity.PasswordResetLog;
import com.foodtraceability.mapper.PasswordResetLogMapper;
import com.foodtraceability.service.PasswordResetLogService;
import com.foodtraceability.utils.IpUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 密码重置操作日志服务实现
 */
@Service
public class PasswordResetLogServiceImpl extends ServiceImpl<PasswordResetLogMapper, PasswordResetLog> implements PasswordResetLogService {


    public PasswordResetLogServiceImpl(PasswordResetLogMapper passwordResetLogMapper) {
        this.passwordResetLogMapper = passwordResetLogMapper;
    }

    private final PasswordResetLogMapper passwordResetLogMapper;

    @Override
    public void logSendCode(Long userId, String username, String codeType, String clientIp, String deviceType, boolean success, String failureReason) {
        PasswordResetLog log = new PasswordResetLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setOperationType("SEND_CODE");
        log.setOperationStatus(success ? "SUCCESS" : "FAILED");
        log.setOperationDetail("发送" + ("EMAIL".equals(codeType) ? "邮箱" : "手机") + "验证码");
        log.setCodeType(codeType);
        log.setClientIp(clientIp);
        log.setClientLocation(IpUtils.getLocationByIp(clientIp));
        log.setDeviceType(deviceType);
        log.setFailureReason(failureReason);
        log.setCreatedAt(LocalDateTime.now());
        log.setUpdatedAt(LocalDateTime.now());
        
        if (!success) {
            log.setIsAbnormal(1);
            log.setAbnormalType("SEND_FAILED");
        } else {
            log.setIsAbnormal(0);
        }
        
        save(log);
    }

    @Override
    public void logVerifyCode(Long userId, String username, String codeType, String clientIp, String deviceType, boolean success, String failureReason) {
        PasswordResetLog log = new PasswordResetLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setOperationType("VERIFY_CODE");
        log.setOperationStatus(success ? "SUCCESS" : "FAILED");
        log.setOperationDetail("验证" + ("EMAIL".equals(codeType) ? "邮箱" : "手机") + "验证码");
        log.setCodeType(codeType);
        log.setClientIp(clientIp);
        log.setClientLocation(IpUtils.getLocationByIp(clientIp));
        log.setDeviceType(deviceType);
        log.setFailureReason(failureReason);
        log.setCreatedAt(LocalDateTime.now());
        log.setUpdatedAt(LocalDateTime.now());
        
        if (!success) {
            log.setIsAbnormal(1);
            log.setAbnormalType("VERIFY_FAILED");
        } else {
            log.setIsAbnormal(0);
        }
        
        save(log);
    }

    @Override
    public void logResetPassword(Long userId, String username, String clientIp, String deviceType, boolean success, String failureReason) {
        PasswordResetLog log = new PasswordResetLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setOperationType("RESET_PASSWORD");
        log.setOperationStatus(success ? "SUCCESS" : "FAILED");
        log.setOperationDetail("重置密码");
        log.setClientIp(clientIp);
        log.setClientLocation(IpUtils.getLocationByIp(clientIp));
        log.setDeviceType(deviceType);
        log.setFailureReason(failureReason);
        log.setCreatedAt(LocalDateTime.now());
        log.setUpdatedAt(LocalDateTime.now());
        
        if (!success) {
            log.setIsAbnormal(1);
            log.setAbnormalType("RESET_FAILED");
        } else {
            log.setIsAbnormal(0);
        }
        
        save(log);
    }

    @Override
    public void logAbnormalOperation(Long userId, String username, String operationType, String abnormalType, String clientIp, String failureReason) {
        PasswordResetLog log = new PasswordResetLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setOperationType(operationType);
        log.setOperationStatus("FAILED");
        log.setOperationDetail("异常操作：" + abnormalType);
        log.setIsAbnormal(1);
        log.setAbnormalType(abnormalType);
        log.setClientIp(clientIp);
        log.setClientLocation(IpUtils.getLocationByIp(clientIp));
        log.setFailureReason(failureReason);
        log.setCreatedAt(LocalDateTime.now());
        log.setUpdatedAt(LocalDateTime.now());
        
        save(log);
    }

    @Override
    public List<PasswordResetLog> getUserRecentOperations(Long userId, int limit) {
        QueryWrapper<PasswordResetLog> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .orderByDesc("created_at")
                .last("LIMIT " + limit);
        return list(wrapper);
    }

    @Override
    public List<PasswordResetLog> getAbnormalOperations(LocalDateTime startTime, LocalDateTime endTime) {
        QueryWrapper<PasswordResetLog> wrapper = new QueryWrapper<>();
        wrapper.eq("is_abnormal", 1)
                .between("created_at", startTime, endTime)
                .orderByDesc("created_at");
        return list(wrapper);
    }

    @Override
    public int countUserOperations(Long userId, String operationType, LocalDateTime startTime, LocalDateTime endTime) {
        QueryWrapper<PasswordResetLog> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .eq(operationType != null, "operation_type", operationType)
                .between("created_at", startTime, endTime);
        return (int) count(wrapper);
    }

    @Override
    public int countIpOperations(String clientIp, LocalDateTime startTime, LocalDateTime endTime) {
        QueryWrapper<PasswordResetLog> wrapper = new QueryWrapper<>();
        wrapper.eq("client_ip", clientIp)
                .between("created_at", startTime, endTime);
        return (int) count(wrapper);
    }

    @Override
    public IPage<PasswordResetLog> getPageList(PasswordResetLogQueryRequest request) {
        Page<PasswordResetLog> page = new Page<>(request.getPage(), request.getSize());
        QueryWrapper<PasswordResetLog> wrapper = new QueryWrapper<>();
        
        wrapper.eq(request.getUserId() != null, "user_id", request.getUserId())
                .like(request.getUsername() != null, "username", request.getUsername())
                .eq(request.getOperationType() != null, "operation_type", request.getOperationType())
                .eq(request.getOperationStatus() != null, "operation_status", request.getOperationStatus())
                .eq(request.getClientIp() != null, "client_ip", request.getClientIp())
                .eq(request.getIsAbnormal() != null, "is_abnormal", request.getIsAbnormal())
                .eq(request.getAbnormalType() != null, "abnormal_type", request.getAbnormalType())
                .between(request.getStartTime() != null && request.getEndTime() != null, 
                        "created_at", request.getStartTime(), request.getEndTime())
                .orderByDesc("created_at");
        
        return page(page, wrapper);
    }

    @Override
    public boolean detectAbnormalBehavior(Long userId, String operationType, String clientIp) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);
        
        // 检查用户操作频率
        int userOperationCount = countUserOperations(userId, operationType, oneHourAgo, now);
        if (userOperationCount > 10) {
            logAbnormalOperation(userId, "系统检测", operationType, "FREQUENCY_LIMIT", clientIp, 
                    "用户操作频率过高，1小时内操作" + userOperationCount + "次");
            return true;
        }
        
        // 检查IP地址操作频率
        int ipOperationCount = countIpOperations(clientIp, oneHourAgo, now);
        if (ipOperationCount > 20) {
            logAbnormalOperation(userId, "系统检测", operationType, "IP_FREQUENCY_LIMIT", clientIp, 
                    "IP地址操作频率过高，1小时内操作" + ipOperationCount + "次");
            return true;
        }
        
        // 检查异常操作比例
        QueryWrapper<PasswordResetLog> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("is_abnormal", 1)
                .between("created_at", oneHourAgo, now);
        int abnormalCount = (int) count(wrapper);
        
        if (abnormalCount > 5) {
            logAbnormalOperation(userId, "系统检测", operationType, "ABNORMAL_RATIO", clientIp, 
                    "异常操作比例过高，1小时内异常操作" + abnormalCount + "次");
            return true;
        }
        
        return false;
    }
}