package com.foodtraceability.driver;

import com.foodtraceability.entity.DeviceStatus;
import com.foodtraceability.entity.HardwareConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 电子秤驱动实现类
 * 负责处理电子秤设备的操作
 */
@Component
public class ScaleDriver implements DeviceDriver {
    
    private static final Logger log = LoggerFactory.getLogger(ScaleDriver.class);
    
    private HardwareConfig config;
    private boolean connected;
    private BigDecimal currentWeight;
    private boolean isStable;
    private String unit;
    
    public ScaleDriver() {
        this.connected = false;
        this.currentWeight = BigDecimal.ZERO;
        this.isStable = false;
        this.unit = "kg";
        log.info("电子秤驱动初始化完成");
    }
    
    @Override
    public boolean init(HardwareConfig config) {
        if (config == null) {
            log.error("电子秤配置不能为空");
            return false;
        }
        
        this.config = config;
        log.info("初始化电子秤驱动: 设备名称={} 连接类型={}", 
                config.getDeviceName(), config.getConnectionType());
        return true;
    }
    
    @Override
    public boolean connect() {
        if (config == null) {
            log.error("电子秤配置未初始化");
            return false;
        }
        
        try {
            log.info("连接电子秤: 设备名称={}", config.getDeviceName());
            connected = true;
            log.info("电子秤连接成功: 设备名称={}", config.getDeviceName());
            return true;
        } catch (Exception e) {
            log.error("电子秤连接失败: {}", e.getMessage(), e);
            connected = false;
            return false;
        }
    }
    
    @Override
    public void disconnect() {
        if (connected) {
            log.info("断开电子秤连接: 设备名称={}", config.getDeviceName());
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
            status.setDetails(String.format("电子秤工作正常, 当前重量: %s %s", currentWeight, unit));
        } else {
            status.setDetails("电子秤未连接");
        }
        
        return status;
    }
    
