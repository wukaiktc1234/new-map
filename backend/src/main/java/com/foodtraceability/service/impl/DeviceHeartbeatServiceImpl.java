package com.foodtraceability.service.impl;

import com.foodtraceability.entity.DeviceStatus;
import com.foodtraceability.entity.HardwareConfig;
import com.foodtraceability.mapper.HardwareConfigMapper;
import com.foodtraceability.service.device.DeviceAlertService;
import com.foodtraceability.service.DeviceConnectionService;
import com.foodtraceability.service.DeviceHeartbeatService;
import com.foodtraceability.service.DeviceStatusService;
import com.foodtraceability.util.DeviceConnectionPool;
import com.foodtraceability.util.DeviceConnectionPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 设备心跳检测服务实现类
 * 用于定时检查设备连接状态，及时发现设备断开连接的情况
 */
@Service
public class DeviceHeartbeatServiceImpl implements DeviceHeartbeatService {
    
    private static final Logger log = LoggerFactory.getLogger(DeviceHeartbeatServiceImpl.class);
    
    // 心跳检测间隔时间（毫秒），默认30秒
    private static final long DEFAULT_HEARTBEAT_INTERVAL = 30 * 1000;
    
    // 心跳检测超时时间（毫秒），默认5秒
    private static final long DEFAULT_HEARTBEAT_TIMEOUT = 5 * 1000;
    
    // 心跳检测线程池
    private final ScheduledExecutorService heartbeatExecutor;
    
    // 设备配置Mapper
    private final HardwareConfigMapper hardwareConfigMapper;

    // 设备连接服务
    private final DeviceConnectionService deviceConnectionService;

    // 设备状态服务
    private final DeviceStatusService deviceStatusService;

    // 设备告警服务
    private final DeviceAlertService deviceAlertService;

    // 心跳检测列表，key为设备标识（设备类型_设备ID或IP_端口）
    private final Map<String, HardwareConfig> heartbeatDeviceMap;

    // 心跳检测是否正在运行
    private volatile boolean heartbeatRunning;

    // 心跳检测间隔时间
    private final long heartbeatInterval;

    // 心跳检测超时时间
    private final long heartbeatTimeout;

