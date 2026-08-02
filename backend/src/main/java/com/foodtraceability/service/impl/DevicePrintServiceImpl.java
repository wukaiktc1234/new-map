package com.foodtraceability.service.impl;

import com.foodtraceability.entity.HardwareConfig;
import com.foodtraceability.entity.PrintTask;
import com.foodtraceability.service.DeviceConnectionService;
import com.foodtraceability.service.DevicePrintService;
import com.foodtraceability.service.DeviceStatusService;
import com.foodtraceability.util.PrintTaskQueueManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 设备打印服务实现类
 * 负责设备打印功能的实现，包括打印数据生成和打印命令发送
 */
@Service
public class DevicePrintServiceImpl implements DevicePrintService {
    
    private static final Logger log = LoggerFactory.getLogger(DevicePrintServiceImpl.class);
    

    private final DeviceStatusService deviceStatusService;
    
    private final DeviceConnectionService deviceConnectionService;
    
    private final PrintTaskQueueManager printTaskQueueManager;
    
    // ObjectMapper单例，避免重复创建
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // 打印格式缓存，按设备类型缓存，有效期5分钟
    private final Map<String, PrintFormatCache> printFormatCache = new ConcurrentHashMap<>();
    
    // 打印格式缓存内部类
    private static class PrintFormatCache {
        private PrintFormat format;
        private long timestamp;
        
        public PrintFormatCache(PrintFormat format) {
            this.format = format;
            this.timestamp = System.currentTimeMillis();
        }
        
        public PrintFormat getFormat() {
            return format;
        }
        
        // 缓存是否有效（5分钟内）
        public boolean isValid() {
            return System.currentTimeMillis() - timestamp < 5 * 60 * 1000;
        }
    }

    public DevicePrintServiceImpl(DeviceStatusService deviceStatusService, DeviceConnectionService deviceConnectionService, PrintTaskQueueManager printTaskQueueManager) {
        this.deviceStatusService = deviceStatusService;
        this.deviceConnectionService = deviceConnectionService;
        this.printTaskQueueManager = printTaskQueueManager;
    }
    
