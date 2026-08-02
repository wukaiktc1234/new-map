package com.foodtraceability.driver;

import com.foodtraceability.entity.DeviceStatus;
import com.foodtraceability.entity.HardwareConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TSPL 标签打印机驱动
 * 专门用于 Xprinter XP-D35E 等支持 TSPL 指令的标签打印机
 *
 * 支持的通信方式：
 * 1. USB 虚拟串口（推荐）
 * 2. 原生 USB（需要厂商 SDK）
 * 3. 网络打印（TCP Socket）
 *
 * TSPL 指令集说明：
 * - SIZE: 设置标签尺寸
 * - GAP: 设置标签间隙
 * - CLS: 清除缓冲区
 * - TEXT: 打印文本
 * - BARCODE: 打印条形码
 * - QRCODE: 打印二维码
 * - PRINT: 执行打印
 */
@Component
public class TsplLabelPrinterDriver implements DeviceDriver {

    private static final Logger log = LoggerFactory.getLogger(TsplLabelPrinterDriver.class);

    // 字符编码
    private static final Charset GBK_CHARSET = Charset.forName("GBK");

    // 设备配置
    private HardwareConfig config;

    // 连接状态
    private volatile boolean connected = false;

    // 输出流（用于发送指令）
    private OutputStream outputStream;

    // 打印机状态缓存
    private final Map<String, Object> printerStatus = new ConcurrentHashMap<>();

    // 支持的打印机型号
    private static final List<String> SUPPORTED_MODELS = List.of(
        "XPRINTER", "XP-D35", "XP-D35E", "GODEX", "TSC", "ZEBRA", "POSTEK"
    );

    // 默认打印配置 - 300 DPI
    private static final int DEFAULT_LABEL_WIDTH_MM = 40;
    private static final int DEFAULT_LABEL_HEIGHT_MM = 30;
    private static final int DEFAULT_GAP_MM = 2;
    private static final int DEFAULT_SPEED = 4;
    private static final int DEFAULT_DENSITY = 12;
    
    // 300 DPI 点数计算
    private static final int DPI = 300;
    private static final int DEFAULT_LABEL_WIDTH_DOTS = 472;   // 40mm @ 300 DPI
    private static final int DEFAULT_LABEL_HEIGHT_DOTS = 354;  // 30mm @ 300 DPI
    private static final int DEFAULT_GAP_DOTS = 24;            // 2mm @ 300 DPI
    
    // 当前打印机名称
    private String currentPrinterName;

    /**
     * 设置打印机名称
     */
    public void setPrinterName(String printerName) {
        this.currentPrinterName = printerName;
        if (config != null) {
            config.setDeviceName(printerName);
        }
        log.info("设置打印机名称: {}", printerName);
    }

    @Override
    public boolean init(HardwareConfig config) {
        if (config == null) {
            log.error("打印机配置不能为空");
            return false;
        }

        this.config = config;
        log.info("初始化 TSPL 标签打印机驱动: 设备名称={}, 型号={}, 连接方式={}",
                config.getDeviceName(), config.getDeviceModel(), getConnectionType());

        // 初始化打印机状态
        printerStatus.put("initialized", true);
        printerStatus.put("lastInitTime", System.currentTimeMillis());

        return true;
    }

    @Override
    public boolean connect() {
        if (config == null) {
            log.error("打印机配置未初始化");
            return false;
        }

        try {
            String connectionType = getConnectionType();
            log.info("连接 TSPL 打印机: 设备名称={}, 连接方式={}", config.getDeviceName(), connectionType);

            switch (connectionType.toUpperCase()) {
                case "WINDOWS":
                case "PRINTER":
                    return connectViaWindowsPrinter();

                case "USB":
                case "SERIAL":
                    // 先尝试 Windows 打印机方式，失败后再尝试串口
                    if (connectViaWindowsPrinter()) {
                        return true;
                    }
                    return connectViaSerial();

                case "NETWORK":
                case "TCP":
                    return connectViaNetwork();

                default:
                    // 默认使用 Windows 打印机方式
                    log.info("使用 Windows 打印机方式连接");
                    return connectViaWindowsPrinter();
            }
        } catch (Exception e) {
            log.error("连接打印机失败: {}", e.getMessage(), e);
            connected = false;
            return false;
        }
    }

