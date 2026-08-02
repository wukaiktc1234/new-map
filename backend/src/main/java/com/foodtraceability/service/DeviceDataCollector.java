package com.foodtraceability.service;

import com.foodtraceability.driver.DeviceDriver;
import com.foodtraceability.driver.DeviceDriverManager;
import com.foodtraceability.entity.DeviceData;
import com.foodtraceability.entity.HardwareConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 设备数据采集服务
 * 负责定期采集设备运行数据并存储到数据库中
 */
@Service
public class DeviceDataCollector {
    
    private static final Logger log = LoggerFactory.getLogger(DeviceDataCollector.class);
    
    // 数据采集线程池
    private final ScheduledExecutorService collectorExecutor;
    
    // 数据采集间隔（默认60秒）
    private static final long DEFAULT_COLLECT_INTERVAL = 60;
    
    // 数据采集状态
    private final AtomicBoolean isRunning;
    
    // 设备驱动管理器
    private final DeviceDriverManager deviceDriverManager;
    
    // 设备数据服务
    private final DeviceDataService deviceDataService;
    
    // 设备标识管理器
    private final DeviceIdentificationManager deviceIdentificationManager;
    
    /**
     * 构造函数
     */
    public DeviceDataCollector(DeviceDriverManager deviceDriverManager, 
                              DeviceDataService deviceDataService, 
                              DeviceIdentificationManager deviceIdentificationManager) {
        this.deviceDriverManager = deviceDriverManager;
        this.deviceDataService = deviceDataService;
        this.deviceIdentificationManager = deviceIdentificationManager;
        this.collectorExecutor = Executors.newScheduledThreadPool(5);
        this.isRunning = new AtomicBoolean(false);
        log.info("设备数据采集服务初始化完成");
    }
    
    /**
     * 启动数据采集服务
     */
    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            log.info("启动设备数据采集服务");
            
            // 启动定期采集任务
            collectorExecutor.scheduleAtFixedRate(
                this::collectAllDeviceData,
                0,
                DEFAULT_COLLECT_INTERVAL,
                TimeUnit.SECONDS
            );
        }
    }
    
    /**
     * 停止数据采集服务
     */
    public void stop() {
        if (isRunning.compareAndSet(true, false)) {
            log.info("停止设备数据采集服务");
            collectorExecutor.shutdown();
        }
    }
    
    /**
     * 采集所有设备数据
     */
    private void collectAllDeviceData() {
        if (!isRunning.get()) {
            return;
        }
        
        try {
            log.debug("开始采集所有设备数据");
            
            // 获取所有设备配置
            Map<String, HardwareConfig> deviceConfigMap = deviceIdentificationManager.getAllDeviceConfigs();
            
            // 遍历所有设备，采集数据
            for (Map.Entry<String, HardwareConfig> entry : deviceConfigMap.entrySet()) {
                String deviceId = entry.getKey();
                HardwareConfig config = entry.getValue();
                
                // 采集单个设备数据
                collectDeviceData(deviceId, config);
            }
            
            log.debug("所有设备数据采集完成");
        } catch (Exception e) {
            log.error("采集设备数据异常: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 采集单个设备数据
     * @param deviceId 设备ID
     * @param config 设备配置
     */
    private void collectDeviceData(String deviceId, HardwareConfig config) {
        try {
            log.debug("开始采集设备 {} 的数据", deviceId);
            
            // 获取设备驱动
            DeviceDriver driver = deviceDriverManager.getDriverByDeviceId(deviceId);
            if (driver == null) {
                log.warn("设备 {} 未找到驱动，跳过数据采集", deviceId);
                return;
            }
            
            // 创建设备数据对象
            DeviceData deviceData = new DeviceData();
            deviceData.setDeviceId(deviceId);
            deviceData.setDeviceType(config.getDeviceType());
            deviceData.setDeviceName(config.getDeviceName());
            deviceData.setStoreId(config.getStoreId());
            deviceData.setCollectTime(LocalDateTime.now());
            deviceData.setCreatedAt(LocalDateTime.now());
            deviceData.setUpdatedAt(LocalDateTime.now());
            
            // 采集连接状态
            deviceData.setConnectionStatus(driver.isConnected() ? 1 : 0);
            
            // 采集设备状态详情
            try {
                String statusDetails = driver.getDeviceStatus().getDetails();
                deviceData.setStatusDetails(statusDetails);
            } catch (Exception e) {
                log.error("获取设备 {} 状态详情失败: {}", deviceId, e.getMessage(), e);
                deviceData.setStatusDetails("获取状态详情失败");
            }
            
            // 这里可以添加更多数据采集逻辑，如响应时间、错误次数等
            // 由于是示例，我们只采集基础数据
            
            // 保存设备数据
            deviceDataService.save(deviceData);
            
            log.debug("设备 {} 数据采集完成", deviceId);
        } catch (Exception e) {
            log.error("采集设备 {} 数据异常: {}", deviceId, e.getMessage(), e);
        }
    }
    
    /**
     * 手动触发设备数据采集
     * @param deviceId 设备ID
     */
    public void triggerDeviceDataCollection(String deviceId) {
        if (!isRunning.get()) {
            log.warn("设备数据采集服务未启动，无法手动触发采集");
            return;
        }
        
        try {
            // 获取设备配置
            HardwareConfig config = deviceIdentificationManager.getDeviceConfig(deviceId);
            if (config == null) {
                log.warn("设备 {} 不存在，无法触发数据采集", deviceId);
                return;
            }
            
            // 采集设备数据
            collectDeviceData(deviceId, config);
            
            log.info("手动触发设备 {} 数据采集完成", deviceId);
        } catch (Exception e) {
            log.error("手动触发设备 {} 数据采集异常: {}", deviceId, e.getMessage(), e);
        }
    }
    
    /**
     * 获取数据采集服务运行状态
     * @return 运行状态
     */
    public boolean isRunning() {
        return isRunning.get();
    }
}