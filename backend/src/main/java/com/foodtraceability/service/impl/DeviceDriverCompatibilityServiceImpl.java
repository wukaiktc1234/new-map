package com.foodtraceability.service.impl;

import com.foodtraceability.entity.DeviceStatus;
import com.foodtraceability.entity.HardwareConfig;
import com.foodtraceability.service.DeviceDriverCompatibilityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 设备驱动兼容性检查服务实现类
 * 实现设备驱动版本与系统的兼容性检查
 */
@Service
public class DeviceDriverCompatibilityServiceImpl implements DeviceDriverCompatibilityService {
    
    private static final Logger log = LoggerFactory.getLogger(DeviceDriverCompatibilityServiceImpl.class);
    
    // 设备驱动兼容性配置
    private static final Map<String, DriverCompatibilityConfig> COMPATIBILITY_CONFIGS = new HashMap<>();
    
    static {
        // 初始化设备兼容性配置
        COMPATIBILITY_CONFIGS.put("SCANNER", new DriverCompatibilityConfig("1.0.0", "2.1.0"));
        COMPATIBILITY_CONFIGS.put("PRINTER", new DriverCompatibilityConfig("1.5.0", "3.2.0"));
        COMPATIBILITY_CONFIGS.put("CAMERA", new DriverCompatibilityConfig("2.0.0", "4.0.0"));
    }
    
    @Override
    public boolean checkCompatibility(String deviceType, String firmwareVersion) {
        if (deviceType == null || firmwareVersion == null) {
            log.warn("设备类型或固件版本为空，无法检查兼容性");
            return false;
        }
        
        DriverCompatibilityConfig config = COMPATIBILITY_CONFIGS.get(deviceType);
        if (config == null) {
            log.warn("未找到设备类型的兼容性配置: {}", deviceType);
            return true; // 默认兼容
        }
        
        int minCompare = compareVersions(firmwareVersion, config.getMinVersion());
        int maxCompare = compareVersions(firmwareVersion, config.getRecommendedVersion());
        
        boolean isCompatible = minCompare >= 0 && maxCompare <= 0;
        log.info("设备兼容性检查结果: 设备类型={}, 固件版本={}, 最小兼容版本={}, 推荐版本={}, 兼容={}", 
                 deviceType, firmwareVersion, config.getMinVersion(), config.getRecommendedVersion(), isCompatible);
        
        return isCompatible;
    }
    
    @Override
    public String getMinCompatibleVersion(String deviceType) {
        DriverCompatibilityConfig config = COMPATIBILITY_CONFIGS.get(deviceType);
        return config != null ? config.getMinVersion() : "1.0.0";
    }
    
    @Override
    public String getRecommendedVersion(String deviceType) {
        DriverCompatibilityConfig config = COMPATIBILITY_CONFIGS.get(deviceType);
        return config != null ? config.getRecommendedVersion() : "1.0.0";
    }
    
    @Override
    public boolean checkConfigCompatibility(HardwareConfig config) {
        if (config == null) {
            return false;
        }
        
        // 简单实现：检查设备类型是否支持
        return COMPATIBILITY_CONFIGS.containsKey(config.getDeviceType());
    }
    
    @Override
    public boolean checkStatusCompatibility(DeviceStatus status) {
        if (status == null || status.getFirmwareVersion() == null) {
            return false;
        }
        
        return checkCompatibility(status.getDeviceType(), status.getFirmwareVersion());
    }
    
    /**
     * 比较两个版本号
     * @param version1 第一个版本号
     * @param version2 第二个版本号
     * @return 1: version1 > version2, 0: version1 = version2, -1: version1 < version2
     */
    private int compareVersions(String version1, String version2) {
        if (version1.equals(version2)) {
            return 0;
        }
        
        String[] v1Parts = version1.split("\\.");
        String[] v2Parts = version2.split("\\.");
        
        int length = Math.max(v1Parts.length, v2Parts.length);
        for (int i = 0; i < length; i++) {
            int v1 = i < v1Parts.length ? Integer.parseInt(v1Parts[i]) : 0;
            int v2 = i < v2Parts.length ? Integer.parseInt(v2Parts[i]) : 0;
            
            if (v1 > v2) {
                return 1;
            } else if (v1 < v2) {
                return -1;
            }
        }
        
        return 0;
    }
    
    /**
     * 设备驱动兼容性配置类
     */
    private static class DriverCompatibilityConfig {
        private final String minVersion;
        private final String recommendedVersion;
        
        public DriverCompatibilityConfig(String minVersion, String recommendedVersion) {
            this.minVersion = minVersion;
            this.recommendedVersion = recommendedVersion;
        }
        
        public String getMinVersion() {
            return minVersion;
        }
        
        public String getRecommendedVersion() {
            return recommendedVersion;
        }
    }
}