    /**
     * 通过串口连接（USB虚拟串口）
     */
    private boolean connectViaSerial() {
        try {
            // Windows 系统下，USB 打印机通常映射为虚拟串口
            // 需要在设备管理器中查看实际的 COM 端口号
            String portName = config.getIpAddress(); // 这里实际存储的是 COM 端口号

            if (portName == null || portName.isEmpty()) {
                // 尝试自动检测
                portName = detectPrinterPort();
                if (portName == null) {
                    log.error("无法检测到打印机端口，请手动配置");
                    return false;
                }
            }

            log.info("尝试连接串口: {}", portName);

            // TODO: 引入 jSerialComm 等串口库实现真实串口连接，当前使用 Windows 打印机端口作为替代
            connected = connectViaWindowsPrinter();
            return connected;

        } catch (Exception e) {
            log.error("串口连接失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 通过 Windows 打印机连接（临时方案）
     * 使用 Java PrintService 发送原始数据
     */
    private boolean connectViaWindowsPrinter() {
        try {
            javax.print.PrintService[] services =
                javax.print.PrintServiceLookup.lookupPrintServices(null, null);

            log.info("=== 开始检测 Windows 打印机 ===");
            log.info("系统中共有 {} 台打印机", services.length);
            for (javax.print.PrintService service : services) {
                log.info("  - 打印机: {}", service.getName());
            }

            // 优先使用通过 setPrinterName 设置的打印机名称
            String printerName = currentPrinterName;
            
            // 如果没有设置，则尝试从配置获取
            if (printerName == null || printerName.isEmpty()) {
                printerName = config.getDeviceName();
                log.info("从配置获取打印机名称: {}", printerName);
            } else {
                log.info("使用设置的打印机名称: {}", printerName);
            }
            
            if (printerName == null || printerName.isEmpty()) {
                // 尝试查找标签打印机
                log.info("尝试自动检测标签打印机...");
                for (javax.print.PrintService service : services) {
                    String name = service.getName().toUpperCase();
                    log.info("  检测打印机: {}", name);
                    if (name.contains("XPRINTER") || name.contains("XP-D35") ||
                        name.contains("LABEL") || name.contains("标签")) {
                        printerName = service.getName();
                        log.info(">>> 自动检测到标签打印机: {}", printerName);
                        break;
                    }
                }
            }

            if (printerName != null) {
                // 验证打印机是否存在
                boolean found = false;
                for (javax.print.PrintService service : services) {
                    if (service.getName().equals(printerName)) {
                        found = true;
                        break;
                    }
                }
                
                if (!found) {
                    log.warn("指定的打印机不存在: {}, 尝试模糊匹配...", printerName);
                    // 尝试模糊匹配
                    String searchName = printerName.toUpperCase();
                    for (javax.print.PrintService service : services) {
                        if (service.getName().toUpperCase().contains(searchName) ||
                            searchName.contains(service.getName().toUpperCase())) {
                            printerName = service.getName();
                            log.info(">>> 模糊匹配到打印机: {}", printerName);
                            found = true;
                            break;
                        }
                    }
                }
                
                if (found || printerName != null) {
                    printerStatus.put("printerName", printerName);
                    connected = true;
                    log.info("=== Windows 打印机连接成功: {} ===", printerName);
                    return true;
                }
            }

            log.error("=== 未找到可用的标签打印机 ===");
            return false;

        } catch (Exception e) {
            log.error("Windows 打印机连接失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 通过网络连接
     */
    private boolean connectViaNetwork() {
        try {
            String ipAddress = config.getIpAddress();
            String portStr = config.getPort();
            int port = 9100; // 默认打印端口

            if (ipAddress == null || ipAddress.isEmpty()) {
                log.error("网络打印机 IP 地址不能为空");
                return false;
            }

            if (portStr != null && !portStr.isEmpty()) {
                try {
                    port = Integer.parseInt(portStr);
                } catch (NumberFormatException e) {
                    log.warn("端口号格式错误，使用默认端口: {}", portStr);
                }
            }

            log.info("连接网络打印机: {}:{}", ipAddress, port);

            // 创建 Socket 连接
            java.net.Socket socket = new java.net.Socket();
            socket.connect(new java.net.InetSocketAddress(ipAddress, port), 5000);
            socket.setSoTimeout(10000);

            outputStream = socket.getOutputStream();
            connected = true;

            log.info("网络打印机连接成功: {}:{}", ipAddress, port);

            // 发送初始化指令
            initializePrinter();

            return true;

        } catch (Exception e) {
            log.error("网络打印机连接失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 自动检测打印机端口
     */
    private String detectPrinterPort() {
        // Windows 系统下检测 USB 打印机端口
        String os = System.getProperty("os.name", "").toLowerCase();

        if (os.contains("win")) {
            // 尝试常见的 USB 虚拟串口
            String[] possiblePorts = {"COM1", "COM2", "COM3", "COM4", "COM5",
                                     "COM6", "COM7", "COM8", "COM9", "COM10"};

            for (String port : possiblePorts) {
                // 实际应该尝试打开端口并查询设备
                log.debug("检测端口: {}", port);
            }
        }

        return null;
    }

    /**
     * 初始化打印机
     */
    private void initializePrinter() throws IOException {
        log.info("发送打印机初始化指令 (300 DPI)");

        // 发送 TSPL 初始化指令 - 使用点数单位（300 DPI）
        StringBuilder initCmd = new StringBuilder();
        initCmd.append("SIZE ").append(DEFAULT_LABEL_WIDTH_DOTS).append(",").append(DEFAULT_LABEL_HEIGHT_DOTS).append("\n");
        initCmd.append("GAP ").append(DEFAULT_GAP_DOTS).append(",0\n");
        initCmd.append("SPEED ").append(DEFAULT_SPEED).append("\n");
        initCmd.append("DENSITY ").append(DEFAULT_DENSITY).append("\n");
        initCmd.append("DIRECTION 1\n");
        initCmd.append("CLS\n");

        sendCommand(initCmd.toString());
    }

    @Override
    public void disconnect() {
        if (connected) {
            try {
                if (outputStream != null) {
                    outputStream.close();
                    outputStream = null;
                }
            } catch (IOException e) {
                log.warn("关闭输出流失败: {}", e.getMessage());
            }

            connected = false;
            log.info("断开 TSPL 打印机连接: {}", config.getDeviceName());
        }
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public DeviceStatus getDeviceStatus() {
        DeviceStatus status = new DeviceStatus();
        status.setDeviceType(config != null ? config.getDeviceType() : "PRINTER");
        status.setOnline(connected);
        status.setIpAddress(config != null ? config.getIpAddress() : null);
        status.setPort(config != null ? config.getPort() : null);
        status.setDeviceModel(config != null ? config.getDeviceModel() : null);

        if (connected) {
            status.setDetails("TSPL 标签打印机已连接");
        } else {
            status.setDetails("TSPL 标签打印机未连接");
        }

        return status;
    }

    @Override
    public Map<String, Object> executeOperation(String operationType, Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();

        if (!connected && !"CONNECT".equals(operationType)) {
            result.put("success", false);
            result.put("message", "打印机未连接");
            return result;
        }

        try {
            switch (operationType) {
                case "PRINT_LABEL":
                    return printLabel(params);

                case "PRINT_TRACEABILITY_LABEL":
                    return printTraceabilityLabel(params);

                case "PRINT_BATCH_LABELS":
                    return printBatchLabels(params);

                case "TEST_PRINT":
                    return testPrint();

                case "SET_CONFIG":
                    return setPrinterConfig(params);

                case "GET_STATUS":
                    return getPrinterStatus();

                case "CONNECT":
                    boolean success = connect();
                    result.put("success", success);
                    result.put("message", success ? "连接成功" : "连接失败");
                    return result;

                default:
                    result.put("success", false);
                    result.put("message", "不支持的操作类型: " + operationType);
                    return result;
            }
        } catch (Exception e) {
            log.error("执行操作失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "操作失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 打印标签
     */
    private Map<String, Object> printLabel(Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();

        try {
            Object tsplDataObj = params.get("tsplData");
            String tsplData = tsplDataObj != null ? tsplDataObj.toString() : null;

            if (tsplData == null || tsplData.isEmpty()) {
                // 如果没有提供 TSPL 数据，则根据参数生成
                tsplData = generateTsplFromParams(params);
            }

            log.info("打印标签, TSPL 指令长度: {}", tsplData != null ? tsplData.length() : 0);

            // 发送打印指令
            boolean success = sendCommand(tsplData);

            result.put("success", success);
            result.put("message", success ? "标签打印成功" : "标签打印失败");
            return result;

        } catch (Exception e) {
            log.error("打印标签失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "打印失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 打印追溯码标签
     */
    private Map<String, Object> printTraceabilityLabel(Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();

        try {
            log.info("打印标签参数: {}", params);
            
            String traceCode = getStringValue(params, "traceCode");
            String materialName = getStringValue(params, "materialName");
            String storeName = getStringValue(params, "storeName");
            String shelfLifeDays = getStringValue(params, "shelfLifeDays");
            String generateTime = getStringValue(params, "generateTime");
            String expiryDate = getStringValue(params, "expiryDate");
            String supplierName = getStringValue(params, "supplierName");

            if (traceCode == null || traceCode.isEmpty()) {
                result.put("success", false);
                result.put("message", "追溯码不能为空");
                return result;
            }

            // 生成 TSPL 指令
            String tsplData = generateTraceabilityLabelTspl(
                traceCode, materialName, storeName, shelfLifeDays, 
                generateTime, expiryDate, supplierName
            );

            log.info("打印追溯码标签: {}", traceCode);
            log.info("TSPL 指令:\n{}", tsplData);

            // 发送打印指令
            boolean success = sendCommand(tsplData);

            result.put("success", success);
            result.put("message", success ? "追溯码标签打印成功" : "追溯码标签打印失败");
            return result;

        } catch (Exception e) {
            log.error("打印追溯码标签失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "打印失败: " + e.getMessage());
            return result;
        }
    }
    
    private String getStringValue(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null) return "";
        return value.toString();
    }

    /**
     * 批量打印标签
     */
    private Map<String, Object> printBatchLabels(Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();

        try {
            Object labelsObj = params.get("labels");
            
            if (labelsObj == null) {
                result.put("success", false);
                result.put("message", "标签列表不能为空");
                return result;
            }
            
            if (!(labelsObj instanceof List)) {
                result.put("success", false);
                result.put("message", "标签列表格式错误");
                return result;
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> labels = (List<Map<String, Object>>) labelsObj;

            if (labels.isEmpty()) {
                result.put("success", false);
                result.put("message", "标签列表不能为空");
                return result;
            }

            int successCount = 0;
            int failCount = 0;

            for (Map<String, Object> label : labels) {
                Map<String, Object> printResult = printTraceabilityLabel(label);
                if (Boolean.TRUE.equals(printResult.get("success"))) {
                    successCount++;
                } else {
                    failCount++;
                }

                // 打印间隔，避免打印机缓冲区溢出
                Thread.sleep(100);
            }

            result.put("success", failCount == 0);
            result.put("message", String.format("批量打印完成: 成功 %d, 失败 %d", successCount, failCount));
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            return result;

        } catch (Exception e) {
            log.error("批量打印失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "批量打印失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 测试打印
     */
    private Map<String, Object> testPrint() {
        Map<String, Object> result = new HashMap<>();

        try {
            String tsplData = generateTestLabelTspl();

            log.info("执行测试打印");

            boolean success = sendCommand(tsplData);

            result.put("success", success);
            result.put("message", success ? "测试打印成功" : "测试打印失败");
            return result;

        } catch (Exception e) {
            log.error("测试打印失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "测试打印失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 设置打印机配置
     */
    private Map<String, Object> setPrinterConfig(Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();

        try {
            StringBuilder configCmd = new StringBuilder();

            // 标签尺寸
            if (params.containsKey("labelWidth") && params.containsKey("labelHeight")) {
                configCmd.append("SIZE ").append(params.get("labelWidth")).append(" mm, ")
                        .append(params.get("labelHeight")).append(" mm\n");
            }

            // 标签间隙
            if (params.containsKey("gapSize")) {
                configCmd.append("GAP ").append(params.get("gapSize")).append(" mm, 0 mm\n");
            }

            // 打印速度
            if (params.containsKey("speed")) {
                configCmd.append("SPEED ").append(params.get("speed")).append("\n");
            }

            // 打印浓度
            if (params.containsKey("density")) {
                configCmd.append("DENSITY ").append(params.get("density")).append("\n");
            }

            if (configCmd.length() > 0) {
                boolean success = sendCommand(configCmd.toString());
                result.put("success", success);
                result.put("message", success ? "配置设置成功" : "配置设置失败");
            } else {
                result.put("success", false);
                result.put("message", "未提供有效的配置参数");
            }

            return result;

        } catch (Exception e) {
            log.error("设置打印机配置失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "设置失败: " + e.getMessage());
            return result;
        }
    }

    /**
     * 获取打印机状态
     */
    private Map<String, Object> getPrinterStatus() {
        Map<String, Object> result = new HashMap<>();
        result.put("connected", connected);
        result.put("printerName", printerStatus.get("printerName"));
        result.put("initialized", printerStatus.get("initialized"));
        result.put("lastInitTime", printerStatus.get("lastInitTime"));
        result.put("success", true);
        return result;
    }

    /**
     * 发送 TSPL 指令
     */
    private boolean sendCommand(String tsplData) {
        try {
            log.info("=== sendCommand 开始 ===");
            log.info("outputStream 是否为 null: {}", outputStream == null);
            log.info("connected 状态: {}", connected);
            
            if (outputStream != null) {
                // 直接发送到输出流
                log.info("使用输出流发送 TSPL 指令");
                outputStream.write(tsplData.getBytes(GBK_CHARSET));
                outputStream.flush();
                log.info("TSPL 指令已发送到输出流");
                return true;
            }

            // 使用 Windows 打印机
            log.info("使用 Windows 打印机发送 TSPL 指令");
            boolean result = sendToWindowsPrinter(tsplData);
            log.info("sendToWindowsPrinter 返回结果: {}", result);
            return result;

        } catch (Exception e) {
            log.error("发送 TSPL 指令失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 通过 Windows 打印机发送原始数据
     * 使用 RAW 模式发送 TSPL 指令
     * 
     * 注意：Windows 打印机驱动可能不支持直接发送 TSPL 指令
     * 如果打印机不支持 RAW 模式，需要使用厂商提供的驱动或 SDK
     */
    private boolean sendToWindowsPrinter(String tsplData) {
        try {
            Object printerNameObj = printerStatus.get("printerName");
            String printerName = printerNameObj != null ? printerNameObj.toString() : null;
            
            log.info("=== 开始发送 TSPL 数据到 Windows 打印机 ===");
            log.info("目标打印机: {}", printerName);
            log.info("printerStatus 内容: {}", printerStatus);
            
            if (printerName == null || printerName.isEmpty()) {
                log.error("未配置打印机名称，尝试自动检测...");
                
                // 自动检测 Xprinter 打印机
                javax.print.PrintService[] services =
                    javax.print.PrintServiceLookup.lookupPrintServices(null, null);
                
                for (javax.print.PrintService service : services) {
                    String name = service.getName().toUpperCase();
                    log.info("检测打印机: {}", service.getName());
                    if (name.contains("XPRINTER") || name.contains("XP-D35") || name.contains("LABEL")) {
                        printerName = service.getName();
                        log.info(">>> 自动检测到标签打印机: {}", printerName);
                        printerStatus.put("printerName", printerName);
                        break;
                    }
                }
                
                if (printerName == null) {
                    log.error("未找到可用的标签打印机");
                    return false;
                }
            }

            // 查找打印机
            javax.print.PrintService[] services =
                javax.print.PrintServiceLookup.lookupPrintServices(null, null);

            javax.print.PrintService targetPrinter = null;
            for (javax.print.PrintService service : services) {
                if (service.getName().equals(printerName)) {
                    targetPrinter = service;
                    break;
                }
            }

            if (targetPrinter == null) {
                log.error("未找到打印机: {}", printerName);
                log.info("可用打印机列表:");
                for (javax.print.PrintService service : services) {
                    log.info("  - {}", service.getName());
                }
                return false;
            }

            log.info("找到目标打印机: {}", targetPrinter.getName());

            // 检查打印机支持的 DocFlavor
            javax.print.DocFlavor[] supportedFlavors = targetPrinter.getSupportedDocFlavors();
            log.info("打印机支持的 DocFlavor 数量: {}", supportedFlavors.length);
            
            // 优先查找 RAW 格式支持
            javax.print.DocFlavor selectedFlavor = null;
            boolean supportsRaw = false;
            
            for (javax.print.DocFlavor flavor : supportedFlavors) {
                log.debug("  支持的格式: {}", flavor);
                String mimeType = flavor.getMimeType();
                if (mimeType != null && (mimeType.contains("raw") || mimeType.contains("application/octet-stream"))) {
                    selectedFlavor = flavor;
                    supportsRaw = true;
                    log.info("找到 RAW 格式支持: {}", flavor);
                    break;
                }
            }
            
            // 如果没有找到 RAW 格式，尝试其他格式
            if (selectedFlavor == null) {
                // 尝试 AUTOSENSE
                for (javax.print.DocFlavor flavor : supportedFlavors) {
                    if (flavor.equals(javax.print.DocFlavor.BYTE_ARRAY.AUTOSENSE)) {
                        selectedFlavor = flavor;
                        log.info("使用 AUTOSENSE 格式");
                        break;
                    }
                }
            }
            
            // 最后使用默认格式
            if (selectedFlavor == null) {
                selectedFlavor = javax.print.DocFlavor.BYTE_ARRAY.AUTOSENSE;
                log.warn("打印机可能不支持 RAW 模式，使用 AUTOSENSE 格式（可能无法正确打印）");
            }

            // 创建打印任务
            javax.print.DocPrintJob job = targetPrinter.createPrintJob();
            log.info("创建打印任务成功");

            byte[] printData = tsplData.getBytes(GBK_CHARSET);
            log.info("TSPL 数据长度: {} 字节", printData.length);
            log.info("TSPL 内容:\n{}", tsplData);

            javax.print.Doc doc = new javax.print.SimpleDoc(printData, selectedFlavor, null);

            // 设置打印属性
            javax.print.attribute.HashPrintRequestAttributeSet attrs = 
                new javax.print.attribute.HashPrintRequestAttributeSet();
            
            // 使用默认 Locale，避免 null 导致的 ClassCastException
            try {
                attrs.add(new javax.print.attribute.standard.JobName("Label Print", java.util.Locale.getDefault()));
                attrs.add(new javax.print.attribute.standard.DocumentName("Label", java.util.Locale.getDefault()));
            } catch (Exception attrEx) {
                log.warn("设置打印属性失败: {}", attrEx.getMessage());
            }
            
            // 执行打印
            log.info("开始发送打印数据...");
            job.print(doc, null);
            log.info("=== TSPL 指令已成功发送到 Windows 打印机: {} ===", printerName);
            
            // 如果不支持 RAW 模式，记录警告
            if (!supportsRaw) {
                log.warn("========================================");
                log.warn("警告：打印机可能不支持 RAW 模式！");
                log.warn("如果打印内容不正确，请尝试以下方案：");
                log.warn("1. 安装 Xprinter 官方驱动程序");
                log.warn("2. 使用 USB 虚拟串口模式");
                log.warn("3. 使用厂商提供的 SDK");
                log.warn("========================================");
            }
            
            // 等待打印任务完成
            Thread.sleep(500);
            
            return true;

        } catch (Exception e) {
            log.error("发送到 Windows 打印机失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 生成追溯码标签的 TSPL 指令
     * 标签尺寸: 40mm x 30mm = 472 x 354 点 (300 DPI)
     * 
     * 布局设计（简化版）：
     * ┌─────────────────────────────┐
     * │ 物料名称          [二维码] │  ← 第1行：物料名称 + 二维码
     * │ 门店:XXX                    │  ← 第2行
     * │ 保质:XX天  入库:XXXX-XX-XX  │  ← 第3行：两列布局
     * │ 到期:XXXX-XX-XX  供应:XXX   │  ← 第4行：两列布局
     * │ 追溯码:XXXXXXXXXXXX         │  ← 第5行：追溯码
     * └─────────────────────────────┘
     */
    private String generateTraceabilityLabelTspl(
            String traceCode, String materialName, String storeName,
            String shelfLifeDays, String generateTime, String expiryDate, String supplierName) {

        StringBuilder tspl = new StringBuilder();

        // 打印机设置
        final int LABEL_WIDTH = 472;   // 40mm @ 300 DPI
        final int LABEL_HEIGHT = 354;  // 30mm @ 300 DPI
        
        tspl.append("SIZE ").append(LABEL_WIDTH).append(",").append(LABEL_HEIGHT).append("\n");
        tspl.append("GAP 24,0\n");
        tspl.append("SPEED 3\n");
        tspl.append("DENSITY 13\n");
        tspl.append("DIRECTION 1\n");
        tspl.append("CLS\n");

        // 中文字体
        String font = "TSS24.BF2";
        
        // 边距
        int margin = 15;
        
        // 二维码参数 - 右上角，确保完全在标签内
        int qrSize = 65;
        int qrX = LABEL_WIDTH - qrSize - margin;  // 右边距
        int qrY = margin;                          // 顶部边距
        
        // 文字区域左边界
        int textX = margin;
        int textY = margin;
        
        // 文字区域宽度（二维码左侧）
        int textWidth = qrX - margin - 10;  // 留出与二维码的间距

        // 第1行：物料名称（大字体）
        if (materialName != null && !materialName.isEmpty()) {
            String name = truncateText(materialName, 4);
            tspl.append("TEXT ").append(textX).append(",").append(textY)
                .append(",\"").append(font).append("\",0,2,2,\"").append(escapeTspl(name)).append("\"\n");
        }
        textY += 40;

        // 第2行：门店
        if (storeName != null && !storeName.isEmpty()) {
            String store = truncateText(storeName, 5);
            tspl.append("TEXT ").append(textX).append(",").append(textY)
                .append(",\"").append(font).append("\",0,1,1,\"门店:").append(escapeTspl(store)).append("\"\n");
        }
        textY += 28;

        // 第3行：保质期 + 入库日期（两列）
        int col1X = textX;
        int col2X = textX + 140;
        
        if (shelfLifeDays != null && !shelfLifeDays.isEmpty()) {
            tspl.append("TEXT ").append(col1X).append(",").append(textY)
                .append(",\"").append(font).append("\",0,1,1,\"保质:").append(shelfLifeDays).append("天\"\n");
        }
        if (generateTime != null && !generateTime.isEmpty()) {
            String time = generateTime.length() > 10 ? generateTime.substring(0, 10) : generateTime;
            tspl.append("TEXT ").append(col2X).append(",").append(textY)
                .append(",\"").append(font).append("\",0,1,1,\"入库:").append(time).append("\"\n");
        }
        textY += 28;

        // 第4行：到期日期 + 供应商（两列）
        if (expiryDate != null && !expiryDate.isEmpty()) {
            String exp = expiryDate.length() > 10 ? expiryDate.substring(0, 10) : expiryDate;
            tspl.append("TEXT ").append(col1X).append(",").append(textY)
                .append(",\"").append(font).append("\",0,1,1,\"到期:").append(exp).append("\"\n");
        }
        if (supplierName != null && !supplierName.isEmpty()) {
            String supplier = truncateText(supplierName, 4);
            tspl.append("TEXT ").append(col2X).append(",").append(textY)
                .append(",\"").append(font).append("\",0,1,1,\"供应:").append(escapeTspl(supplier)).append("\"\n");
        }
        textY += 28;

        // 二维码 - 右上角
        String escapedTraceCode = escapeTspl(traceCode);
        tspl.append("QRCODE ").append(qrX).append(",").append(qrY)
            .append(",H,4,A,0,M2,S3,\"").append(escapedTraceCode).append("\"\n");

        // 第5行：追溯码 - 底部
        int codeY = LABEL_HEIGHT - margin - 24;
        tspl.append("TEXT ").append(textX).append(",").append(codeY)
            .append(",\"").append(font).append("\",0,1,1,\"码:").append(escapedTraceCode).append("\"\n");

        // 执行打印
        tspl.append("PRINT 1\n");

        log.info("生成 TSPL 标签: {}x{}, 二维码位置: ({}, {})", 
                 LABEL_WIDTH, LABEL_HEIGHT, qrX, qrY);
        
        return tspl.toString();
    }
    
    /**
     * 截断文本
     */
    private String truncateText(String text, int maxChars) {
        if (text == null) return "";
        if (text.length() <= maxChars) return text;
        return text.substring(0, maxChars);
    }

    /**
     * 生成测试标签的 TSPL 指令
     * 使用 300 DPI 设置和中文字体
     */
    private String generateTestLabelTspl() {
        StringBuilder tspl = new StringBuilder();

        // 300 DPI: 40mm = 472 点, 30mm = 354 点
        tspl.append("SIZE 472,354\n");
        tspl.append("GAP 24,0\n");
        tspl.append("DIRECTION 1\n");
        tspl.append("DENSITY 12\n");
        tspl.append("CLS\n");

        // 使用中文字体 TSS24.BF2
        String chineseFont = "TSS24.BF2";

        tspl.append("TEXT 20,20,\"").append(chineseFont).append("\",0,1,1,\"打印机测试\"\n");
        tspl.append("TEXT 20,50,\"").append(chineseFont).append("\",0,1,1,\"Xprinter XP-D35E\"\n");
        tspl.append("TEXT 20,80,\"").append(chineseFont).append("\",0,1,1,\"300 DPI 中文字体\"\n");
        tspl.append("TEXT 20,110,\"").append(chineseFont).append("\",0,1,1,\"时间: ")
            .append(java.time.LocalDateTime.now().toString()).append("\"\n");

        tspl.append("BARCODE 20,150,\"128\",50,1,0,2,2,\"TEST001\"\n");

        // 二维码位置调整到右侧
        tspl.append("QRCODE 380,20,H,5,A,0,M2,S3,\"TEST-QR-CODE\"\n");

        tspl.append("PRINT 1,1\n");

        return tspl.toString();
    }

    /**
     * 从参数生成 TSPL 指令
     */
    private String generateTsplFromParams(Map<String, Object> params) {
        StringBuilder tspl = new StringBuilder();

        // 标签设置 - 安全获取整数值
        int width = getIntValue(params, "labelWidth", DEFAULT_LABEL_WIDTH_MM);
        int height = getIntValue(params, "labelHeight", DEFAULT_LABEL_HEIGHT_MM);
        int gap = getIntValue(params, "gapSize", DEFAULT_GAP_MM);

        tspl.append("SIZE ").append(width).append(" mm, ").append(height).append(" mm\n");
        tspl.append("GAP ").append(gap).append(" mm, 0 mm\n");
        tspl.append("DIRECTION 1\n");
        tspl.append("CLS\n");

        // 添加内容（简化版，实际应根据参数动态生成）
        Object contentObj = params.get("content");
        String content = contentObj != null ? contentObj.toString() : "";
        if (!content.isEmpty()) {
            tspl.append("TEXT 20,20,\"2\",0,1,1,\"").append(escapeTspl(content)).append("\"\n");
        }

        tspl.append("PRINT 1,1\n");

        return tspl.toString();
    }
    
    /**
     * 安全获取整数值
     */
    private int getIntValue(Map<String, Object> params, String key, int defaultValue) {
        Object value = params.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * TSPL 特殊字符转义（完整版）
     */
    private String escapeTspl(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t")
                   .replace("\0", "\\0")
                   .replace("\b", "\\b")
                   .replace("\f", "\\f");
    }

    /**
     * 获取连接类型
     */
    private String getConnectionType() {
        if (config == null) return "USB";

        String connectionType = config.getConnectionType();
        if (connectionType != null && !connectionType.isEmpty()) {
            return connectionType;
        }

        // 根据 IP 地址判断
        String ip = config.getIpAddress();
        if (ip != null && !ip.isEmpty()) {
            if (ip.toUpperCase().startsWith("COM") || ip.toUpperCase().startsWith("/DEV/")) {
                return "SERIAL";
            }
            return "NETWORK";
        }

        return "USB";
    }

    @Override
    public String getDriverName() {
        return "TSPL Label Printer Driver";
    }

    @Override
    public String getDriverVersion() {
        return "1.0.0";
    }

    @Override
    public String getSupportedDeviceType() {
        return "LABEL_PRINTER";
    }

    @Override
    public boolean isSupported(HardwareConfig config) {
        if (config == null) return false;

        // 检查设备类型
        String deviceType = config.getDeviceType();
        if (!"PRINTER".equals(deviceType) && !"LABEL_PRINTER".equals(deviceType)) {
            return false;
        }

        // 检查设备型号
        String deviceModel = config.getDeviceModel();
        if (deviceModel != null) {
            String modelUpper = deviceModel.toUpperCase();
            for (String supported : SUPPORTED_MODELS) {
                if (modelUpper.contains(supported)) {
                    return true;
                }
            }
        }

        // 默认支持
        return true;
    }

    @Override
    public void close() {
        disconnect();
        log.info("关闭 TSPL 标签打印机驱动");
    }
}
