package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.LoginLogQueryRequest;
import com.foodtraceability.entity.LoginLog;
import com.foodtraceability.mapper.LoginLogMapper;
import com.foodtraceability.service.LoginLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LoginLogServiceImpl implements LoginLogService {

    private static final Logger logger = LoggerFactory.getLogger(LoginLogServiceImpl.class);


    public LoginLogServiceImpl(LoginLogMapper loginLogMapper) {
        this.loginLogMapper = loginLogMapper;
    }

    private final LoginLogMapper loginLogMapper;

    @Override
    public void recordLoginSuccess(Long userId, String username, String loginIp, String sessionId, 
                                String deviceType, String browser, String os) {
        LoginLog loginLog = new LoginLog();
        loginLog.setUserId(userId);
        loginLog.setUsername(username);
        loginLog.setLoginTime(LocalDateTime.now());
        loginLog.setLoginIp(loginIp);
        loginLog.setDeviceType(deviceType);
        loginLog.setBrowser(browser);
        loginLog.setOs(os);
        loginLog.setLoginStatus(1);
        loginLog.setSessionId(sessionId);
        loginLog.setCreatedAt(LocalDateTime.now());
        
        loginLogMapper.insert(loginLog);
        
        logger.info("Login success recorded: userId={}, username={}, ip={}, sessionId={}", 
            userId, username, loginIp, sessionId);
    }

    @Override
    public void recordLoginFailure(String username, String loginIp, String failureReason) {
        LoginLog loginLog = new LoginLog();
        loginLog.setUsername(username);
        loginLog.setLoginTime(LocalDateTime.now());
        loginLog.setLoginIp(loginIp);
        loginLog.setLoginStatus(0);
        loginLog.setFailureReason(failureReason);
        loginLog.setCreatedAt(LocalDateTime.now());
        
        loginLogMapper.insert(loginLog);
        
        logger.info("Login failure recorded: username={}, ip={}, reason={}", 
            username, loginIp, failureReason);
    }

    @Override
    public void recordLogout(String sessionId) {
        QueryWrapper<LoginLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("session_id", sessionId)
                   .eq("login_status", 1)
                   .orderByDesc("login_time")
                   .last("LIMIT 1");
        
        LoginLog loginLog = loginLogMapper.selectOne(queryWrapper);
        
        if (loginLog != null) {
            loginLog.setLogoutTime(LocalDateTime.now());
            loginLogMapper.updateById(loginLog);
            
            logger.info("Logout recorded: sessionId={}, userId={}", 
                sessionId, loginLog.getUserId());
        }
    }

    @Override
    public IPage<LoginLog> getLoginLogs(LoginLogQueryRequest request) {
        Page<LoginLog> page = new Page<>(request.getPageNum(), request.getPageSize());
        
        QueryWrapper<LoginLog> queryWrapper = new QueryWrapper<>();
        
        if (request.getUserId() != null) {
            queryWrapper.eq("user_id", request.getUserId());
        }
        
        if (request.getUsername() != null && !request.getUsername().isEmpty()) {
            queryWrapper.like("username", request.getUsername());
        }
        
        if (request.getLoginStatus() != null) {
            queryWrapper.eq("login_status", request.getLoginStatus());
        }
        
        if (request.getStartTime() != null) {
            queryWrapper.ge("login_time", request.getStartTime());
        }
        
        if (request.getEndTime() != null) {
            queryWrapper.le("login_time", request.getEndTime());
        }
        
        queryWrapper.orderByDesc("login_time");
        
        IPage<LoginLog> result = loginLogMapper.selectPage(page, queryWrapper);
        
        logger.info("Retrieved {} login logs for request: {}", 
            result.getRecords().size(), request);
        
        return result;
    }

    @Override
    public LoginLog getLoginLogById(Long id) {
        return loginLogMapper.selectById(id);
    }

    @Override
    public void save(LoginLog loginLog) {
        loginLogMapper.insert(loginLog);
    }
}