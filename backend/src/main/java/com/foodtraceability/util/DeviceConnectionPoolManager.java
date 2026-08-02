package com.foodtraceability.util;

import com.foodtraceability.entity.HardwareConfig;
import com.foodtraceability.service.DeviceIdentificationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 设备连接池管理器
 * 用于管理多个设备的连接池实例，实现连接池的创建、获取、关闭等功能
 */
@Component
public class DeviceConnectionPoolManager {

    private static final Logger log = LoggerFactory.getLogger(DeviceConnectionPoolManager.class);

    private static DeviceConnectionPoolManager instance;

    // 设备连接池映射，key为设备标识（设备类型_设备ID或IP_端口）
    private final Map<String, DeviceConnectionPool> connectionPools;

    // 定时清理线程池，用于定期清理过期连接
    private final ScheduledExecutorService cleanupExecutor;

    // 设备标识管理器
    private final DeviceIdentificationManager deviceIdentificationManager;

    /**
     * 构造函数注入（禁止 @Autowired 字段注入）
     * @param deviceIdentificationManager 设备标识管理器
     */
    public DeviceConnectionPoolManager(DeviceIdentificationManager deviceIdentificationManager) {
        this.deviceIdentificationManager = deviceIdentificationManager;
        this.connectionPools = new ConcurrentHashMap<>();
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "device-connection-pool-cleanup");
            thread.setDaemon(true);
            return thread;
        });

        log.info("设备连接池管理器初始化完成");
    }

    /**
     * 获取单例实例
     * @return 单例实例
     */
    public static DeviceConnectionPoolManager getInstance() {
        if (instance == null) {
            // 注意：此处创建的实例无设备标识管理器依赖，仅为兼容旧代码
            // 推荐通过Spring注入获取Bean实例
            throw new IllegalStateException("DeviceConnectionPoolManager 未初始化，请通过 Spring 注入获取实例");
        }
        return instance;
    }

    /**
     * 初始化方法，Spring自动调用
     */
    @PostConstruct
    private void init() {
        // 设置单例引用，供静态 getInstance() 使用
        instance = this;
        // 启动定时清理任务，每5分钟清理一次过期连接
        this.cleanupExecutor.scheduleAtFixedRate(
                this::cleanupExpiredConnections,
                5, 5, TimeUnit.MINUTES
        );

        // 注册JVM关闭钩子，确保关闭连接池
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        log.info("设备连接池管理器初始化完成");
    }
    
    /**
     * 生成设备标识
     * @param config 设备配置
     * @return 设备标识
     */
    private String generateDeviceKey(HardwareConfig config) {
        // 使用DeviceIdentificationManager生成设备唯一标识
        return deviceIdentificationManager.generateDeviceId(config);
    }
    
    /**
     * 获取设备连接池
     * 如果连接池不存在，则创建新的连接池
     * @param config 设备配置
     * @return 设备连接池
     */
    public DeviceConnectionPool getConnectionPool(HardwareConfig config) {
        String deviceKey = generateDeviceKey(config);
        return connectionPools.computeIfAbsent(deviceKey, k -> {
            log.info("创建设备连接池: 设备类型={}, IP={}, 端口={}", 
                    config.getDeviceType(), config.getIpAddress(), config.getPort());
            return new DeviceConnectionPool(config);
        });
    }
    
    /**
     * 获取设备连接池
     * @param deviceType 设备类型
     * @param ipAddress 设备IP地址
     * @param port 设备端口
     * @return 设备连接池，如果不存在则返回null
     */
    public DeviceConnectionPool getConnectionPool(String deviceType, String ipAddress, String port) {
        // 生成临时设备配置用于获取设备标识
        HardwareConfig tempConfig = new HardwareConfig();
        tempConfig.setDeviceType(deviceType);
        tempConfig.setIpAddress(ipAddress);
        tempConfig.setPort(port);
        
        String deviceKey = deviceIdentificationManager.generateDeviceId(tempConfig);
        return connectionPools.get(deviceKey);
    }
    
    /**
     * 根据设备唯一标识获取设备连接池
     * @param deviceId 设备唯一标识
     * @return 设备连接池，如果不存在则返回null
     */
    public DeviceConnectionPool getConnectionPoolById(String deviceId) {
        return connectionPools.get(deviceId);
    }
    
    /**
     * 关闭设备连接池
     * @param config 设备配置
     */
    public void closeConnectionPool(HardwareConfig config) {
        String deviceKey = generateDeviceKey(config);
        DeviceConnectionPool pool = connectionPools.remove(deviceKey);
        if (pool != null) {
            pool.close();
            log.info("关闭设备连接池: 设备类型={}, IP={}, 端口={}", 
                    config.getDeviceType(), config.getIpAddress(), config.getPort());
        }
    }
    
    /**
     * 关闭所有设备连接池
     */
    public void closeAllConnectionPools() {
        log.info("开始关闭所有设备连接池");
        for (Map.Entry<String, DeviceConnectionPool> entry : connectionPools.entrySet()) {
            entry.getValue().close();
            log.info("关闭设备连接池: {}", entry.getKey());
        }
        connectionPools.clear();
        log.info("所有设备连接池已关闭");
    }
    
    /**
     * 清理过期连接
     */
    private void cleanupExpiredConnections() {
        log.info("开始清理过期设备连接");
        
        int totalPools = connectionPools.size();
        int poolsCleaned = 0;
        
        for (Map.Entry<String, DeviceConnectionPool> entry : connectionPools.entrySet()) {
            String deviceKey = entry.getKey();
            DeviceConnectionPool pool = entry.getValue();
            
            try {
                // 这里的清理逻辑依赖于连接池内部的实现，当连接被获取时会自动检查是否过期
                // 打印连接池状态，便于监控
                DeviceConnectionPool.ConnectionPoolStatus status = pool.getStatus();
                log.debug("设备连接池状态: 设备={}, {}", deviceKey, status);
                poolsCleaned++;
            } catch (Exception e) {
                log.error("清理设备连接池失败: {}, 错误: {}", deviceKey, e.getMessage());
            }
        }
        
        log.info("设备连接过期清理完成，共检查连接池: {}个，清理: {}个", totalPools, poolsCleaned);
    }
    
    /**
     * 关闭连接池管理器，释放资源
     */
    public void shutdown() {
        log.info("关闭设备连接池管理器");
        
        // 关闭定时清理线程池
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // 关闭所有连接池
        closeAllConnectionPools();
        
        log.info("设备连接池管理器已关闭");
    }
    
    /**
     * 获取设备连接池数量
     * @return 设备连接池数量
     */
    public int getPoolCount() {
        return connectionPools.size();
    }
    
    /**
     * 获取所有设备连接池状态
     * @return 设备连接池状态映射
     */
    public Map<String, DeviceConnectionPool.ConnectionPoolStatus> getPoolStatuses() {
        Map<String, DeviceConnectionPool.ConnectionPoolStatus> statuses = new ConcurrentHashMap<>();
        for (Map.Entry<String, DeviceConnectionPool> entry : connectionPools.entrySet()) {
            statuses.put(entry.getKey(), entry.getValue().getStatus());
        }
        return statuses;
    }
}