package com.foodtraceability.service;

import com.foodtraceability.entity.HardwareConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 设备标识管理器
 * 负责生成和管理设备的唯一标识
 */
@Component
public class DeviceIdentificationManager {
    
    private static final Logger log = LoggerFactory.getLogger(DeviceIdentificationManager.class);
    
    // 设备标识缓存，key为设备唯一标识，value为设备配置
    private final Map<String, HardwareConfig> deviceIdCache;
    
    // MAC地址正则表达式（改进版，避免匹配WSD设备标识符）
    private static final Pattern MAC_PATTERN = Pattern.compile("\\b([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})\\b");
    
    /**
     * 构造函数
     */
    public DeviceIdentificationManager() {
        this.deviceIdCache = new ConcurrentHashMap<>();
        log.info("设备标识管理器初始化完成");
    }
    
    /**
     * 生成设备唯一标识
     * @param config 设备配置
     * @return 设备唯一标识
     */
    public String generateDeviceId(HardwareConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("设备配置不能为空");
        }
        
        // 尝试从配置中获取设备唯一标识
        if (config.getId() != null) {
            String deviceId = String.format("%s_%d", config.getDeviceType(), config.getId());
            deviceIdCache.put(deviceId, config);
            return deviceId;
        }
        
        // 尝试从配置JSON中提取设备序列号或MAC地址
        String serialNumber = extractSerialNumber(config.getConfigJson());
        String macAddress = extractMacAddress(config.getConfigJson());
        
        // 生成唯一标识（设备类型_设备型号_序列号/IP_端口/MAC地址）
        StringBuilder sb = new StringBuilder();
        sb.append(config.getDeviceType()).append("_");
        
        if (config.getDeviceModel() != null) {
            sb.append(config.getDeviceModel().replaceAll("[^a-zA-Z0-9]", "_")).append("_");
        }
        
        if (serialNumber != null) {
            sb.append(serialNumber).append("_");
        } else if (macAddress != null) {
            sb.append(macAddress.replaceAll("[:-]", "")).append("_");
        } else if (config.getIpAddress() != null) {
            sb.append(config.getIpAddress().replace(".", "_")).append("_");
        }
        
        if (config.getPort() != null) {
            sb.append(config.getPort());
        } else if (config.getBaudRate() != null) {
            sb.append(config.getBaudRate());
        }
        
        // 去除尾部的下划线
        String deviceId = sb.toString().replaceAll("_$", "");
        deviceIdCache.put(deviceId, config);
        
        log.info("生成设备唯一标识: {} 设备类型: {} IP: {} 端口: {}", 
                deviceId, config.getDeviceType(), config.getIpAddress(), config.getPort());
        
        return deviceId;
    }
    
    /**
     * 从配置JSON中提取设备序列号
     * @param configJson 配置JSON字符串
     * @return 设备序列号
     */
    private String extractSerialNumber(String configJson) {
        if (configJson == null) {
            return null;
        }
        
        // 简单的序列号提取逻辑，实际项目中应使用JSON解析
        Pattern serialPattern = Pattern.compile("serialNumber.*?:.*?[\"']([^\"']+)[\"']");
        Matcher matcher = serialPattern.matcher(configJson);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }
    
    /**
     * 从配置JSON中提取MAC地址
     * @param configJson 配置JSON字符串
     * @return MAC地址
     */
    private String extractMacAddress(String configJson) {
        if (configJson == null) {
            return null;
        }
        
        // 改进的MAC地址提取逻辑，避免匹配WSD设备标识符
        // MAC地址格式：XX:XX:XX:XX:XX:XX 或 XX-XX-XX-XX-XX-XX
        Matcher matcher = MAC_PATTERN.matcher(configJson);
        if (matcher.find()) {
            String macAddress = matcher.group(0);
            // 确保不是WSD设备标识符（WSD-开头）
            if (!macAddress.startsWith("WSD-")) {
                return macAddress;
            }
        }
        
        return null;
    }
    
    /**
     * 根据设备唯一标识获取设备配置
     * @param deviceId 设备唯一标识
     * @return 设备配置
     */
    public HardwareConfig getDeviceConfig(String deviceId) {
        return deviceIdCache.get(deviceId);
    }
    
    /**
     * 注册设备标识
     * @param deviceId 设备唯一标识
     * @param config 设备配置
     */
    public void registerDeviceId(String deviceId, HardwareConfig config) {
        deviceIdCache.put(deviceId, config);
        log.info("注册设备标识: {} 设备名称: {}", deviceId, config.getDeviceName());
    }
    
    /**
     * 注销设备标识
     * @param deviceId 设备唯一标识
     */
    public void unregisterDeviceId(String deviceId) {
        HardwareConfig config = deviceIdCache.remove(deviceId);
        if (config != null) {
            log.info("注销设备标识: {} 设备名称: {}", deviceId, config.getDeviceName());
        }
    }
    
    /**
     * 检查设备标识是否已注册
     * @param deviceId 设备唯一标识
     * @return 是否已注册
     */
    public boolean isDeviceIdRegistered(String deviceId) {
        return deviceIdCache.containsKey(deviceId);
    }
    
    /**
     * 获取设备标识缓存大小
     * @return 缓存大小
     */
    public int getDeviceIdCacheSize() {
        return deviceIdCache.size();
    }
    
    /**
     * 获取所有设备配置
     * @return 所有设备配置映射
     */
    public Map<String, HardwareConfig> getAllDeviceConfigs() {
        return new ConcurrentHashMap<>(deviceIdCache);
    }
}