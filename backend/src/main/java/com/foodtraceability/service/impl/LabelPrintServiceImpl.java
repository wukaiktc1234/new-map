package com.foodtraceability.service.impl;

import com.foodtraceability.driver.TsplLabelPrinterDriver;
import com.foodtraceability.entity.HardwareConfig;
import com.foodtraceability.service.DeviceStatusService;
import com.foodtraceability.service.LabelPrintService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 标签打印服务实现类
 * 专门用于 TSPL 标签打印机（如 Xprinter XP-D35E）
 */
@Service
public class LabelPrintServiceImpl implements LabelPrintService {

    private static final Logger log = LoggerFactory.getLogger(LabelPrintServiceImpl.class);


    public LabelPrintServiceImpl(TsplLabelPrinterDriver tsplDriver, DeviceStatusService deviceStatusService) {
        this.tsplDriver = tsplDriver;
        this.deviceStatusService = deviceStatusService;
    }

    private final TsplLabelPrinterDriver tsplDriver;

    private final DeviceStatusService deviceStatusService;

    // 打印机配置缓存
    private HardwareConfig cachedPrinterConfig;
    private long configCacheTime = 0;
    private static final long CONFIG_CACHE_DURATION = 60000; // 1分钟缓存

    @Override
    public boolean printTraceabilityLabel(String traceCode, String materialName) {
        return printTraceabilityLabel(traceCode, materialName, null, null, null, null);
    }

