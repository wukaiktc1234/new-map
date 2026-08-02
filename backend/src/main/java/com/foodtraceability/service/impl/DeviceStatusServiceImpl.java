package com.foodtraceability.service.impl;

import com.foodtraceability.entity.DeviceStatus;
import com.foodtraceability.entity.HardwareConfig;
import com.foodtraceability.constant.DeviceConstants;
import com.foodtraceability.mapper.HardwareConfigMapper;
import com.foodtraceability.service.device.DeviceAlertService;
import com.foodtraceability.service.DeviceConnectionService;
import com.foodtraceability.service.DeviceDriverCompatibilityService;
import com.foodtraceability.service.DeviceStatusHistoryService;
import com.foodtraceability.service.DeviceStatusService;
import com.foodtraceability.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 设备状态服务实现类
 * 负责设备状态的获取、检测和缓存管理
 */
@Service
public class DeviceStatusServiceImpl implements DeviceStatusService {
    
    private static final Logger log = LoggerFactory.getLogger(DeviceStatusServiceImpl.class);
    
    // 设备类型列表
    private static final List<String> DEVICE_TYPES = DeviceConstants.DEVICE_TYPES;
    
    // 设备状态缓存有效期（秒）
    private static final long DEFAULT_CACHE_EXPIRY = DeviceConstants.DEVICE_STATUS_CACHE_EXPIRY; // 默认5分钟
    private static final long SCANNER_CACHE_EXPIRY = 60; // 扫码枪1分钟
    private static final long PRINTER_CACHE_EXPIRY = 5 * 60; // 打印机5分钟
    private static final long CAMERA_CACHE_EXPIRY = 10 * 60; // 摄像头10分钟
    
    // 异步线程池，用于处理设备状态检测

    public DeviceStatusServiceImpl(HardwareConfigMapper hardwareConfigMapper, DeviceConnectionService deviceConnectionService, DeviceAlertService deviceAlertService, DeviceDriverCompatibilityService deviceDriverCompatibilityService, DeviceStatusHistoryService deviceStatusHistoryService) {
        this.hardwareConfigMapper = hardwareConfigMapper;
        this.deviceConnectionService = deviceConnectionService;
        this.deviceAlertService = deviceAlertService;
        this.deviceDriverCompatibilityService = deviceDriverCompatibilityService;
        this.deviceStatusHistoryService = deviceStatusHistoryService;
    }

    private final ExecutorService deviceStatusThreadPool = Executors.newFixedThreadPool(
        Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
        r -> {
            Thread thread = new Thread(r, "device-status-checker-");
            thread.setDaemon(true);
            return thread;
        }
    );

    // 设备状态内存缓存：deviceType -> 状态条目（带TTL）
    private final ConcurrentHashMap<String, CacheEntry<Boolean>> deviceStatusCache = new ConcurrentHashMap<>();
    // 设备详细状态内存缓存：deviceType -> 详细状态条目（带TTL）
    private final ConcurrentHashMap<String, CacheEntry<DeviceStatus>> deviceDetailedStatusCache = new ConcurrentHashMap<>();

    /**
     * 内存缓存条目，记录值和过期时间
     */
    private static class CacheEntry<T> {
        final T value;
        final long expireTimeMillis;

        CacheEntry(T value, long expireTimeMillis) {
            this.value = value;
            this.expireTimeMillis = expireTimeMillis;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireTimeMillis;
        }
    }
    
    /**
     * 根据设备类型获取缓存有效期
     * @param deviceType 设备类型
     * @return 缓存有效期（秒）
     */
    private long getCacheExpiryByDeviceType(String deviceType) {
        switch (deviceType) {
            case "SCANNER":
                return SCANNER_CACHE_EXPIRY;
            case "PRINTER":
                return PRINTER_CACHE_EXPIRY;
            case "CAMERA":
                return CAMERA_CACHE_EXPIRY;
            default:
                return DEFAULT_CACHE_EXPIRY;
        }
    }
    
    private final HardwareConfigMapper hardwareConfigMapper;

    private final DeviceConnectionService deviceConnectionService;

    private final DeviceAlertService deviceAlertService;
    
    private final DeviceDriverCompatibilityService deviceDriverCompatibilityService;
    
    private final DeviceStatusHistoryService deviceStatusHistoryService;
    
