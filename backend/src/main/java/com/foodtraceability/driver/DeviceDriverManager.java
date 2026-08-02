package com.foodtraceability.driver;

import com.foodtraceability.entity.HardwareConfig;
import com.foodtraceability.util.ConnectionHeartbeatManager;
import com.foodtraceability.util.DynamicDriverLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 设备驱动管理器
 * 负责设备驱动的加载、注册和管理
 */
@Component
public class DeviceDriverManager {
    
    private static final Logger log = LoggerFactory.getLogger(DeviceDriverManager.class);
    
    // 驱动注册表，key为设备类型，value为支持该设备类型的驱动列表
    private final Map<String, List<DeviceDriver>> driverRegistry;
    
    // 设备驱动映射，key为设备唯一标识，value为该设备使用的驱动实例
    private final Map<String, DeviceDriver> deviceDriverMap;
    
    // Spring应用上下文
    private final ApplicationContext applicationContext;
    
    // 设备标识管理器
    private final com.foodtraceability.service.DeviceIdentificationManager deviceIdentificationManager;

    // 连接心跳检测管理器
    private final ConnectionHeartbeatManager heartbeatManager;

    // 动态驱动加载器
    private final DynamicDriverLoader dynamicDriverLoader;

    // 动态加载的驱动列表
    private final List<DeviceDriver> dynamicallyLoadedDrivers = new ArrayList<>();

    /**
     * 构造函数
     */
    public DeviceDriverManager(ApplicationContext applicationContext, com.foodtraceability.service.DeviceIdentificationManager deviceIdentificationManager, ConnectionHeartbeatManager heartbeatManager, DynamicDriverLoader dynamicDriverLoader) {
        this.driverRegistry = new ConcurrentHashMap<>();
        this.deviceDriverMap = new ConcurrentHashMap<>();
        this.applicationContext = applicationContext;
        this.deviceIdentificationManager = deviceIdentificationManager;
        this.heartbeatManager = heartbeatManager;
        this.dynamicDriverLoader = dynamicDriverLoader;
        log.info("设备驱动管理器初始化完成");
    }
    
    /**
     * 初始化驱动管理器
     * 从Spring容器中自动发现所有DeviceDriver实现类并注册
     */
    @PostConstruct
    private void init() {
        // 从Spring容器中获取所有DeviceDriver实现类
        Map<String, DeviceDriver> driverBeans = applicationContext.getBeansOfType(DeviceDriver.class);
        
        // 注册所有驱动
        for (DeviceDriver driver : driverBeans.values()) {
            registerDriver(driver);
        }
        
        // 加载外部驱动
        loadExternalDrivers();
        
        // 启动心跳检测服务
        heartbeatManager.start();
        
        log.info("设备驱动初始化完成，共注册 {} 个驱动", driverRegistry.values().stream().mapToInt(List::size).sum());
    }
    