    /**
     * 构造函数注入（禁止 @Autowired 字段注入）
     * @param hardwareConfigMapper 设备配置Mapper
     * @param deviceConnectionService 设备连接服务
     * @param deviceStatusService 设备状态服务
     * @param deviceAlertService 设备告警服务
     */
    public DeviceHeartbeatServiceImpl(HardwareConfigMapper hardwareConfigMapper, DeviceConnectionService deviceConnectionService, DeviceStatusService deviceStatusService, DeviceAlertService deviceAlertService) {
        this.hardwareConfigMapper = hardwareConfigMapper;
        this.deviceConnectionService = deviceConnectionService;
        this.deviceStatusService = deviceStatusService;
        this.deviceAlertService = deviceAlertService;
        this.heartbeatInterval = DEFAULT_HEARTBEAT_INTERVAL;
        this.heartbeatTimeout = DEFAULT_HEARTBEAT_TIMEOUT;
        this.heartbeatDeviceMap = new ConcurrentHashMap<>();
        this.heartbeatRunning = false;

        // 创建心跳检测线程池
        this.heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "device-heartbeat-detector");
            thread.setDaemon(true);
            return thread;
        });

        log.info("设备心跳检测服务初始化完成，心跳间隔: {}ms, 超时时间: {}ms", heartbeatInterval, heartbeatTimeout);
    }
    
    /**
     * 生成设备标识
     * @param config 设备配置
     * @return 设备标识
     */
    private String generateDeviceKey(HardwareConfig config) {
        return String.format("%s_%s_%s", 
                config.getDeviceType(), 
                config.getIpAddress(), 
                config.getPort());
    }
    
    @Override
    public void startHeartbeatDetection() {
        if (heartbeatRunning) {
            log.warn("设备心跳检测已经在运行中");
            return;
        }
        
        // 加载所有设备配置到心跳检测列表
        loadAllDevicesToHeartbeatList();
        
        // 启动定时心跳检测任务
        heartbeatExecutor.scheduleAtFixedRate(
                this::checkAllDevicesHeartbeat,
                0, heartbeatInterval, TimeUnit.MILLISECONDS
        );
        
        heartbeatRunning = true;
        log.info("设备心跳检测已启动，心跳间隔: {}ms", heartbeatInterval);
    }
    
    @Override
    public void stopHeartbeatDetection() {
        if (!heartbeatRunning) {
            log.warn("设备心跳检测已经停止");
            return;
        }
        
        // 停止心跳检测任务
        heartbeatExecutor.shutdown();
        try {
            if (!heartbeatExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                heartbeatExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            heartbeatExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        heartbeatRunning = false;
        log.info("设备心跳检测已停止");
    }
    
    @Override
    public boolean checkDeviceHeartbeat(HardwareConfig config) {
        try {
            log.debug("检查设备心跳: 类型={}, 名称={}, IP={}, 端口={}", 
                    config.getDeviceType(), config.getDeviceName(), config.getIpAddress(), config.getPort());
            
            // 使用设备连接池检查设备状态
            DeviceConnectionPool pool = DeviceConnectionPoolManager.getInstance().getConnectionPool(config);
            DeviceConnectionPool.DeviceConnection connection = pool.getConnection(heartbeatTimeout);
            if (connection == null) {
                log.warn("设备心跳检测失败: 类型={}, 名称={}, IP={}, 端口={}, 错误: 无法获取连接", 
                        config.getDeviceType(), config.getDeviceName(), config.getIpAddress(), config.getPort());
                return false;
            }
            
            try {
                // 检查连接是否有效
                boolean isValid = connection.isValid();
                if (isValid) {
                    // 更新设备最后使用时间
                    connection.updateLastUsedTime();
                    log.debug("设备心跳正常: 类型={}, 名称={}, IP={}, 端口={}", 
                            config.getDeviceType(), config.getDeviceName(), config.getIpAddress(), config.getPort());
                } else {
                    log.warn("设备心跳检测失败: 类型={}, 名称={}, IP={}, 端口={}, 错误: 连接无效", 
                            config.getDeviceType(), config.getDeviceName(), config.getIpAddress(), config.getPort());
                }
                return isValid;
            } finally {
                // 归还连接到连接池
                pool.returnConnection(connection);
            }
        } catch (Exception e) {
            log.error("设备心跳检测异常: 类型={}, 名称={}, IP={}, 端口={}, 错误: {}", 
                    config.getDeviceType(), config.getDeviceName(), config.getIpAddress(), config.getPort(), e.getMessage());
            return false;
        }
    }
    
    @Override
    public void checkAllDevicesHeartbeat() {
        log.debug("开始检查所有设备心跳，设备数量: {}", heartbeatDeviceMap.size());
        
        // 获取当前心跳检测列表中的所有设备
        Set<Map.Entry<String, HardwareConfig>> entries = heartbeatDeviceMap.entrySet();
        
        for (Map.Entry<String, HardwareConfig> entry : entries) {
            HardwareConfig config = entry.getValue();
            try {
                // 检查设备心跳
                boolean isOnline = checkDeviceHeartbeat(config);
                
                // 获取设备当前状态
                DeviceStatus currentStatus = deviceStatusService.getDeviceDetailedStatus(config.getDeviceType());
                
                // 如果设备状态发生变化，更新设备状态并触发告警
                if (currentStatus.isOnline() != isOnline) {
                    log.info("设备状态变化: 类型={}, 名称={}, IP={}, 端口={}, 旧状态={}, 新状态={}", 
                            config.getDeviceType(), config.getDeviceName(), config.getIpAddress(), config.getPort(), 
                            currentStatus.isOnline() ? "在线" : "离线", isOnline ? "在线" : "离线");
                    
                    // 更新设备状态
                    config.setStatus(isOnline ? 1 : 0);
                    hardwareConfigMapper.updateById(config);
                    
                    // 清除设备状态缓存，强制重新检测
                    deviceStatusService.clearDeviceStatusCache(config.getDeviceType());
                    
                    // 触发设备状态告警
                    deviceAlertService.checkDeviceStatusAndAlert(currentStatus);
                }
            } catch (Exception e) {
                log.error("检查设备心跳异常: 类型={}, 名称={}, IP={}, 端口={}, 错误: {}", 
                        config.getDeviceType(), config.getDeviceName(), config.getIpAddress(), config.getPort(), e.getMessage());
            }
        }
        
        log.debug("所有设备心跳检查完成，设备数量: {}", heartbeatDeviceMap.size());
    }
    
    @Override
    public void addDeviceToHeartbeatList(HardwareConfig config) {
        String deviceKey = generateDeviceKey(config);
        heartbeatDeviceMap.put(deviceKey, config);
        log.info("添加设备到心跳检测列表: 类型={}, 名称={}, IP={}, 端口={}", 
                config.getDeviceType(), config.getDeviceName(), config.getIpAddress(), config.getPort());
    }
    
    @Override
    public void removeDeviceFromHeartbeatList(HardwareConfig config) {
        String deviceKey = generateDeviceKey(config);
        heartbeatDeviceMap.remove(deviceKey);
        log.info("从心跳检测列表移除设备: 类型={}, 名称={}, IP={}, 端口={}", 
                config.getDeviceType(), config.getDeviceName(), config.getIpAddress(), config.getPort());
    }
    
    @Override
    public boolean isHeartbeatRunning() {
        return heartbeatRunning;
    }
    
    /**
     * 加载所有设备配置到心跳检测列表
     */
    private void loadAllDevicesToHeartbeatList() {
        try {
            // 查询所有设备配置
            List<HardwareConfig> configs = hardwareConfigMapper.selectList(null);
            if (configs == null || configs.isEmpty()) {
                log.info("没有找到设备配置");
                return;
            }
            
            // 添加到心跳检测列表
            for (HardwareConfig config : configs) {
                addDeviceToHeartbeatList(config);
            }
            
            log.info("加载设备配置到心跳检测列表完成，设备数量: {}", configs.size());
        } catch (Exception e) {
            log.error("加载设备配置到心跳检测列表失败: {}", e.getMessage());
        }
    }
}