    @Override
    public boolean printTraceabilityLabel(String traceCode, String materialName,
                                         String batchNo, String expiryDate,
                                         String supplierName, String storeName) {
        try {
            log.info("打印追溯码标签: 追溯码={}, 物料={}", traceCode, materialName);

            // 获取打印机配置
            HardwareConfig printerConfig = getLabelPrinterConfig();
            if (printerConfig == null) {
                log.error("未找到标签打印机配置");
                return false;
            }

            // 初始化驱动
            if (!tsplDriver.isConnected()) {
                if (!tsplDriver.init(printerConfig)) {
                    log.error("初始化标签打印机驱动失败");
                    return false;
                }

                if (!tsplDriver.connect()) {
                    log.error("连接标签打印机失败");
                    return false;
                }
            }

            // 构建打印参数
            Map<String, Object> params = new HashMap<>();
            params.put("traceCode", traceCode);
            params.put("materialName", materialName);
            params.put("batchNo", batchNo != null ? batchNo : "");
            params.put("expiryDate", expiryDate != null ? expiryDate : "");
            params.put("supplierName", supplierName != null ? supplierName : "");
            params.put("storeName", storeName != null ? storeName : "");

            // 执行打印
            Map<String, Object> result = tsplDriver.executeOperation("PRINT_TRACEABILITY_LABEL", params);

            boolean success = Boolean.TRUE.equals(result.get("success"));
            if (success) {
                log.info("追溯码标签打印成功: {}", traceCode);
            } else {
                log.error("追溯码标签打印失败: {}, 原因: {}", traceCode, result.get("message"));
            }

            return success;

        } catch (Exception e) {
            log.error("打印追溯码标签异常: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean batchPrintLabels(List<Map<String, Object>> labels) {
        try {
            log.info("=== 开始批量打印标签 ===");
            log.info("标签数量: {}", labels.size());

            // 获取打印机配置
            HardwareConfig printerConfig = getLabelPrinterConfig();
            if (printerConfig == null) {
                log.error("未找到标签打印机配置");
                return false;
            }
            
            log.info("打印机配置: 设备名称={}, 设备型号={}, 连接方式={}", 
                printerConfig.getDeviceName(), 
                printerConfig.getDeviceModel(), 
                printerConfig.getConnectionType());

            // 初始化驱动
            if (!tsplDriver.isConnected()) {
                log.info("驱动未连接，开始初始化...");
                if (!tsplDriver.init(printerConfig)) {
                    log.error("初始化标签打印机驱动失败");
                    return false;
                }
                log.info("驱动初始化成功");

                // 如果之前设置了打印机名称，确保在连接前设置
                if (cachedPrinterConfig != null && cachedPrinterConfig.getDeviceName() != null) {
                    log.info("设置打印机名称: {}", cachedPrinterConfig.getDeviceName());
                    tsplDriver.setPrinterName(cachedPrinterConfig.getDeviceName());
                }

                log.info("开始连接打印机...");
                if (!tsplDriver.connect()) {
                    log.error("连接标签打印机失败");
                    return false;
                }
                log.info("打印机连接成功");
            } else {
                log.info("驱动已连接");
                // 已连接，确保使用正确的打印机名称
                if (cachedPrinterConfig != null && cachedPrinterConfig.getDeviceName() != null) {
                    log.info("更新打印机名称: {}", cachedPrinterConfig.getDeviceName());
                    tsplDriver.setPrinterName(cachedPrinterConfig.getDeviceName());
                }
            }

            // 构建批量打印参数
            Map<String, Object> params = new HashMap<>();
            params.put("labels", labels);

            log.info("执行批量打印操作...");
            // 执行批量打印
            Map<String, Object> result = tsplDriver.executeOperation("PRINT_BATCH_LABELS", params);

            boolean success = Boolean.TRUE.equals(result.get("success"));
            if (success) {
                log.info("=== 批量打印标签成功 ===");
            } else {
                log.error("批量打印标签失败: {}", result.get("message"));
            }

            return success;

        } catch (Exception e) {
            log.error("批量打印标签异常: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean testPrint() {
        try {
            log.info("执行测试打印");

            // 获取打印机配置
            HardwareConfig printerConfig = getLabelPrinterConfig();
            if (printerConfig == null) {
                log.error("未找到标签打印机配置");
                return false;
            }

            // 初始化驱动
            if (!tsplDriver.init(printerConfig)) {
                log.error("初始化标签打印机驱动失败");
                return false;
            }

            if (!tsplDriver.connect()) {
                log.error("连接标签打印机失败");
                return false;
            }

            // 执行测试打印
            Map<String, Object> result = tsplDriver.executeOperation("TEST_PRINT", null);

            boolean success = Boolean.TRUE.equals(result.get("success"));
            if (success) {
                log.info("测试打印成功");
            } else {
                log.error("测试打印失败: {}", result.get("message"));
            }

            return success;

        } catch (Exception e) {
            log.error("测试打印异常: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean setPrinterConfig(int labelWidth, int labelHeight, int gapSize, int speed, int density) {
        try {
            log.info("设置打印机配置: 宽度={}mm, 高度={}mm, 间隙={}mm, 速度={}, 浓度={}",
                    labelWidth, labelHeight, gapSize, speed, density);

            // 获取打印机配置
            HardwareConfig printerConfig = getLabelPrinterConfig();
            if (printerConfig == null) {
                log.error("未找到标签打印机配置");
                return false;
            }

            // 初始化驱动
            if (!tsplDriver.isConnected()) {
                if (!tsplDriver.init(printerConfig)) {
                    log.error("初始化标签打印机驱动失败");
                    return false;
                }

                if (!tsplDriver.connect()) {
                    log.error("连接标签打印机失败");
                    return false;
                }
            }

            // 构建配置参数
            Map<String, Object> params = new HashMap<>();
            params.put("labelWidth", labelWidth);
            params.put("labelHeight", labelHeight);
            params.put("gapSize", gapSize);
            params.put("speed", speed);
            params.put("density", density);

            // 执行配置
            Map<String, Object> result = tsplDriver.executeOperation("SET_CONFIG", params);

            boolean success = Boolean.TRUE.equals(result.get("success"));
            if (success) {
                log.info("打印机配置设置成功");
            } else {
                log.error("打印机配置设置失败: {}", result.get("message"));
            }

            return success;

        } catch (Exception e) {
            log.error("设置打印机配置异常: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getPrinterStatus() {
        try {
            // 获取打印机配置
            HardwareConfig printerConfig = getLabelPrinterConfig();
            if (printerConfig == null) {
                Map<String, Object> result = new HashMap<>();
                result.put("connected", false);
                result.put("message", "未找到标签打印机配置");
                return result;
            }

            // 初始化驱动
            if (!tsplDriver.isConnected()) {
                tsplDriver.init(printerConfig);
            }

            // 获取状态
            Map<String, Object> result = tsplDriver.executeOperation("GET_STATUS", null);
            result.put("printerName", printerConfig.getDeviceName());
            result.put("printerModel", printerConfig.getDeviceModel());
            result.put("connectionType", printerConfig.getConnectionType());

            return result;

        } catch (Exception e) {
            log.error("获取打印机状态异常: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("connected", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    @Override
    public boolean printCustomLabel(String tsplData) {
        try {
            log.info("打印自定义标签, TSPL 指令长度: {}", tsplData.length());

            // 获取打印机配置
            HardwareConfig printerConfig = getLabelPrinterConfig();
            if (printerConfig == null) {
                log.error("未找到标签打印机配置");
                return false;
            }

            // 初始化驱动
            if (!tsplDriver.isConnected()) {
                if (!tsplDriver.init(printerConfig)) {
                    log.error("初始化标签打印机驱动失败");
                    return false;
                }

                if (!tsplDriver.connect()) {
                    log.error("连接标签打印机失败");
                    return false;
                }
            }

            // 构建打印参数
            Map<String, Object> params = new HashMap<>();
            params.put("tsplData", tsplData);

            // 执行打印
            Map<String, Object> result = tsplDriver.executeOperation("PRINT_LABEL", params);

            boolean success = Boolean.TRUE.equals(result.get("success"));
            if (success) {
                log.info("自定义标签打印成功");
            } else {
                log.error("自定义标签打印失败: {}", result.get("message"));
            }

            return success;

        } catch (Exception e) {
            log.error("打印自定义标签异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取标签打印机配置
     * 优先从数据库获取，使用缓存机制
     */
    private HardwareConfig getLabelPrinterConfig() {
        // 检查缓存
        if (cachedPrinterConfig != null &&
            System.currentTimeMillis() - configCacheTime < CONFIG_CACHE_DURATION) {
            return cachedPrinterConfig;
        }

        try {
            // 尝试从数据库获取标签打印机配置
            HardwareConfig config = deviceStatusService.getHardwareConfig("LABEL_PRINTER");

            if (config == null) {
                // 尝试获取通用打印机配置
                config = deviceStatusService.getHardwareConfig("PRINTER");
            }

            if (config == null) {
                // 使用默认配置
                config = createDefaultLabelPrinterConfig();
                log.warn("未找到标签打印机配置，使用默认配置");
            }

            // 更新缓存
            cachedPrinterConfig = config;
            configCacheTime = System.currentTimeMillis();

            return config;

        } catch (Exception e) {
            log.error("获取标签打印机配置失败: {}", e.getMessage(), e);
            return createDefaultLabelPrinterConfig();
        }
    }

    /**
     * 创建默认的标签打印机配置
     */
    private HardwareConfig createDefaultLabelPrinterConfig() {
        HardwareConfig config = new HardwareConfig();
        config.setDeviceType("LABEL_PRINTER");
        config.setDeviceName("Xprinter XP-D35E");
        config.setDeviceModel("XP-D35E");
        config.setConnectionType("WINDOWS"); // 使用 Windows 打印机方式
        config.setIpAddress(""); // USB 连接不需要 IP
        config.setPort(null);
        config.setStatus(1); // 1=ONLINE

        // 设置默认打印参数
        Map<String, Object> printParams = new HashMap<>();
        printParams.put("labelWidth", 40);
        printParams.put("labelHeight", 30);
        printParams.put("gapSize", 2);
        printParams.put("speed", 4);
        printParams.put("density", 10);

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            config.setConfigJson(mapper.writeValueAsString(printParams));
        } catch (Exception e) {
            log.warn("序列化默认配置失败: {}", e.getMessage());
        }

        return config;
    }

    /**
     * 清除配置缓存
     */
    public void clearConfigCache() {
        cachedPrinterConfig = null;
        configCacheTime = 0;
        log.info("标签打印机配置缓存已清除");
    }

    @Override
    public void setPrinterName(String printerName) {
        log.info("设置标签打印机名称: {}", printerName);
        
        // 清除缓存，以便下次获取配置时重新加载
        clearConfigCache();
        
        // 更新当前配置的打印机名称
        HardwareConfig config = getLabelPrinterConfig();
        if (config != null) {
            config.setDeviceName(printerName);
            // 更新缓存
            cachedPrinterConfig = config;
            configCacheTime = System.currentTimeMillis();
        }
        
        // 同时更新驱动中的打印机名称
        tsplDriver.setPrinterName(printerName);
    }
}
