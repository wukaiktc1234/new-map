package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.HardwareOperationLog;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 硬件设备操作日志Service接口
 */
public interface HardwareOperationLogService extends IService<HardwareOperationLog> {

    /**
     * 记录设备操作日志
     * @param log 日志实体
     */
    void recordLog(HardwareOperationLog log);
    
    /**
     * 记录设备操作日志，包含详细审计信息
     * @param log 日志实体
     * @param request HttpServletRequest对象，用于获取客户端IP和用户代理信息
     */
    void recordLog(HardwareOperationLog log, HttpServletRequest request);
}