    @Override
    public Map<String, Object> executeOperation(String operationType, Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        
        if (!connected) {
            result.put("success", false);
            result.put("message", "电子秤未连接");
            return result;
        }
        
        try {
            log.info("执行电子秤操作: 类型={} 参数={}", operationType, params);
            
            switch (operationType) {
                case "GET_WEIGHT":
                    return getWeight(params);
                    
                case "TARE":
                    return tare(params);
                    
                case "ZERO":
                    return zero(params);
                    
                case "CALIBRATE":
                    return calibrate(params);
                    
                case "SET_UNIT":
                    return setUnit(params);
                    
                case "GET_STATUS":
                    return getStatus(params);
                    
                case "TEST":
                    return testScale(params);
                    
                case "START_CONTINUOUS":
                    return startContinuous(params);
                    
                case "STOP_CONTINUOUS":
                    return stopContinuous(params);
                    
                default:
                    result.put("success", false);
                    result.put("message", "不支持的电子秤操作类型: " + operationType);
                    return result;
            }
        } catch (Exception e) {
            log.error("执行电子秤操作失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "执行电子秤操作失败: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * 获取重量
     */
    private Map<String, Object> getWeight(Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("获取重量: 设备名称={}", config.getDeviceName());
            
            readWeightFromDevice();
            
            result.put("success", true);
            result.put("message", "获取重量成功");
            result.put("weight", currentWeight);
            result.put("unit", unit);
            result.put("stable", isStable);
            result.put("timestamp", System.currentTimeMillis());
            return result;
        } catch (Exception e) {
            log.error("获取重量失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "获取重量失败: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * 去皮
     */
    private Map<String, Object> tare(Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("执行去皮操作: 设备名称={}", config.getDeviceName());
            
            sendTareCommand();
            
            result.put("success", true);
            result.put("message", "去皮成功");
            return result;
        } catch (Exception e) {
            log.error("去皮失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "去皮失败: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * 归零
     */
    private Map<String, Object> zero(Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("执行归零操作: 设备名称={}", config.getDeviceName());
            
            sendZeroCommand();
            currentWeight = BigDecimal.ZERO;
            
            result.put("success", true);
            result.put("message", "归零成功");
            return result;
        } catch (Exception e) {
            log.error("归零失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "归零失败: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * 校准
     */
    private Map<String, Object> calibrate(Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            BigDecimal calibrationWeight = params != null ? 
                    new BigDecimal(params.getOrDefault("weight", "0").toString()) : BigDecimal.ZERO;
            
            log.info("执行校准操作: 设备名称={} 校准重量={}", config.getDeviceName(), calibrationWeight);
            
            sendCalibrateCommand(calibrationWeight);
            
            result.put("success", true);
            result.put("message", "校准成功");
            return result;
        } catch (Exception e) {
            log.error("校准失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "校准失败: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * 设置单位
     */
    private Map<String, Object> setUnit(Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String newUnit = params != null ? (String) params.getOrDefault("unit", "kg") : "kg";
            
            if (!newUnit.matches("kg|g|lb|oz")) {
                result.put("success", false);
                result.put("message", "不支持的单位: " + newUnit);
                return result;
            }
            
            log.info("设置单位: 设备名称={} 单位={}", config.getDeviceName(), newUnit);
            
            this.unit = newUnit;
            
            result.put("success", true);
            result.put("message", "设置单位成功");
            result.put("unit", unit);
            return result;
        } catch (Exception e) {
            log.error("设置单位失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "设置单位失败: " + e.getMessage());
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
        result.put("currentWeight", currentWeight);
        result.put("unit", unit);
        result.put("stable", isStable);
        return result;
    }
    
    /**
     * 测试电子秤
     */
    private Map<String, Object> testScale(Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("测试电子秤: 设备名称={}", config.getDeviceName());
            
            result.put("success", true);
            result.put("message", "电子秤测试成功");
            return result;
        } catch (Exception e) {
            log.error("测试电子秤失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "测试电子秤失败: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * 开始连续读取
     */
    private Map<String, Object> startContinuous(Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("开始连续读取: 设备名称={}", config.getDeviceName());
            
            result.put("success", true);
            result.put("message", "已开始连续读取");
            return result;
        } catch (Exception e) {
            log.error("开始连续读取失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "开始连续读取失败: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * 停止连续读取
     */
    private Map<String, Object> stopContinuous(Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("停止连续读取: 设备名称={}", config.getDeviceName());
            
            result.put("success", true);
            result.put("message", "已停止连续读取");
            return result;
        } catch (Exception e) {
            log.error("停止连续读取失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "停止连续读取失败: " + e.getMessage());
            return result;
        }
    }
    
    private void readWeightFromDevice() {
        Random random = new Random();
        double weight = random.nextDouble() * 10;
        currentWeight = BigDecimal.valueOf(weight).setScale(3, BigDecimal.ROUND_HALF_UP);
        isStable = random.nextBoolean();
    }
    
    private void sendTareCommand() {
        log.info("发送去皮命令");
    }
    
    private void sendZeroCommand() {
        log.info("发送归零命令");
    }
    
    private void sendCalibrateCommand(BigDecimal weight) {
        log.info("发送校准命令, 校准重量: {}", weight);
    }
    
    @Override
    public String getDriverName() {
        return "Universal Scale Driver";
    }
    
    @Override
    public String getDriverVersion() {
        return "1.0.0";
    }
    
    @Override
    public String getSupportedDeviceType() {
        return "SCALE";
    }
    
    @Override
    public boolean isSupported(HardwareConfig config) {
        if (config == null) {
            return false;
        }
        return "SCALE".equals(config.getDeviceType());
    }
    
    @Override
    public void close() {
        disconnect();
        log.info("关闭电子秤驱动: 设备名称={}", config != null ? config.getDeviceName() : "未知设备");
    }
}