    @Override
    public Boolean printTraceabilityLabel(String traceabilityCode, String productName) {
        try {
            log.info("打印追溯码标签: {} - {}", traceabilityCode, productName);
            
            // 创建打印任务
            PrintTask task = new PrintTask();
            task.setPrintType(2);  // 2=标签
            task.setDeviceType(1);  // 1=打印机
            task.setTraceabilityCode(traceabilityCode);
            task.setProductName(productName);
            
            // 提交打印任务到队列
            PrintTask submittedTask = printTaskQueueManager.submitTask(task);
            log.info("打印任务已提交: 任务ID={}", submittedTask.getTaskId());
            
            // 等待任务执行完成（5秒超时）
            long startTime = System.currentTimeMillis();
            long timeout = 5000;
            while (System.currentTimeMillis() - startTime < timeout) {
                PrintTask currentTask = printTaskQueueManager.getTaskById(String.valueOf(submittedTask.getTaskId()));
                if (currentTask != null) {
                    if (Integer.valueOf(2).equals(currentTask.getStatus())) {
                        log.info("打印成功: 任务ID={}", currentTask.getTaskId());
                        return true;
                    } else if (Integer.valueOf(3).equals(currentTask.getStatus())) {
                        log.warn("打印失败: 任务ID={}, 错误信息={}", currentTask.getTaskId(), currentTask.getErrorMessage());
                        return false;
                    }
                }
                // 等待100毫秒后再次查询
                Thread.sleep(100);
            }
            
            log.warn("打印任务超时: 任务ID={}", submittedTask.getTaskId());
            return false;
        } catch (Exception e) {
            log.error("打印异常: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public String generatePrintData(String traceabilityCode, String productName, HardwareConfig config) {
        try {
            // 从缓存获取或创建打印格式
            PrintFormat printFormat = getOrCreatePrintFormat(config);
            
            StringBuilder sb = new StringBuilder();
            
            // 直接生成EPSON ESC/POS格式的打印数据
            sb.append((char) 27).append((char) 64); // 初始化打印机 (ESC @)
            
            // 设置打印浓度
            int densityLevel = printFormat.getPrintDensity();
            if (densityLevel >= 1 && densityLevel <= 5) {
                // ESC * n - 设置打印浓度，n=0-8
                int densityValue = 2 + (densityLevel - 1) * 2; // 映射到0-8范围
                sb.append((char) 27).append((char) 42).append((char) densityValue);
            }
            
            // 设置字体大小
            int fontSize = printFormat.getTitleSize();
            int fontCommand = 0;
            if (fontSize >= 36) fontCommand = 24; // 3倍大小
            else if (fontSize >= 24) fontCommand = 8; // 2倍大小
            else if (fontSize >= 18) fontCommand = 3; // 1.5倍大小
            else fontCommand = 0; // 正常大小
            sb.append((char) 27).append((char) 33).append((char) fontCommand); // 放大文字 (ESC ! n)
            
            // 设置对齐方式
            String align = printFormat.getTitleAlign();
            if ("right".equals(align)) {
                sb.append((char) 27).append((char) 97).append((char) 2); // 右对齐 (ESC a 2)
            } else if ("center".equals(align)) {
                sb.append((char) 27).append((char) 97).append((char) 1); // 居中对齐 (ESC a 1)
            } else {
                sb.append((char) 27).append((char) 97).append((char) 0); // 左对齐 (ESC a 0)
            }
            
            // 打印标题
            sb.append(printFormat.getTitle()).append("\n");
            
            // 重置对齐为左对齐，用于后续内容
            sb.append((char) 27).append((char) 97).append((char) 0); // 左对齐 (ESC a 0)
            
            // 打印分隔线
            sb.append("----------\n");
            
            // 设置正常字体大小
            sb.append((char) 27).append((char) 33).append((char) 0); // 正常大小 (ESC ! 0)
            
            // 打印产品信息
            if (printFormat.isShowProductName()) {
                sb.append("产品名称: ").append(productName).append("\n");
            }
            if (printFormat.isShowTraceabilityCode()) {
                sb.append("追溯码: ").append(traceabilityCode).append("\n");
            }
            if (printFormat.isShowDate()) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                sb.append("打印时间: ").append(sdf.format(new Date())).append("\n");
            }
            
            // 打印空行
            sb.append("\n");
            sb.append("\n");
            
            // 走纸并切纸
            sb.append((char) 27).append((char) 100).append((char) printFormat.getFeedLines()); // 换行N行 (ESC d N)
            sb.append((char) 29).append((char) 86).append((char) 65).append((char) 0); // 切纸 (GS V A 0)
            
            return sb.toString();
        } catch (Exception e) {
            log.error("生成打印数据失败: {}", e.getMessage(), e);
            return generateDefaultPrintData(traceabilityCode, productName);
        }
    }
    
    /**
     * 从缓存获取或创建打印格式
     * @param config 设备配置
     * @return 打印格式
     */
    private PrintFormat getOrCreatePrintFormat(HardwareConfig config) {
        String cacheKey = "print_format_" + config.getDeviceType();
        PrintFormatCache cache = printFormatCache.get(cacheKey);
        
        // 检查缓存是否有效
        if (cache != null && cache.isValid()) {
            log.debug("使用缓存的打印格式: {}", config.getDeviceType());
            return cache.getFormat();
        }
        
        // 缓存无效或不存在，创建新的打印格式
        PrintFormat format = new PrintFormat();
        
        // 从 config_json 加载自定义打印格式
        if (config.getConfigJson() != null) {
            try {
                com.fasterxml.jackson.databind.JsonNode configJson = objectMapper.readTree(config.getConfigJson());
                if (configJson.has("printFormat")) {
                    com.fasterxml.jackson.databind.JsonNode formatJson = configJson.get("printFormat");
                    // 合并自定义配置到默认配置
                    if (formatJson.has("title")) {
                        format.setTitle(formatJson.get("title").asText());
                    }
                    if (formatJson.has("titleFont")) {
                        format.setTitleFont(formatJson.get("titleFont").asText());
                    }
                    if (formatJson.has("titleSize")) {
                        format.setTitleSize(formatJson.get("titleSize").asInt());
                    }
                    if (formatJson.has("titleAlign")) {
                        format.setTitleAlign(formatJson.get("titleAlign").asText());
                    }
                    if (formatJson.has("showProductName")) {
                        format.setShowProductName(formatJson.get("showProductName").asBoolean());
                    }
                    if (formatJson.has("showTraceabilityCode")) {
                        format.setShowTraceabilityCode(formatJson.get("showTraceabilityCode").asBoolean());
                    }
                    if (formatJson.has("showDate")) {
                        format.setShowDate(formatJson.get("showDate").asBoolean());
                    }
                    if (formatJson.has("showQrCode")) {
                        format.setShowQrCode(formatJson.get("showQrCode").asBoolean());
                    }
                    if (formatJson.has("paperWidth")) {
                        format.setPaperWidth(formatJson.get("paperWidth").asText());
                    }
                    if (formatJson.has("printDensity")) {
                        format.setPrintDensity(formatJson.get("printDensity").asInt());
                    }
                    if (formatJson.has("feedLines")) {
                        format.setFeedLines(formatJson.get("feedLines").asInt());
                    }
                }
            } catch (Exception e) {
                log.warn("解析打印格式配置失败: {}", e.getMessage());
                // 解析失败使用默认配置
            }
        }
        
        // 更新缓存
        printFormatCache.put(cacheKey, new PrintFormatCache(format));
        log.debug("创建并缓存新的打印格式: {}", config.getDeviceType());
        
        return format;
    }
    
    @Override
    public String generateDefaultPrintData(String traceabilityCode, String productName) {
        StringBuilder sb = new StringBuilder();
        
        // 直接生成EPSON ESC/POS格式的打印数据
        sb.append((char) 27).append((char) 64); // 初始化打印机 (ESC @)
        sb.append("\n"); // 换行
        
        // 设置字体大小
        sb.append((char) 27).append((char) 33).append((char) 8); // 放大文字 (ESC ! 8)
        
        // 打印标题
        sb.append("食品追溯标签\n");
        sb.append("----------\n");
        
        // 打印产品信息
        sb.append("产品名称: ").append(productName).append("\n");
        sb.append("追溯码: ").append(traceabilityCode).append("\n");
        
        // 打印空行
        sb.append("\n");
        sb.append("\n");
        
        // 走纸并切纸
        sb.append((char) 27).append((char) 100).append((char) 5); // 换行5行 (ESC d 5)
        sb.append((char) 29).append((char) 86).append((char) 65).append((char) 0); // 切纸 (GS V A 0)
        
        return sb.toString();
    }
    
    @Override
    public Boolean sendPrintCommand(HardwareConfig config, String printData) {
        try {
            String ip = config.getIpAddress();
            String port = config.getPort();
            
            log.info("正在向EPSON打印机发送打印数据: {}:{} 型号: {}", ip, port, config.getDeviceModel());
            
            // 直接使用已经生成好的EPSON ESC/POS格式数据
            Boolean result;
            if (port != null && port.contains("WSD")) {
                // WSD协议使用HTTP POST请求
                result = deviceConnectionService.sendWsdPrintCommand(ip, printData);
            } else {
                // 传统端口使用Socket连接
                int numericPort = port != null ? Integer.parseInt(port) : 9100;
                result = deviceConnectionService.sendSocketPrintCommand(ip, numericPort, printData);
            }
            
            return result;
        } catch (Exception e) {
            log.error("EPSON打印机打印失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public byte[] generateTestPrintData(HardwareConfig config) {
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            
            baos.write(new byte[] { 0x1B, 0x40 });
            baos.write(new byte[] { 0x1B, 0x61, 0x01 });
            
            baos.write("========================================\n".getBytes("GBK"));
            baos.write(new byte[] { 0x1B, 0x61, 0x00 });
            baos.write("        打印机测试页\n".getBytes("GBK"));
            baos.write(new byte[] { 0x1B, 0x61, 0x01 });
            baos.write("========================================\n".getBytes("GBK"));
            baos.write("\n".getBytes("GBK"));
            
            baos.write(new byte[] { 0x1B, 0x61, 0x00 });
            baos.write(("设备名称: " + (config.getDeviceName() != null ? config.getDeviceName() : "未设置") + "\n").getBytes("GBK"));
            baos.write(("设备类型: " + config.getDeviceType() + "\n").getBytes("GBK"));
            baos.write(("通信IP: " + config.getIpAddress() + "\n").getBytes("GBK"));
            baos.write(("通信端口: " + (config.getPort() != null ? config.getPort() : "默认") + "\n").getBytes("GBK"));
            baos.write("设备状态: 在线\n".getBytes("GBK"));
            baos.write("\n".getBytes("GBK"));
            
            String testTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
            baos.write(("测试时间: " + testTime + "\n").getBytes("GBK"));
            baos.write("\n".getBytes("GBK"));
            
            baos.write(new byte[] { 0x1B, 0x61, 0x01 });
            baos.write("========================================\n".getBytes("GBK"));
            baos.write("      测试完成 - 打印机工作正常\n".getBytes("GBK"));
            baos.write("========================================\n".getBytes("GBK"));
            
            baos.write(new byte[] { 0x1B, 0x64, 0x04 });
            
            byte[] result = baos.toByteArray();
            log.info("生成测试打印数据: {} 字节", result.length);
            return result;
        } catch (Exception e) {
            log.error("生成测试打印数据失败: {}", e.getMessage(), e);
            return "打印机测试\n".getBytes();
        }
    }
    
    // 打印格式内部类，用于存储自定义打印格式配置
    private static class PrintFormat {
        private String title;
        private String titleFont;
        private int titleSize;
        private String titleAlign;
        private boolean showProductName;
        private boolean showTraceabilityCode;
        private boolean showDate;
        private boolean showQrCode;
        private String paperWidth;
        private int printDensity;
        private int feedLines;
        
        public PrintFormat() {
            this.title = "食品追溯标签";
            this.titleFont = "Microsoft YaHei";
            this.titleSize = 24;
            this.titleAlign = "center";
            this.showProductName = true;
            this.showTraceabilityCode = true;
            this.showDate = true;
            this.showQrCode = true;
            this.paperWidth = "80";
            this.printDensity = 3;
            this.feedLines = 5;
        }
        
        // Getters and setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getTitleFont() { return titleFont; }
        public void setTitleFont(String titleFont) { this.titleFont = titleFont; }
        public int getTitleSize() { return titleSize; }
        public void setTitleSize(int titleSize) { this.titleSize = titleSize; }
        public String getTitleAlign() { return titleAlign; }
        public void setTitleAlign(String titleAlign) { this.titleAlign = titleAlign; }
        public boolean isShowProductName() { return showProductName; }
        public void setShowProductName(boolean showProductName) { this.showProductName = showProductName; }
        public boolean isShowTraceabilityCode() { return showTraceabilityCode; }
        public void setShowTraceabilityCode(boolean showTraceabilityCode) { this.showTraceabilityCode = showTraceabilityCode; }
        public boolean isShowDate() { return showDate; }
        public void setShowDate(boolean showDate) { this.showDate = showDate; }
        public boolean isShowQrCode() { return showQrCode; }
        public void setShowQrCode(boolean showQrCode) { this.showQrCode = showQrCode; }
        public String getPaperWidth() { return paperWidth; }
        public void setPaperWidth(String paperWidth) { this.paperWidth = paperWidth; }
        public int getPrintDensity() { return printDensity; }
        public void setPrintDensity(int printDensity) { this.printDensity = printDensity; }
        public int getFeedLines() { return feedLines; }
        public void setFeedLines(int feedLines) { this.feedLines = feedLines; }
    }
    
    /**
     * 直接执行打印热敏纸，不通过任务队列
     * 用于避免循环调用
     */
    private Boolean executePrintThermalPaper(String content, String deviceType) {
        try {
            log.info("直接打印热敏纸: 设备类型={}, 内容长度={}", deviceType, content.length());
            
            HardwareConfig printerConfig = deviceStatusService.getHardwareConfig(deviceType);
            if (printerConfig == null) {
                log.warn("打印机配置不存在: {}", deviceType);
                return false;
            }
            
            // 生成热敏纸打印数据，使用ESC/POS指令
            StringBuilder sb = new StringBuilder();
            sb.append((char) 27).append((char) 64); // 初始化打印机
            sb.append((char) 27).append((char) 61).append((char) 0); // 选择标准模式
            sb.append(content); // 打印内容
            sb.append((char) 10); // 换行
            sb.append((char) 27).append((char) 100).append((char) 5); // 走纸5行
            sb.append((char) 29).append((char) 86).append((char) 65).append((char) 0); // 切纸
            
            Boolean result = sendPrintCommand(printerConfig, sb.toString());
            
            if (result) {
                log.info("直接热敏纸打印成功");
            } else {
                log.warn("直接热敏纸打印失败");
            }
            
            return result;
        } catch (Exception e) {
            log.error("直接打印热敏纸异常: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public Boolean printThermalPaper(String content, String deviceType) {
        try {
            log.info("打印热敏纸: 设备类型={}, 内容长度={}", deviceType, content.length());
            
            // 创建打印任务
            PrintTask task = new PrintTask();
            task.setPrintType(1);  // 1=小票（热敏纸）
            task.setDeviceType(mapDeviceTypeToInteger(deviceType));
            task.setContent(content);
            
            // 提交打印任务到队列
            PrintTask submittedTask = printTaskQueueManager.submitTask(task);
            log.info("热敏纸打印任务已提交: 任务ID={}", submittedTask.getTaskId());
            
            // 等待任务执行完成（5秒超时）
            long startTime = System.currentTimeMillis();
            long timeout = 5000;
            while (System.currentTimeMillis() - startTime < timeout) {
                PrintTask currentTask = printTaskQueueManager.getTaskById(String.valueOf(submittedTask.getTaskId()));
                if (currentTask != null) {
                    if (Integer.valueOf(2).equals(currentTask.getStatus())) {
                        log.info("热敏纸打印成功: 任务ID={}", currentTask.getTaskId());
                        return true;
                    } else if (Integer.valueOf(3).equals(currentTask.getStatus())) {
                        log.warn("热敏纸打印失败: 任务ID={}, 错误信息={}", currentTask.getTaskId(), currentTask.getErrorMessage());
                        return false;
                    }
                }
                // 等待100毫秒后再次查询
                Thread.sleep(100);
            }
            
            log.warn("热敏纸打印任务超时: 任务ID={}", submittedTask.getTaskId());
            return false;
        } catch (Exception e) {
            log.error("打印热敏纸异常: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 直接执行打印文件，不通过任务队列
     * 用于避免循环调用
     */
    private Boolean executePrintFile(String filePath, String deviceType) {
        try {
            log.info("直接打印文件: 设备类型={}, 文件路径={}", deviceType, filePath);
            
            HardwareConfig printerConfig = deviceStatusService.getHardwareConfig(deviceType);
            if (printerConfig == null) {
                log.warn("打印机配置不存在: {}", deviceType);
                return false;
            }
            
            // 读取文件内容
            java.io.File file = new java.io.File(filePath);
            if (!file.exists() || !file.isFile()) {
                log.warn("文件不存在或不是文件: {}", filePath);
                return false;
            }
            
            String content;
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(file))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                content = sb.toString();
            }
            
            // 生成打印数据
            StringBuilder printData = new StringBuilder();
            printData.append((char) 27).append((char) 64); // 初始化打印机
            printData.append((char) 27).append((char) 61).append((char) 0); // 选择标准模式
            printData.append(content); // 打印文件内容
            printData.append((char) 27).append((char) 100).append((char) 5); // 走纸5行
            printData.append((char) 29).append((char) 86).append((char) 65).append((char) 0); // 切纸
            
            Boolean result = sendPrintCommand(printerConfig, printData.toString());
            
            if (result) {
                log.info("直接文件打印成功");
            } else {
                log.warn("直接文件打印失败");
            }
            
            return result;
        } catch (Exception e) {
            log.error("直接打印文件异常: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public Boolean printFile(String filePath, String deviceType) {
        try {
            log.info("打印文件: 设备类型={}, 文件路径={}", deviceType, filePath);
            
            // 检查文件是否存在
            java.io.File file = new java.io.File(filePath);
            if (!file.exists() || !file.isFile()) {
                log.warn("文件不存在或不是文件: {}", filePath);
                return false;
            }
            
            // 创建打印任务
            PrintTask task = new PrintTask();
            task.setPrintType(3);  // 3=报表/文件
            task.setDeviceType(mapDeviceTypeToInteger(deviceType));
            task.setFilePath(filePath);
            
            // 提交打印任务到队列
            PrintTask submittedTask = printTaskQueueManager.submitTask(task);
            log.info("文件打印任务已提交: 任务ID={}", submittedTask.getTaskId());
            
            // 等待任务执行完成（10秒超时，文件打印可能需要更长时间）
            long startTime = System.currentTimeMillis();
            long timeout = 10000;
            while (System.currentTimeMillis() - startTime < timeout) {
                PrintTask currentTask = printTaskQueueManager.getTaskById(String.valueOf(submittedTask.getTaskId()));
                if (currentTask != null) {
                    if (Integer.valueOf(2).equals(currentTask.getStatus())) {
                        log.info("文件打印成功: 任务ID={}", currentTask.getTaskId());
                        return true;
                    } else if (Integer.valueOf(3).equals(currentTask.getStatus())) {
                        log.warn("文件打印失败: 任务ID={}, 错误信息={}", currentTask.getTaskId(), currentTask.getErrorMessage());
                        return false;
                    }
                }
                // 等待200毫秒后再次查询
                Thread.sleep(200);
            }
            
            log.warn("文件打印任务超时: 任务ID={}", submittedTask.getTaskId());
            return false;
        } catch (Exception e) {
            log.error("打印文件异常: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 直接执行打印发票，不通过任务队列
     * 用于避免循环调用
     */
    private Boolean executePrintInvoice(Map<String, Object> invoiceData, String deviceType) {
        try {
            log.info("直接打印发票: 设备类型={}, 发票数据={}", deviceType, invoiceData);
            
            HardwareConfig printerConfig = deviceStatusService.getHardwareConfig(deviceType);
            if (printerConfig == null) {
                log.warn("打印机配置不存在: {}", deviceType);
                return false;
            }
            
            // 生成发票打印数据，格式化发票信息
            StringBuilder sb = new StringBuilder();
            sb.append((char) 27).append((char) 64); // 初始化打印机
            sb.append((char) 27).append((char) 77).append((char) 1); // 选择字符大小
            sb.append("\n");
            sb.append("=========== 发票 ===========\n");
            sb.append("\n");
            
            // 发票头信息
            sb.append("发票号码: " + invoiceData.getOrDefault("invoiceNo", "") + "\n");
            sb.append("发票日期: " + invoiceData.getOrDefault("invoiceDate", "") + "\n");
            sb.append("\n");
            
            // 发票内容
            sb.append("商品名称          数量  单价    金额\n");
            sb.append("---------------------------------\n");
            
            // 商品列表
            if (invoiceData.containsKey("items")) {
                @SuppressWarnings("unchecked")
                java.util.List<Map<String, Object>> items = (java.util.List<Map<String, Object>>) invoiceData.get("items");
                for (Map<String, Object> item : items) {
                    String name = (String) item.getOrDefault("name", "");
                    String quantity = String.valueOf(item.getOrDefault("quantity", 0));
                    String price = String.valueOf(item.getOrDefault("price", 0));
                    String amount = String.valueOf(item.getOrDefault("amount", 0));
                    
                    // 格式化商品行
                    sb.append(String.format("%-16s %4s %6s %8s\n", name, quantity, price, amount));
                }
            }
            
            sb.append("---------------------------------\n");
            sb.append("合计金额: " + invoiceData.getOrDefault("totalAmount", "") + "\n");
            sb.append("\n");
            sb.append("备注: " + invoiceData.getOrDefault("remark", "") + "\n");
            sb.append("\n");
            sb.append("=============================\n");
            sb.append("\n");
            
            // 走纸并切纸
            sb.append((char) 27).append((char) 100).append((char) 5); // 走纸5行
            sb.append((char) 29).append((char) 86).append((char) 65).append((char) 0); // 切纸
            
            Boolean result = sendPrintCommand(printerConfig, sb.toString());
            
            if (result) {
                log.info("直接发票打印成功");
            } else {
                log.warn("直接发票打印失败");
            }
            
            return result;
        } catch (Exception e) {
            log.error("直接打印发票异常: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public Boolean printInvoice(Map<String, Object> invoiceData, String deviceType) {
        try {
            log.info("打印发票: 设备类型={}, 发票数据={}", deviceType, invoiceData);
            
            // 创建打印任务
            PrintTask task = new PrintTask();
            task.setPrintType(1);  // 1=小票（发票也是小票形式）
            task.setDeviceType(mapDeviceTypeToInteger(deviceType));
            // 将发票数据转换为JSON字符串存储在contentJson字段中
            try {
                task.setContentJson(objectMapper.writeValueAsString(invoiceData));
            } catch (Exception e) {
                log.warn("序列化发票数据失败: {}", e.getMessage());
                task.setContentJson(invoiceData.toString());
            }
            
            // 提交打印任务到队列
            PrintTask submittedTask = printTaskQueueManager.submitTask(task);
            log.info("发票打印任务已提交: 任务ID={}", submittedTask.getTaskId());
            
            // 等待任务执行完成（10秒超时，发票打印可能需要更长时间）
            long startTime = System.currentTimeMillis();
            long timeout = 10000;
            while (System.currentTimeMillis() - startTime < timeout) {
                PrintTask currentTask = printTaskQueueManager.getTaskById(String.valueOf(submittedTask.getTaskId()));
                if (currentTask != null) {
                    if (Integer.valueOf(2).equals(currentTask.getStatus())) {
                        log.info("发票打印成功: 任务ID={}", currentTask.getTaskId());
                        return true;
                    } else if (Integer.valueOf(3).equals(currentTask.getStatus())) {
                        log.warn("发票打印失败: 任务ID={}, 错误信息={}", currentTask.getTaskId(), currentTask.getErrorMessage());
                        return false;
                    }
                }
                // 等待200毫秒后再次查询
                Thread.sleep(200);
            }
            
            log.warn("发票打印任务超时: 任务ID={}", submittedTask.getTaskId());
            return false;
        } catch (Exception e) {
            log.error("打印发票异常: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 将设备类型字符串映射为整数
     * @param deviceType 设备类型字符串
     * @return 整数类型的设备类型
     */
    private Integer mapDeviceTypeToInteger(String deviceType) {
        if (deviceType == null) return 1;  // 默认为打印机
        switch (deviceType.toUpperCase()) {
            case "PRINTER":
            case "1":
                return 1;
            case "SCANNER":
            case "2":
                return 2;
            case "CAMERA":
            case "SCALE":
            case "3":
                return 3;
            case "KDS":
            case "4":
                return 4;
            case "POS":
            case "5":
                return 5;
            default:
                try {
                    return Integer.parseInt(deviceType);
                } catch (NumberFormatException e) {
                    return 1;  // 默认为打印机
                }
        }
    }
    
    /**
     * 提交打印任务到队列
     * @param task 打印任务
     * @return 提交结果，包含任务ID
     */
    @Override
    public PrintTask submitPrintTask(PrintTask task) {
        log.info("提交打印任务到队列: 任务ID={}, 打印类型={}, 设备类型={}", 
                task.getTaskId(), task.getPrintType(), task.getDeviceType());
        return printTaskQueueManager.submitTask(task);
    }
}
