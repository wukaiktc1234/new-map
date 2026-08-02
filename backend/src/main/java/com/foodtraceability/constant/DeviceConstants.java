package com.foodtraceability.constant;

/**
 * 设备管理相关常量
 * 集中管理设备管理模块的默认值和配置
 */
public class DeviceConstants {
    
    // 默认门店ID
    public static final Long DEFAULT_STORE_ID = 1L;
    
    // 默认IP地址
    public static final String DEFAULT_IP_ADDRESS = "192.168.1.100";
    
    // 默认端口
    public static final String DEFAULT_PORT = "8080";
    
    // 默认波特率
    public static final String DEFAULT_BAUD_RATE = "9600";
    
    // 默认固件版本
    public static final String DEFAULT_FIRMWARE_VERSION = "1.0.0";
    
    // 打印机默认IP地址
    public static final String DEFAULT_PRINTER_IP = "192.168.1.101";
    
    // 打印机默认端口
    public static final String DEFAULT_PRINTER_PORT = "9100";
    
    // 扫码枪默认IP地址
    public static final String DEFAULT_SCANNER_IP = "192.168.1.100";
    
    // 摄像头默认IP地址
    public static final String DEFAULT_CAMERA_IP = "192.168.1.102";
    
    // 设备状态缓存有效期（秒）
    public static final long DEVICE_STATUS_CACHE_EXPIRY = 5 * 60;
    
    // 设备类型列表
    public static final java.util.List<String> DEVICE_TYPES = java.util.List.of("SCANNER", "PRINTER", "CAMERA");
}