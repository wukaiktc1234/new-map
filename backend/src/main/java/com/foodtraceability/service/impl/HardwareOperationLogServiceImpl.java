package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.HardwareOperationLog;
import com.foodtraceability.mapper.HardwareOperationLogMapper;
import com.foodtraceability.service.HardwareOperationLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 硬件设备操作日志Service实现类
 */
@Service
public class HardwareOperationLogServiceImpl extends ServiceImpl<HardwareOperationLogMapper, HardwareOperationLog> implements HardwareOperationLogService {

    private static final Logger logger = LoggerFactory.getLogger(HardwareOperationLogServiceImpl.class);


    public HardwareOperationLogServiceImpl(HardwareOperationLogMapper hardwareOperationLogMapper) {
        this.hardwareOperationLogMapper = hardwareOperationLogMapper;
    }

    private final HardwareOperationLogMapper hardwareOperationLogMapper;

    @Override
    public void recordLog(HardwareOperationLog log) {
        try {
            hardwareOperationLogMapper.insert(log);
        } catch (Exception e) {
            // 记录日志失败时，避免影响主业务流程
            logger.error("记录硬件操作日志失败: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public void recordLog(HardwareOperationLog log, HttpServletRequest request) {
        try {
            // 记录客户端IP地址
            if (request != null) {
                log.setIpAddress(com.foodtraceability.utils.WebUtils.getClientIpAddress(request));
                log.setUserAgent(com.foodtraceability.utils.WebUtils.getUserAgent(request));
            }
            hardwareOperationLogMapper.insert(log);
        } catch (Exception e) {
            // 记录日志失败时，避免影响主业务流程
            logger.error("记录硬件操作日志失败: {}", e.getMessage(), e);
        }
    }
}