    /**
     * 加载外部驱动
     */
    private void loadExternalDrivers() {
        try {
            List<DeviceDriver> externalDrivers = dynamicDriverLoader.loadDriversFromDirectory();
            for (DeviceDriver driver : externalDrivers) {
                registerDriver(driver);
                dynamicallyLoadedDrivers.add(driver);
            }
        } catch (Exception e) {
            log.error("加载外部驱动失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 注册设备驱动
     * @param driver 设备驱动实例
     */
    public void registerDriver(DeviceDriver driver) {
        if (driver == null) {
            throw new IllegalArgumentException("设备驱动不能为空");
        }
        
        String supportedDeviceType = driver.getSupportedDeviceType();
        driverRegistry.computeIfAbsent(supportedDeviceType, k -> new ArrayList<>()).add(driver);
        log.info("注册设备驱动: {} 版本: {} 支持设备类型: {}", 
                driver.getDriverName(), driver.getDriverVersion(), supportedDeviceType);
    }
    
    /**
     * 注销设备驱动
     * @param driver 设备驱动实例
     */
    public void unregisterDriver(DeviceDriver driver) {
        if (driver == null) {
            return;
        }
        
        String supportedDeviceType = driver.getSupportedDeviceType();
        List<DeviceDriver> drivers = driverRegistry.get(supportedDeviceType);
        if (drivers != null) {
            drivers.remove(driver);
            log.info("注销设备驱动: {} 版本: {} 支持设备类型: {}", 
                    driver.getDriverName(), driver.getDriverVersion(), supportedDeviceType);
        }
    }
    
    /**
     * 为设备获取合适的驱动
     * @param config 设备配置
     * @return 合适的驱动实例
     */
    public DeviceDriver getDriverForDevice(HardwareConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("设备配置不能为空");
        }
        
        // 生成设备唯一标识
        String deviceId = deviceIdentificationManager.generateDeviceId(config);
        
        // 检查设备是否已有驱动实例
        DeviceDriver existingDriver = deviceDriverMap.get(deviceId);
        if (existingDriver != null) {
            return existingDriver;
        }
        
        // 查找支持该设备的驱动
        List<DeviceDriver> drivers = driverRegistry.get(config.getDeviceType());
        if (drivers != null && !drivers.isEmpty()) {
            // 选择第一个支持该设备的驱动
            for (DeviceDriver driver : drivers) {
                if (driver.isSupported(config)) {
                    // 初始化驱动
                    if (driver.init(config)) {
                        // 连接设备
                        if (driver.connect()) {
                            deviceDriverMap.put(deviceId, driver);
                            log.info("为设备 {} 分配驱动: {} 版本: {}", 
                                    deviceId, driver.getDriverName(), driver.getDriverVersion());
                            
                            // 添加心跳检测
                            heartbeatManager.addHeartbeatForDevice(deviceId, driver);
                            
                            return driver;
                        }
                    }
                }
            }
        }
        
        log.warn("未找到适合设备的驱动: 设备类型={} 设备名称={}", config.getDeviceType(), config.getDeviceName());
        return null;
    }
    
    /**
     * 获取设备使用的驱动实例
     * @param deviceId 设备唯一标识
     * @return 驱动实例
     */
    public DeviceDriver getDriverByDeviceId(String deviceId) {
        return deviceDriverMap.get(deviceId);
    }
    
    /**
     * 释放设备驱动
     * @param deviceId 设备唯一标识
     */
    public void releaseDriver(String deviceId) {
        DeviceDriver driver = deviceDriverMap.remove(deviceId);
        if (driver != null) {
            // 移除心跳检测
            heartbeatManager.removeHeartbeatForDevice(deviceId);
            
            driver.disconnect();
            driver.close();
            log.info("释放设备驱动: {} 设备ID: {}", driver.getDriverName(), deviceId);
        }
    }
    
    /**
     * 获取支持指定设备类型的驱动列表
     * @param deviceType 设备类型
     * @return 驱动列表
     */
    public List<DeviceDriver> getDriversByDeviceType(String deviceType) {
        return driverRegistry.getOrDefault(deviceType, Collections.emptyList());
    }
    
    /**
     * 获取所有注册的驱动
     * @return 所有驱动列表
     */
    public List<DeviceDriver> getAllDrivers() {
        List<DeviceDriver> allDrivers = new ArrayList<>();
        for (List<DeviceDriver> drivers : driverRegistry.values()) {
            allDrivers.addAll(drivers);
        }
        return allDrivers;
    }
    
    /**
     * 获取设备驱动数量
     * @return 设备驱动数量
     */
    public int getDriverCount() {
        return getAllDrivers().size();
    }
    
    /**
     * 重新加载外部驱动
     * @return 重新加载的驱动数量
     */
    public int reloadExternalDrivers() {
        log.info("开始重新加载外部驱动");
        
        // 卸载所有动态加载的驱动
        unloadAllDynamicallyLoadedDrivers();
        
        // 重新加载外部驱动
        List<DeviceDriver> externalDrivers = dynamicDriverLoader.loadDriversFromDirectory();
        for (DeviceDriver driver : externalDrivers) {
            registerDriver(driver);
            dynamicallyLoadedDrivers.add(driver);
        }
        
        log.info("重新加载外部驱动完成，共加载 {} 个驱动", externalDrivers.size());
        return externalDrivers.size();
    }
    
    /**
     * 从指定文件加载驱动
     * @param jarFilePath 驱动jar文件路径
     * @return 加载的驱动数量
     * @throws Exception 加载异常
     */
    public int loadDriverFromFile(String jarFilePath) throws Exception {
        log.info("开始从文件加载驱动: {}", jarFilePath);
        
        List<DeviceDriver> drivers = dynamicDriverLoader.loadDriver(jarFilePath);
        for (DeviceDriver driver : drivers) {
            registerDriver(driver);
            dynamicallyLoadedDrivers.add(driver);
        }
        
        log.info("从文件加载驱动完成，共加载 {} 个驱动", drivers.size());
        return drivers.size();
    }
    
    /**
     * 卸载所有动态加载的驱动
     */
    public void unloadAllDynamicallyLoadedDrivers() {
        log.info("开始卸载所有动态加载的驱动");
        
        for (DeviceDriver driver : dynamicallyLoadedDrivers) {
            // 注销驱动
            unregisterDriver(driver);
            
            // 关闭驱动
            driver.close();
        }
        
        dynamicallyLoadedDrivers.clear();
        
        // 卸载驱动类加载器
        dynamicDriverLoader.unloadAllDrivers();
        
        log.info("所有动态加载的驱动已卸载");
    }
    
    /**
     * 关闭驱动管理器，释放所有驱动资源
     */
    public void shutdown() {
        log.info("开始关闭设备驱动管理器");
        
        // 停止心跳检测服务
        heartbeatManager.stop();
        
        // 释放所有设备驱动
        for (Map.Entry<String, DeviceDriver> entry : deviceDriverMap.entrySet()) {
            String deviceId = entry.getKey();
            DeviceDriver driver = entry.getValue();
            
            // 移除心跳检测
            heartbeatManager.removeHeartbeatForDevice(deviceId);
            
            driver.disconnect();
            driver.close();
            log.info("关闭设备驱动: {} 设备ID: {}", driver.getDriverName(), deviceId);
        }
        
        deviceDriverMap.clear();
        driverRegistry.clear();
        
        log.info("设备驱动管理器已关闭");
    }
}