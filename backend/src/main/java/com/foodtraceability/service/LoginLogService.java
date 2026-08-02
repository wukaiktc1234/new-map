package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.LoginLogQueryRequest;
import com.foodtraceability.entity.LoginLog;

public interface LoginLogService {
    
    void recordLoginSuccess(Long userId, String username, String loginIp, String sessionId, 
                          String deviceType, String browser, String os);
    
    void recordLoginFailure(String username, String loginIp, String failureReason);
    
    void recordLogout(String sessionId);
    
    IPage<LoginLog> getLoginLogs(LoginLogQueryRequest request);
    
    LoginLog getLoginLogById(Long id);
    
    void save(LoginLog loginLog);
}