    /**
     * 获取设备状态（简单状态）
     * @param deviceType 设备类型
     * @return 设备在线状态：true表示在线，false表示离线
     * @throws IllegalArgumentException 当设备类型为空时抛出
     */
    @Override
    public Boolean getDeviceStatus(String deviceType) {
        try {
            log.info("获取设备状态: {}", deviceType);

            // 参数校验
            if (deviceType == null || deviceType.trim().isEmpty()) {
                throw new IllegalArgumentException("设备类型不能为空");
            }

            // 检查内存缓存中是否有设备状态
            CacheEntry<Boolean> cachedEntry = deviceStatusCache.get(deviceType);
            if (cachedEntry != null && !cachedEntry.isExpired()) {
                log.info("使用内存缓存的设备状态: {} - {}", deviceType, cachedEntry.value);
                return cachedEntry.value;
            }

            // 缓存不存在或已过期，重新检测设备状态
            HardwareConfig config = getHardwareConfig(deviceType);
            if (config == null) {
                log.warn("设备配置不存在: {}", deviceType);
                // 更新内存缓存，有效期根据设备类型调整
                long cacheExpiry = getCacheExpiryByDeviceType(deviceType);
                deviceStatusCache.put(deviceType, new CacheEntry<>(false, System.currentTimeMillis() + cacheExpiry * 1000));
                return false;
            }

            // 结合设备配置状态和实际连接测试
            Boolean result = config.getStatus() == 1 && deviceConnectionService.testConnection(config);

            // 更新内存缓存，有效期根据设备类型调整
            long cacheExpiry = getCacheExpiryByDeviceType(deviceType);
            deviceStatusCache.put(deviceType, new CacheEntry<>(result, System.currentTimeMillis() + cacheExpiry * 1000));

            log.info("设备状态检测结果: {} - {}", deviceType, result);
            return result;
        } catch (IllegalArgumentException e) {
            log.error("获取设备状态失败 - 参数错误: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("获取设备状态失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 从缓存或数据库获取设备配置
     * @param deviceType 设备类型
     * @return 设备配置对象，如果未找到则返回默认配置
     * @throws IllegalArgumentException 当设备类型为空时抛出
     */
    @Override
    public HardwareConfig getHardwareConfig(String deviceType) {
        try {
            // 参数校验
            if (deviceType == null || deviceType.trim().isEmpty()) {
                throw new IllegalArgumentException("设备类型不能为空");
            }
            
            String storeIdStr = SecurityUtils.getCurrentUserStoreId();
            Long storeId = null;
            
            if (storeIdStr == null) {
                // 当没有认证用户时，使用默认的storeId
                log.info("未找到当前用户门店ID，使用默认门店ID: {}", DeviceConstants.DEFAULT_STORE_ID);
                storeId = DeviceConstants.DEFAULT_STORE_ID; // 默认门店ID
            } else {
                try {
                    storeId = Long.parseLong(storeIdStr);
                } catch (NumberFormatException e) {
                    log.warn("门店ID格式错误: {}, 使用默认门店ID", storeIdStr);
                    storeId = DeviceConstants.DEFAULT_STORE_ID;
                }
            }
            
            HardwareConfig config = hardwareConfigMapper.selectConfig(String.valueOf(storeId), deviceType);
            if (config == null && storeId != DeviceConstants.DEFAULT_STORE_ID) {
                // 如果指定门店没有配置，尝试使用默认门店配置
                log.info("门店{}未找到{}设备配置，尝试使用默认配置", storeId, deviceType);
                config = hardwareConfigMapper.selectConfig(String.valueOf(DeviceConstants.DEFAULT_STORE_ID), deviceType);
            }
            
            if (config == null) {
                // 如果仍然没有配置，返回一个默认配置对象
                log.info("未找到{}设备的任何配置，返回默认配置", deviceType);
                config = createDefaultHardwareConfig(deviceType);
            }
            
            return config;
        } catch (IllegalArgumentException e) {
            log.error("获取硬件配置失败 - 参数错误: {}", e.getMessage());
            // 发生参数异常时返回默认配置
            return createDefaultHardwareConfig(deviceType);
        } catch (Exception e) {
            log.error("获取硬件配置失败: {}", e.getMessage(), e);
            // 发生其他异常时返回默认配置
            return createDefaultHardwareConfig(deviceType);
        }
    }
    
    /**
     * 创建默认设备配置
     * @param deviceType 设备类型
     * @return 默认的设备配置对象
     * @throws IllegalArgumentException 当设备类型为空时抛出
     */
    @Override
    public HardwareConfig createDefaultHardwareConfig(String deviceType) {
        if (deviceType == null || deviceType.trim().isEmpty()) {
            throw new IllegalArgumentException("设备类型不能为空");
        }
        
        HardwareConfig config = new HardwareConfig();
        config.setDeviceType(deviceType);
        config.setStoreId(DeviceConstants.DEFAULT_STORE_ID); // 使用默认门店ID
        config.setDeviceName(getDefaultDeviceName(deviceType));
        config.setIpAddress(getDefaultDeviceIp(deviceType));
        config.setPort(getDefaultDevicePort(deviceType));
        config.setDeviceModel(getDefaultDeviceModel(deviceType));
        config.setConnectionType(getDefaultConnectionType(deviceType));
        config.setStatus(0); // 默认状态为未连接
        config.setCreatedBy("system");
        config.setUpdatedBy("system");
        return config;
    }
    
    private String getDefaultConnectionType(String deviceType) {
        if (deviceType == null) return "NETWORK";
        
        switch (deviceType.toUpperCase()) {
            case "LABEL_PRINTER":
                return "WINDOWS";
            case "PRINTER":
                return "NETWORK";
            case "SCANNER":
                return "USB";
            case "SCALE":
                return "SERIAL";
            default:
                return "NETWORK";
        }
    }
    
    @Override
    public DeviceStatus getDeviceDetailedStatus(String deviceType) {
        try {
            // 检查内存缓存中是否有设备详细状态
            CacheEntry<DeviceStatus> cachedEntry = deviceDetailedStatusCache.get(deviceType);
            if (cachedEntry != null && !cachedEntry.isExpired()) {
                DeviceStatus cachedStatus = cachedEntry.value;
                log.info("使用内存缓存的设备详细状态: {} - {}", deviceType, cachedStatus.isOnline());
                return cachedStatus;
            }

            // 缓存不存在或已过期，重新检测设备状态
            DeviceStatus status = new DeviceStatus();
            status.setDeviceType(deviceType);
            status.setLastCheckTime(new Date());

            try {
                // 获取设备配置
                HardwareConfig config = getHardwareConfig(deviceType);
                status.setDeviceId(config.getId());
                status.setDeviceName(config.getDeviceName());
                status.setDeviceModel(config.getDeviceModel());
                status.setConnectionType(config.getConnectionType());
                status.setIpAddress(config.getIpAddress());
                status.setPort(config.getPort());

                // 检测设备在线状态
                long startTime = System.currentTimeMillis();
                boolean online = deviceConnectionService.testConnection(config);
                long responseTime = System.currentTimeMillis() - startTime;

                status.setOnline(online);
                status.setResponseTime(responseTime);

                if (online) {
                    // 模拟设备固件版本（实际项目中应该从设备获取）
                    String firmwareVersion = DeviceConstants.DEFAULT_FIRMWARE_VERSION;
                    status.setFirmwareVersion(firmwareVersion);

                    // 检查设备驱动版本兼容性
                    boolean isCompatible = deviceDriverCompatibilityService.checkCompatibility(deviceType, firmwareVersion);
                    if (isCompatible) {
                        status.setDetails("设备在线，响应正常，驱动版本兼容");
                    } else {
                        status.setDetails("设备在线，响应正常，但驱动版本不兼容");
                        // 记录兼容性警告日志
                        log.warn("设备驱动版本不兼容: 设备类型={}, 当前版本={}, 推荐版本范围={}~{}",
                                 deviceType, firmwareVersion,
                                 deviceDriverCompatibilityService.getMinCompatibleVersion(deviceType),
                                 deviceDriverCompatibilityService.getRecommendedVersion(deviceType));
                    }
                } else {
                    status.setDetails("设备离线，无法连接");
                    status.setErrorMessage("连接超时或设备未响应");
                }
            } catch (IllegalArgumentException ex) {
                log.error("获取设备详细状态失败 - 参数错误: {}", ex.getMessage());
                status.setOnline(false);
                status.setDetails("参数错误: " + ex.getMessage());
                status.setErrorMessage(ex.getMessage());
            } catch (Exception e) {
                log.error("获取设备详细状态失败: {}", e.getMessage(), e);
                status.setOnline(false);
                status.setDetails("获取设备状态失败");
                status.setErrorMessage(e.getMessage());
            }

            // 检查设备状态并触发告警
            deviceAlertService.checkDeviceStatusAndAlert(status);

            // 记录设备状态历史
            deviceStatusHistoryService.recordDeviceStatus(status);

            // 更新内存缓存，有效期根据设备类型调整
            long cacheExpiry = getCacheExpiryByDeviceType(deviceType);
            deviceDetailedStatusCache.put(deviceType, new CacheEntry<>(status, System.currentTimeMillis() + cacheExpiry * 1000));

            return status;
        } catch (Exception e) {
            log.error("获取设备详细状态失败: {}", e.getMessage(), e);
            // 缓存操作失败时，直接返回新的状态对象
            DeviceStatus status = new DeviceStatus();
            status.setDeviceType(deviceType);
            status.setLastCheckTime(new Date());
            status.setOnline(false);
            status.setDetails("获取设备状态失败");
            status.setErrorMessage(e.getMessage());
            return status;
        }
    }
    
    @Override
    public void clearDeviceStatusCache(String deviceType) {
        // 清理内存缓存中的设备状态
        deviceStatusCache.remove(deviceType);
        deviceDetailedStatusCache.remove(deviceType);

        log.info("已清除内存中的设备状态缓存: {}", deviceType);
    }
    
    private String getDefaultDeviceName(String deviceType) {
        switch (deviceType) {
            case "SCANNER": return "默认扫码枪";
            case "PRINTER": return "默认打印机";
            case "CAMERA": return "默认摄像头";
            default: return "默认设备";
        }
    }
    
    private String getDefaultDeviceIp(String deviceType) {
        switch (deviceType) {
            case "SCANNER": return DeviceConstants.DEFAULT_SCANNER_IP;
            case "PRINTER": return DeviceConstants.DEFAULT_PRINTER_IP;
            case "CAMERA": return DeviceConstants.DEFAULT_CAMERA_IP;
            default: return DeviceConstants.DEFAULT_IP_ADDRESS;
        }
    }
    
    private String getDefaultDevicePort(String deviceType) {
        switch (deviceType) {
            case "SCANNER": return DeviceConstants.DEFAULT_PORT;
            case "PRINTER": return DeviceConstants.DEFAULT_PRINTER_PORT;
            case "CAMERA": return DeviceConstants.DEFAULT_PORT;
            default: return DeviceConstants.DEFAULT_PORT;
        }
    }
    
    private String getDefaultDeviceModel(String deviceType) {
        switch (deviceType) {
            case "SCANNER": return "Honeywell 1900";
            case "PRINTER": return "EPSON TM-T88V";
            case "CAMERA": return "Hikvision DS-2CD";
            default: return "通用型号";
        }
    }
    
    /**
     * 异步检测设备状态
     * @param deviceType 设备类型
     */
    @Override
    public void asyncCheckDeviceStatus(String deviceType) {
        deviceStatusThreadPool.submit(() -> {
            try {
                log.info("开始异步检测设备状态: {}", deviceType);
                // 调用现有的设备详细状态检测方法，会自动更新缓存
                getDeviceDetailedStatus(deviceType);
                log.info("异步检测设备状态完成: {}", deviceType);
            } catch (Exception e) {
                log.error("异步检测设备状态失败: {}", e.getMessage(), e);
            }
        });
    }
    
    /**
     * 异步检测所有设备状态
     */
    @Override
    public void asyncCheckAllDeviceStatus() {
        log.info("开始异步检测所有设备状态");
        
        for (String deviceType : DEVICE_TYPES) {
            asyncCheckDeviceStatus(deviceType);
        }
        
        log.info("所有设备状态异步检测任务已提交");
    }
    
    /**
     * 获取所有设备状态
     * @return 所有设备状态，key为设备类型，value为设备状态
     */
    @Override
    public Map<String, DeviceStatus> getAllDeviceStatus() {
        log.info("获取所有设备状态");
        Map<String, DeviceStatus> allStatus = new HashMap<>();
        
        for (String deviceType : DEVICE_TYPES) {
            DeviceStatus status = getDeviceDetailedStatus(deviceType);
            allStatus.put(deviceType, status);
        }
        
        return allStatus;
    }

    /**
     * 获取所有设备配置
     * @param storeId 门店ID
     * @return 所有设备配置
     */
    @Override
    public List<HardwareConfig> getAllDevices(Long storeId) {
        log.info("获取所有设备配置，门店ID: {}", storeId);
        
        List<HardwareConfig> allConfigs = new java.util.ArrayList<>();
        
        for (String deviceType : DEVICE_TYPES) {
            try {
                HardwareConfig config = hardwareConfigMapper.selectConfig(String.valueOf(storeId), deviceType);
                if (config == null) {
                    config = createDefaultHardwareConfig(deviceType);
                }
                allConfigs.add(config);
            } catch (Exception e) {
                log.error("获取设备配置失败: {}", e.getMessage(), e);
                allConfigs.add(createDefaultHardwareConfig(deviceType));
            }
        }
        
        return allConfigs;
    }
}