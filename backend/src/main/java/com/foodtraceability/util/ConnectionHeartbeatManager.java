package com.foodtraceability.util;

import com.foodtraceability.driver.DeviceDriver;
import com.foodtraceability.entity.HardwareConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 设备连接心跳检测管理器
 * 负责管理所有设备的心跳检测，确保设备连接的可靠性
 */
@Component
public class ConnectionHeartbeatManager {
    
    private static final Logger log = LoggerFactory.getLogger(ConnectionHeartbeatManager.class);
    
    // 心跳检测线程池
    private final ScheduledExecutorService heartbeatExecutor;
    
    // 心跳检测间隔（默认30秒）
    private static final long DEFAULT_HEARTBEAT_INTERVAL = 30;
    
    // 心跳超时次数阈值（默认3次）
    private static final int DEFAULT_HEARTBEAT_TIMEOUT_THRESHOLD = 3;
    
    // 设备心跳状态映射
    private final Map<String, DeviceHeartbeatInfo> deviceHeartbeatMap;
    
    // 心跳检测状态
    private final AtomicBoolean isRunning;
    
    // 设备驱动管理器
    private final com.foodtraceability.driver.DeviceDriverManager deviceDriverManager;
    
    /**
     * 设备心跳信息类
     */
    private static class DeviceHeartbeatInfo {
        private final DeviceDriver driver;
        private final String deviceId;
        private long lastHeartbeatTime;
        private int consecutiveTimeoutCount;
        private int reconnectAttempts;
        private ScheduledFuture<?> heartbeatTask;
        private ScheduledFuture<?> reconnectTask;
        private final DeviceReconnectStrategy reconnectStrategy;
        
        public DeviceHeartbeatInfo(DeviceDriver driver, String deviceId) {
            this.driver = driver;
            this.deviceId = deviceId;
            this.lastHeartbeatTime = System.currentTimeMillis();
            this.consecutiveTimeoutCount = 0;
            this.reconnectAttempts = 0;
            // 使用默认的指数退避重连策略
            this.reconnectStrategy = DeviceReconnectStrategy.ReconnectStrategyFactory.getDefaultStrategy();
        }
        
        public DeviceDriver getDriver() {
            return driver;
        }
        
        public String getDeviceId() {
            return deviceId;
        }
        
        public long getLastHeartbeatTime() {
            return lastHeartbeatTime;
        }
        
        public void updateLastHeartbeatTime() {
            this.lastHeartbeatTime = System.currentTimeMillis();
            this.consecutiveTimeoutCount = 0;
            this.reconnectAttempts = 0;
            this.reconnectStrategy.reset();
            // 取消正在进行的重连任务
            if (reconnectTask != null) {
                reconnectTask.cancel(true);
                reconnectTask = null;
            }
        }
        
        public int getConsecutiveTimeoutCount() {
            return consecutiveTimeoutCount;
        }
        
        public void incrementTimeoutCount() {
            this.consecutiveTimeoutCount++;
        }
        
        public ScheduledFuture<?> getHeartbeatTask() {
            return heartbeatTask;
        }
        
        public void setHeartbeatTask(ScheduledFuture<?> heartbeatTask) {
            this.heartbeatTask = heartbeatTask;
        }
        
        public ScheduledFuture<?> getReconnectTask() {
            return reconnectTask;
        }
        
        public void setReconnectTask(ScheduledFuture<?> reconnectTask) {
            this.reconnectTask = reconnectTask;
        }
        
        public int getReconnectAttempts() {
            return reconnectAttempts;
        }
        
        public void incrementReconnectAttempts() {
            this.reconnectAttempts++;
        }
        
        public DeviceReconnectStrategy getReconnectStrategy() {
            return reconnectStrategy;
        }
    }
    
    /**
     * 构造函数
     * @param deviceDriverManager 设备驱动管理器
     */
    public ConnectionHeartbeatManager(@Lazy com.foodtraceability.driver.DeviceDriverManager deviceDriverManager) {
        this.deviceDriverManager = deviceDriverManager;
        this.heartbeatExecutor = Executors.newScheduledThreadPool(10);
        this.deviceHeartbeatMap = new ConcurrentHashMap<>();
        this.isRunning = new AtomicBoolean(false);
        log.info("连接心跳检测管理器初始化完成");
    }
    
