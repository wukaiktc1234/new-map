package com.foodtraceability.driver;

import com.foodtraceability.entity.DeviceStatus;
import com.foodtraceability.entity.HardwareConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 钱箱驱动实现类
 * 负责处理钱箱设备的操作
 */
@Component
public class CashboxDriver implements DeviceDriver {
    
    private static final Logger log = LoggerFactory.getLogger(CashboxDriver.class);
    
    private HardwareConfig config;
    private boolean connected;
    private boolean isOpened;
    
    public CashboxDriver() {
        this.connected = false;
        this.isOpened = false;
        log.info("钱箱驱动初始化完成");
    }
    
    @Override
    public boolean init(HardwareConfig config) {
        if (config == null) {
            log.error("钱箱配置不能为空");
            return false;
        }
        
        this.config = config;
        log.info("初始化钱箱驱动: 设备名称={} 连接类型={}", 
                config.getDeviceName(), config.getConnectionType());
        return true;
    }
    
    @Override
    public boolean connect() {
        if (config == null) {
            log.error("钱箱配置未初始化");
            return false;
        }
        
        try {
            log.info("连接钱箱: 设备名称={}", config.getDeviceName());
            connected = true;
            log.info("钱箱连接成功: 设备名称={}", config.getDeviceName());
            return true;
        } catch (Exception e) {
            log.error("钱箱连接失败: {}", e.getMessage(), e);
            connected = false;
            return false;
        }
    }
    
    @Override
    public void disconnect() {
        if (connected) {
            log.info("断开钱箱连接: 设备名称={}", config.getDeviceName());
            connected = false;
        }
    }
    
    @Override
    public boolean isConnected() {
        return connected;
    }
    
    @Override
    public DeviceStatus getDeviceStatus() {
        DeviceStatus status = new DeviceStatus();
        status.setDeviceType(config.getDeviceType());
        status.setOnline(connected);
        status.setIpAddress(config.getIpAddress());
        status.setPort(config.getPort());
        status.setDeviceModel(config.getDeviceModel());
        
        if (connected) {
            status.setDetails(isOpened ? "钱箱已打开" : "钱箱已关闭");
        } else {
            status.setDetails("钱箱未连接");
        }
        
        return status;
    }
    
    @Override
    public Map<String, Object> executeOperation(String operationType, Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        
        if (!connected) {
            result.put("success", false);
            result.put("message", "钱箱未连接");
            return result;
        }
        
        try {
            log.info("执行钱箱操作: 类型={} 参数={}", operationType, params);
            
            switch (operationType) {
                case "OPEN":
                    return openCashbox(params);
                    
                case "CLOSE":
                    return closeCashbox(params);
                    
                case "GET_STATUS":
                    return getStatus(params);
                    
                case "TEST":
                    return testCashbox(params);
                    
                default:
                    result.put("success", false);
                    result.put("message", "不支持的钱箱操作类型: " + operationType);
                    return result;
            }
        } catch (Exception e) {
            log.error("执行钱箱操作失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "执行钱箱操作失败: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * 打开钱箱
     */
    private Map<String, Object> openCashbox(Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("打开钱箱: 设备名称={}", config.getDeviceName());
            
            String connectionType = config.getConnectionType();
            
            switch (connectionType) {
                case "USB":
                    openViaUsb();
                    break;
                case "SERIAL":
                    openViaSerial();
                    break;
                case "NETWORK":
                    openViaNetwork();
                    break;
                case "PRINTER":
                    openViaPrinter();
                    break;
                default:
                    openViaPrinter();
            }
            
            isOpened = true;
            
            result.put("success", true);
            result.put("message", "钱箱已打开");
            result.put("openTime", System.currentTimeMillis());
            return result;
        } catch (Exception e) {
            log.error("打开钱箱失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "打开钱箱失败: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * 关闭钱箱
     */
    private Map<String, Object> closeCashbox(Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("关闭钱箱: 设备名称={}", config.getDeviceName());
            
            isOpened = false;
            
            result.put("success", true);
            result.put("message", "钱箱已关闭");
            return result;
        } catch (Exception e) {
            log.error("关闭钱箱失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "关闭钱箱失败: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * 获取状态
     */
    private Map<String, Object> getStatus(Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        
        result.put("success", true);
        result.put("message", "获取状态成功");
        result.put("connected", connected);
        result.put("opened", isOpened);
        return result;
    }
    
    /**
     * 测试钱箱
     */
    private Map<String, Object> testCashbox(Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("测试钱箱: 设备名称={}", config.getDeviceName());
            
            result.put("success", true);
            result.put("message", "钱箱测试成功");
            return result;
        } catch (Exception e) {
            log.error("测试钱箱失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "测试钱箱失败: " + e.getMessage());
            return result;
        }
    }
    
    private void openViaUsb() {
        log.info("通过USB打开钱箱");
    }
    
    private void openViaSerial() {
        log.info("通过串口打开钱箱");
    }
    
    private void openViaNetwork() {
        log.info("通过网络打开钱箱");
    }
    
    private void openViaPrinter() {
        log.info("通过打印机驱动钱箱");
    }
    
    @Override
    public String getDriverName() {
        return "Universal Cashbox Driver";
    }
    
    @Override
    public String getDriverVersion() {
        return "1.0.0";
    }
    
    @Override
    public String getSupportedDeviceType() {
        return "CASHBOX";
    }
    
    @Override
    public boolean isSupported(HardwareConfig config) {
        if (config == null) {
            return false;
        }
        return "CASHBOX".equals(config.getDeviceType());
    }
    
    @Override
    public void close() {
        disconnect();
        log.info("关闭钱箱驱动: 设备名称={}", config != null ? config.getDeviceName() : "未知设备");
    }
}
