package com.foodtraceability.config;

import com.foodtraceability.service.DeviceHeartbeatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 设备心跳检测配置类
 * 用于在应用启动时自动启动设备心跳检测服务
 * 注意：已禁用自动启动，避免向打印机发送HTTP请求导致打印HTTP请求头
 */
@Component
public class DeviceHeartbeatConfig implements ApplicationRunner {
    
    private static final Logger log = LoggerFactory.getLogger(DeviceHeartbeatConfig.class);
    
    // 设备心跳检测服务

    public DeviceHeartbeatConfig(DeviceHeartbeatService deviceHeartbeatService) {
        this.deviceHeartbeatService = deviceHeartbeatService;
    }

    private final DeviceHeartbeatService deviceHeartbeatService;
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            // 已禁用自动启动设备心跳检测服务
            // 如需启用，请取消下面代码的注释
            // deviceHeartbeatService.startHeartbeatDetection();
            log.info("设备心跳检测服务已禁用自动启动（避免向打印机发送HTTP请求导致打印HTTP请求头）");
        } catch (Exception e) {
            log.error("启动设备心跳检测服务失败: {}", e.getMessage(), e);
        }
    }
}