    /**
     * 启动心跳检测服务
     */
    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            log.info("启动设备连接心跳检测服务");
        }
    }
    
    /**
     * 停止心跳检测服务
     */
    public void stop() {
        if (isRunning.compareAndSet(true, false)) {
            log.info("停止设备连接心跳检测服务");
            
            // 取消所有心跳任务
            for (DeviceHeartbeatInfo heartbeatInfo : deviceHeartbeatMap.values()) {
                if (heartbeatInfo.getHeartbeatTask() != null) {
                    heartbeatInfo.getHeartbeatTask().cancel(true);
                }
            }
            
            // 清空设备心跳映射
            deviceHeartbeatMap.clear();
        }
    }
    
    /**
     * 为设备添加心跳检测
     * @param deviceId 设备ID
     * @param driver 设备驱动
     */
    public void addHeartbeatForDevice(String deviceId, DeviceDriver driver) {
        if (!isRunning.get()) {
            log.warn("心跳检测服务未启动，无法添加设备心跳检测");
            return;
        }
        
        // 检查设备是否已存在心跳检测
        if (deviceHeartbeatMap.containsKey(deviceId)) {
            log.warn("设备 {} 已存在心跳检测，无需重复添加", deviceId);
            return;
        }
        
        log.info("为设备 {} 添加心跳检测", deviceId);
        
        // 创建设备心跳信息
        DeviceHeartbeatInfo heartbeatInfo = new DeviceHeartbeatInfo(driver, deviceId);
        
        // 启动心跳检测任务
        ScheduledFuture<?> heartbeatTask = heartbeatExecutor.scheduleAtFixedRate(
            () -> checkDeviceHeartbeat(heartbeatInfo),
            DEFAULT_HEARTBEAT_INTERVAL,
            DEFAULT_HEARTBEAT_INTERVAL,
            TimeUnit.SECONDS
        );
        
        heartbeatInfo.setHeartbeatTask(heartbeatTask);
        deviceHeartbeatMap.put(deviceId, heartbeatInfo);
    }
    
    /**
     * 移除设备的心跳检测
     * @param deviceId 设备ID
     */
    public void removeHeartbeatForDevice(String deviceId) {
        DeviceHeartbeatInfo heartbeatInfo = deviceHeartbeatMap.remove(deviceId);
        if (heartbeatInfo != null) {
            log.info("移除设备 {} 的心跳检测", deviceId);
            if (heartbeatInfo.getHeartbeatTask() != null) {
                heartbeatInfo.getHeartbeatTask().cancel(true);
            }
        }
    }
    
    /**
     * 检查设备心跳
     * @param heartbeatInfo 设备心跳信息
     */
    private void checkDeviceHeartbeat(DeviceHeartbeatInfo heartbeatInfo) {
        String deviceId = heartbeatInfo.getDeviceId();
        DeviceDriver driver = heartbeatInfo.getDriver();
        
        try {
            // 检查设备连接状态
            if (driver.isConnected()) {
                // 设备已连接，执行心跳检测
                boolean heartbeatSuccess = executeHeartbeat(driver);
                
                if (heartbeatSuccess) {
                    // 心跳成功，更新心跳时间
                    heartbeatInfo.updateLastHeartbeatTime();
                    log.debug("设备 {} 心跳检测成功", deviceId);
                } else {
                    // 心跳失败，增加超时计数
                    heartbeatInfo.incrementTimeoutCount();
                    log.warn("设备 {} 心跳检测失败，连续超时次数: {}", 
                            deviceId, heartbeatInfo.getConsecutiveTimeoutCount());
                    
                    // 检查是否达到超时阈值
                    if (heartbeatInfo.getConsecutiveTimeoutCount() >= DEFAULT_HEARTBEAT_TIMEOUT_THRESHOLD) {
                        log.error("设备 {} 心跳超时次数达到阈值，触发重连机制", deviceId);
                        handleHeartbeatTimeout(heartbeatInfo);
                    }
                }
            } else {
                // 设备未连接，增加超时计数
                heartbeatInfo.incrementTimeoutCount();
                log.warn("设备 {} 未连接，连续超时次数: {}", 
                        deviceId, heartbeatInfo.getConsecutiveTimeoutCount());
                
                // 检查是否达到超时阈值
                if (heartbeatInfo.getConsecutiveTimeoutCount() >= DEFAULT_HEARTBEAT_TIMEOUT_THRESHOLD) {
                    log.error("设备 {} 未连接超时次数达到阈值，触发重连机制", deviceId);
                    handleHeartbeatTimeout(heartbeatInfo);
                }
            }
        } catch (Exception e) {
            log.error("设备 {} 心跳检测异常: {}", deviceId, e.getMessage(), e);
            heartbeatInfo.incrementTimeoutCount();
            
            // 检查是否达到超时阈值
            if (heartbeatInfo.getConsecutiveTimeoutCount() >= DEFAULT_HEARTBEAT_TIMEOUT_THRESHOLD) {
                log.error("设备 {} 心跳检测异常次数达到阈值，触发重连机制", deviceId);
                handleHeartbeatTimeout(heartbeatInfo);
            }
        }
    }
    
    /**
     * 执行设备心跳检测
     * @param driver 设备驱动
     * @return 心跳检测结果
     */
    private boolean executeHeartbeat(DeviceDriver driver) {
        try {
            // 执行设备心跳检测
            // 这里可以根据不同设备类型执行不同的心跳检测逻辑
            // 由于是示例，我们简单地检查连接状态
            return driver.isConnected();
        } catch (Exception e) {
            log.error("执行设备心跳检测异常: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 处理心跳超时
     * @param heartbeatInfo 设备心跳信息
     */
    private void handleHeartbeatTimeout(DeviceHeartbeatInfo heartbeatInfo) {
        String deviceId = heartbeatInfo.getDeviceId();
        
        // 检查是否已经有重连任务在执行
        if (heartbeatInfo.getReconnectTask() != null && !heartbeatInfo.getReconnectTask().isDone()) {
            log.debug("设备 {} 已有重连任务在执行，跳过本次重连请求", deviceId);
            return;
        }
        
        // 检查是否应该继续重连
        if (!heartbeatInfo.getReconnectStrategy().shouldContinueReconnecting(heartbeatInfo.getReconnectAttempts())) {
            log.error("设备 {} 重连次数达到上限，停止重连", deviceId);
            return;
        }
        
        // 获取下一次重连间隔
        long reconnectInterval = heartbeatInfo.getReconnectStrategy().getNextReconnectInterval(heartbeatInfo.getReconnectAttempts());
        log.info("设备 {} 将在 {} 毫秒后进行第 {} 次重连尝试", 
                deviceId, reconnectInterval, heartbeatInfo.getReconnectAttempts() + 1);
        
        // 提交重连任务
        ScheduledFuture<?> reconnectTask = heartbeatExecutor.schedule(
            () -> executeReconnect(heartbeatInfo),
            reconnectInterval,
            TimeUnit.MILLISECONDS
        );
        
        heartbeatInfo.setReconnectTask(reconnectTask);
    }
    
    /**
     * 执行设备重连
     * @param heartbeatInfo 设备心跳信息
     */
    private void executeReconnect(DeviceHeartbeatInfo heartbeatInfo) {
        String deviceId = heartbeatInfo.getDeviceId();
        DeviceDriver driver = heartbeatInfo.getDriver();
        
        try {
            log.info("执行设备 {} 的第 {} 次重连尝试", deviceId, heartbeatInfo.getReconnectAttempts() + 1);
            
            // 断开现有连接
            driver.disconnect();
            
            // 重新连接设备
            boolean reconnectSuccess = driver.connect();
            
            if (reconnectSuccess) {
                log.info("设备 {} 第 {} 次重连成功", deviceId, heartbeatInfo.getReconnectAttempts() + 1);
                // 重置心跳信息
                heartbeatInfo.updateLastHeartbeatTime();
            } else {
                log.error("设备 {} 第 {} 次重连失败", deviceId, heartbeatInfo.getReconnectAttempts() + 1);
                // 增加重连尝试次数
                heartbeatInfo.incrementReconnectAttempts();
                // 继续执行重连
                handleHeartbeatTimeout(heartbeatInfo);
            }
        } catch (Exception e) {
            log.error("设备 {} 第 {} 次重连异常: {}", 
                    deviceId, heartbeatInfo.getReconnectAttempts() + 1, e.getMessage(), e);
            // 增加重连尝试次数
            heartbeatInfo.incrementReconnectAttempts();
            // 继续执行重连
            handleHeartbeatTimeout(heartbeatInfo);
        }
    }
    
    /**
     * 获取设备心跳状态
     * @param deviceId 设备ID
     * @return 设备心跳状态
     */
    public Map<String, Object> getDeviceHeartbeatStatus(String deviceId) {
        DeviceHeartbeatInfo heartbeatInfo = deviceHeartbeatMap.get(deviceId);
        Map<String, Object> statusMap = new ConcurrentHashMap<>();
        
        if (heartbeatInfo != null) {
            statusMap.put("deviceId", deviceId);
            statusMap.put("lastHeartbeatTime", heartbeatInfo.getLastHeartbeatTime());
            statusMap.put("consecutiveTimeoutCount", heartbeatInfo.getConsecutiveTimeoutCount());
            statusMap.put("isConnected", heartbeatInfo.getDriver().isConnected());
        } else {
            statusMap.put("deviceId", deviceId);
            statusMap.put("error", "设备不存在或未启用心跳检测");
        }
        
        return statusMap;
    }
    
    /**
     * 获取所有设备心跳状态
     * @return 所有设备心跳状态映射
     */
    public Map<String, Map<String, Object>> getAllDeviceHeartbeatStatus() {
        Map<String, Map<String, Object>> allStatusMap = new ConcurrentHashMap<>();
        
        for (String deviceId : deviceHeartbeatMap.keySet()) {
            allStatusMap.put(deviceId, getDeviceHeartbeatStatus(deviceId));
        }
        
        return allStatusMap;
    }
    
    /**
     * 获取心跳检测运行状态
     * @return 心跳检测运行状态
     */
    public boolean isRunning() {
        return isRunning.get();
    }
    
    /**
     * 获取设备心跳检测数量
     * @return 设备心跳检测数量
     */
    public int getHeartbeatDeviceCount() {
        return deviceHeartbeatMap.size();
    }
}