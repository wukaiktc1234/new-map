package com.foodtraceability.driver;

import com.foodtraceability.entity.HardwareConfig;
import com.foodtraceability.entity.DeviceStatus;
import org.springframework.stereotype.Component;
import javax.print.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class WsdPrinterDriver implements DeviceDriver {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WsdPrinterDriver.class);
    private static final String WSD_PRINT_SERVICE = "/wsd/PrintService";
    private HardwareConfig config;
    private String printerIp;
    private int timeout = 10000;
    private boolean connected = false;
    private PrintService targetPrintService;

    @Override
    public boolean init(HardwareConfig config) {
        this.config = config;
        this.printerIp = config.getIpAddress();
        this.timeout = 10000;
        log.info("初始化WSD打印机驱动: {}", printerIp);
        return true;
    }

    @Override
    public boolean connect() {
        try {
            log.info("尝试连接WSD打印机: {}", printerIp);
            boolean reachable = InetAddress.getByName(printerIp).isReachable(5000);
            if (!reachable) {
                log.warn("无法ping通打印机: {}", printerIp);
            }
            targetPrintService = findSystemPrinter(printerIp);
            if (targetPrintService != null) {
                connected = true;
                log.info("WSD打印机连接成功: {} -> {}", printerIp, targetPrintService.getName());
                return true;
            }
            log.warn("未找到系统打印机: {}", printerIp);
            connected = reachable;
            return connected;
        } catch (Exception e) {
            log.error("连接WSD打印机失败", e);
            return false;
        }
    }

    @Override
    public void disconnect() {
        log.info("断开WSD打印机连接: {}", printerIp);
        connected = false;
        targetPrintService = null;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public DeviceStatus getDeviceStatus() {
        DeviceStatus status = new DeviceStatus();
        status.setDeviceType("PRINTER");
        status.setIpAddress(printerIp);
        status.setConnectionType("WSD");
        status.setLastCheckTime(new Date());
        try {
            long start = System.currentTimeMillis();
            boolean reachable = InetAddress.getByName(printerIp).isReachable(3000);
            long responseTime = System.currentTimeMillis() - start;
            status.setOnline(reachable);
            status.setResponseTime(responseTime);
            status.setDetails(reachable ? "设备在线" : "设备离线");
        } catch (Exception e) {
            status.setOnline(false);
            status.setErrorMessage(e.getMessage());
            status.setDetails("检测失败: " + e.getMessage());
        }
        return status;
    }

    @Override
    public Map<String, Object> executeOperation(String operationType, Map<String, Object> params) {
        Map<String, Object> result = new HashMap<>();
        switch (operationType.toLowerCase()) {
        case "print": 
        case "printtext": 
            String content = (String) params.get("content");
            if (content != null) {
                result = printText(content);
            } else {
                result.put("success", false);
                result.put("error", "缺少打印内容");
            }
            break;
        case "test": 
            result = printTestPage();
            break;
        case "status": 
            DeviceStatus status = getDeviceStatus();
            result.put("success", true);
            result.put("online", status.isOnline());
            result.put("details", status.getDetails());
            break;
        default: 
            result.put("success", false);
            result.put("error", "不支持的操作: " + operationType);
        }
        return result;
    }

    @Override
    public String getDriverName() {
        return "WSD-Printer-Driver";
    }

    @Override
    public String getDriverVersion() {
        return "1.0.0";
    }

    @Override
    public String getSupportedDeviceType() {
        return "PRINTER";
    }

    @Override
    public boolean isSupported(HardwareConfig config) {
        if (!"PRINTER".equals(config.getDeviceType())) {
            return false;
        }
        String connectionType = config.getConnectionType();
        return "WSD".equalsIgnoreCase(connectionType) || "NETWORK".equalsIgnoreCase(connectionType) || connectionType == null;
    }

    @Override
    public void close() {
        disconnect();
    }

    public Map<String, Object> printText(String content) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (targetPrintService == null) {
                targetPrintService = findSystemPrinter(printerIp);
            }
            if (targetPrintService == null) {
                result.put("success", false);
                result.put("error", "未找到打印机");
                return result;
            }
            PrintRequestAttributeSet attrs = new HashPrintRequestAttributeSet();
            attrs.add(MediaSizeName.ISO_A4);
            attrs.add(OrientationRequested.PORTRAIT);
            attrs.add(new Copies(1));
            DocPrintJob job = targetPrintService.createPrintJob();
            byte[] printData = content.getBytes(StandardCharsets.UTF_8);
            Doc doc = new SimpleDoc(printData, DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
            job.print(doc, attrs);
            log.info("打印任务已发送: {}", targetPrintService.getName());
            result.put("success", true);
            result.put("printer", targetPrintService.getName());
        } catch (Exception e) {
            log.error("打印失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> printTestPage() {
        String testContent = buildTestPage();
        return printText(testContent);
    }

    private PrintService findSystemPrinter(String ipAddress) {
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        for (PrintService service : services) {
            String name = service.getName().toLowerCase();
            if (name.contains("epson") || name.contains("l4366") || name.contains(ipAddress) || name.contains("wsd")) {
                log.info("找到匹配的系统打印机: {}", service.getName());
                return service;
            }
        }
        PrintService defaultPrinter = PrintServiceLookup.lookupDefaultPrintService();
        if (defaultPrinter != null) {
            log.info("使用默认打印机: {}", defaultPrinter.getName());
        }
        return defaultPrinter;
    }

    private String buildTestPage() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n");
        sb.append("=====================================\n");
        sb.append("        食品追溯系统 - 打印测试页\n");
        sb.append("=====================================\n\n");
        sb.append("打印机: EPSON L4366\n");
        sb.append("IP地址: ").append(printerIp).append("\n");
        sb.append("协议: WSD\n");
        sb.append("测试时间: ").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\n\n");
        sb.append("-------------------------------------\n");
        sb.append("测试内容:\n");
        sb.append("  - 中文字符: 食品追溯管理系统\n");
        sb.append("  - 英文字符: Food Traceability System\n");
        sb.append("  - 数字字符: 0123456789\n");
        sb.append("-------------------------------------\n\n");
        sb.append("如果看到此页面，说明打印机连接成功！\n\n");
        sb.append("=====================================\n");
        return sb.toString();
    }
}
