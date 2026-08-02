package com.foodtraceability.service.impl;

import com.foodtraceability.entity.HardwareConfig;
import com.foodtraceability.entity.TestResult;
import com.foodtraceability.service.DeviceCommunicationTestService;
import com.foodtraceability.service.DeviceConnectionService;
import com.foodtraceability.util.CommunicationProtocolFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * 设备通信测试服务实现类
 * 提供设备通信的测试和验证功能
 */
@Service
public class DeviceCommunicationTestServiceImpl implements DeviceCommunicationTestService {

    private static final Logger log = LoggerFactory.getLogger(DeviceCommunicationTestServiceImpl.class);

    // 连接超时时间（毫秒）
    private static final int CONNECTION_TIMEOUT = 5000;

    // 读取超时时间（毫秒）
    private static final int READ_TIMEOUT = 3000;


    public DeviceCommunicationTestServiceImpl(DeviceConnectionService deviceConnectionService) {
        this.deviceConnectionService = deviceConnectionService;
    }

    private final DeviceConnectionService deviceConnectionService;

    @Override
    public TestResult testCommunication(HardwareConfig config) {
        log.info("测试设备通信: 设备类型={} 设备名称={}", config.getDeviceType(), config.getDeviceName());
        
        TestResult result = new TestResult();
        result.setDeviceType(config.getDeviceType());
        result.setDeviceName(config.getDeviceName());
        result.setConnectionType(config.getConnectionType());
        result.setIpAddress(config.getIpAddress());
        result.setPort(config.getPort());

        long startTime = System.currentTimeMillis();

        try {
            // 根据设备类型选择测试方法
            switch (config.getDeviceType()) {
                case "PRINTER":
                    result = testPrinterCommunication(config);
                    break;
                case "SCANNER":
                    result = testScannerCommunication(config);
                    break;
                case "SCALE":
                    result = testScaleCommunication(config);
                    break;
                case "CAMERA":
                    result = testCameraCommunication(config);
                    break;
                case "KDS":
                    result = testKdsCommunication(config);
                    break;
                default:
                    // 通用连接测试
                    boolean connected = testGenericConnection(config);
                    result.setSuccess(connected);
                    result.setMessage(connected ? "设备连接成功" : "设备连接失败");
                    break;
            }

            long responseTime = System.currentTimeMillis() - startTime;
            result.setResponseTime(responseTime);

        } catch (Exception e) {
            log.error("测试设备通信异常: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("测试异常: " + e.getMessage());
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    @Override
    public TestResult testPrinterCommunication(HardwareConfig config) {
        TestResult result = new TestResult();
        result.setDeviceType(config.getDeviceType());
        result.setDeviceName(config.getDeviceName());
        result.setConnectionType(config.getConnectionType());
        result.setIpAddress(config.getIpAddress());
        result.setPort(config.getPort());

        try {
            log.info("测试打印机通信: 连接方式={} IP={} 端口={}", 
                    config.getConnectionType(), config.getIpAddress(), config.getPort());

            if ("NETWORK".equals(config.getConnectionType()) || "WIFI".equals(config.getConnectionType())) {
                // 网络打印机测试
                String ip = config.getIpAddress();
                int port = config.getPort() != null ? Integer.parseInt(config.getPort()) : 9100;

                boolean connected = testSocketConnection(ip, port);
                result.setSuccess(connected);
                result.setMessage(connected ? "网络打印机连接成功" : "网络打印机连接失败");
                result.setDetails("测试端口: " + port);

            } else if ("USB".equals(config.getConnectionType())) {
                // USB打印机测试
                boolean connected = testUsbConnection(config.getDeviceModel());
                result.setSuccess(connected);
                result.setMessage(connected ? "USB打印机连接成功" : "USB打印机连接失败");

            } else if ("SERIAL".equals(config.getConnectionType())) {
                // 串口打印机测试
                boolean connected = testSerialConnection(config.getPort(), 
                        config.getBaudRate() != null ? Integer.parseInt(config.getBaudRate()) : 9600);
                result.setSuccess(connected);
                result.setMessage(connected ? "串口打印机连接成功" : "串口打印机连接失败");

            } else {
                result.setSuccess(false);
                result.setMessage("不支持的打印机连接方式: " + config.getConnectionType());
            }

        } catch (Exception e) {
            log.error("测试打印机通信异常: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("测试异常: " + e.getMessage());
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    @Override
    public TestResult testScannerCommunication(HardwareConfig config) {
        TestResult result = new TestResult();
        result.setDeviceType(config.getDeviceType());
        result.setDeviceName(config.getDeviceName());
        result.setConnectionType(config.getConnectionType());
        result.setIpAddress(config.getIpAddress());
        result.setPort(config.getPort());

        try {
            log.info("测试扫码枪通信: 连接方式={}", config.getConnectionType());

            if ("NETWORK".equals(config.getConnectionType())) {
                // 网络扫码枪测试
                String ip = config.getIpAddress();
                int port = config.getPort() != null ? Integer.parseInt(config.getPort()) : 9101;

                boolean connected = testSocketConnection(ip, port);
                result.setSuccess(connected);
                result.setMessage(connected ? "网络扫码枪连接成功" : "网络扫码枪连接失败");

            } else if ("USB".equals(config.getConnectionType())) {
                // USB扫码枪测试
                boolean connected = testUsbConnection(config.getDeviceModel());
                result.setSuccess(connected);
                result.setMessage(connected ? "USB扫码枪连接成功" : "USB扫码枪连接失败");

            } else if ("SERIAL".equals(config.getConnectionType())) {
                // 串口扫码枪测试
                boolean connected = testSerialConnection(config.getPort(),
                        config.getBaudRate() != null ? Integer.parseInt(config.getBaudRate()) : 9600);
                result.setSuccess(connected);
                result.setMessage(connected ? "串口扫码枪连接成功" : "串口扫码枪连接失败");

            } else {
                result.setSuccess(false);
                result.setMessage("不支持的扫码枪连接方式: " + config.getConnectionType());
            }

        } catch (Exception e) {
            log.error("测试扫码枪通信异常: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("测试异常: " + e.getMessage());
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    @Override
    public TestResult testScaleCommunication(HardwareConfig config) {
        TestResult result = new TestResult();
        result.setDeviceType(config.getDeviceType());
        result.setDeviceName(config.getDeviceName());
        result.setConnectionType(config.getConnectionType());
        result.setIpAddress(config.getIpAddress());
        result.setPort(config.getPort());

        try {
            log.info("测试电子秤通信: 连接方式={}", config.getConnectionType());

            if ("NETWORK".equals(config.getConnectionType())) {
                // 网络电子秤测试
                String ip = config.getIpAddress();
                int port = config.getPort() != null ? Integer.parseInt(config.getPort()) : 9102;

                boolean connected = testSocketConnection(ip, port);
                result.setSuccess(connected);
                result.setMessage(connected ? "网络电子秤连接成功" : "网络电子秤连接失败");

            } else if ("SERIAL".equals(config.getConnectionType())) {
                // 串口电子秤测试
                boolean connected = testSerialConnection(config.getPort(),
                        config.getBaudRate() != null ? Integer.parseInt(config.getBaudRate()) : 9600);
                result.setSuccess(connected);
                result.setMessage(connected ? "串口电子秤连接成功" : "串口电子秤连接失败");

            } else if ("USB".equals(config.getConnectionType())) {
                // USB电子秤测试
                boolean connected = testUsbConnection(config.getDeviceModel());
                result.setSuccess(connected);
                result.setMessage(connected ? "USB电子秤连接成功" : "USB电子秤连接失败");

            } else {
                result.setSuccess(false);
                result.setMessage("不支持的电子秤连接方式: " + config.getConnectionType());
            }

        } catch (Exception e) {
            log.error("测试电子秤通信异常: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("测试异常: " + e.getMessage());
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    @Override
    public TestResult testCameraCommunication(HardwareConfig config) {
        TestResult result = new TestResult();
        result.setDeviceType(config.getDeviceType());
        result.setDeviceName(config.getDeviceName());
        result.setConnectionType(config.getConnectionType());
        result.setIpAddress(config.getIpAddress());
        result.setPort(config.getPort());

        try {
            log.info("测试摄像头通信: 连接方式={}", config.getConnectionType());

            if ("NETWORK".equals(config.getConnectionType())) {
                // 网络摄像头测试（RTSP协议）
                String ip = config.getIpAddress();
                int port = config.getPort() != null ? Integer.parseInt(config.getPort()) : 554;

                CommunicationProtocolFactory.RtspProtocol rtspProtocol =
                        CommunicationProtocolFactory.getRtspProtocol();

                boolean connected = rtspProtocol.connect(ip, port);
                result.setSuccess(connected);
                result.setMessage(connected ? "网络摄像头连接成功" : "网络摄像头连接失败");

            } else if ("USB".equals(config.getConnectionType())) {
                // USB摄像头测试
                boolean connected = testUsbConnection(config.getDeviceModel());
                result.setSuccess(connected);
                result.setMessage(connected ? "USB摄像头连接成功" : "USB摄像头连接失败");

            } else {
                result.setSuccess(false);
                result.setMessage("不支持的摄像头连接方式: " + config.getConnectionType());
            }

        } catch (Exception e) {
            log.error("测试摄像头通信异常: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("测试异常: " + e.getMessage());
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    @Override
    public TestResult testKdsCommunication(HardwareConfig config) {
        TestResult result = new TestResult();
        result.setDeviceType(config.getDeviceType());
        result.setDeviceName(config.getDeviceName());
        result.setConnectionType(config.getConnectionType());
        result.setIpAddress(config.getIpAddress());
        result.setPort(config.getPort());

        try {
            log.info("测试KDS通信: IP={} 端口={}", config.getIpAddress(), config.getPort());

            String ip = config.getIpAddress();
            int port = config.getPort() != null ? Integer.parseInt(config.getPort()) : 8080;

            boolean connected = testSocketConnection(ip, port);
            result.setSuccess(connected);
            result.setMessage(connected ? "KDS连接成功" : "KDS连接失败");

        } catch (Exception e) {
            log.error("测试KDS通信异常: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("测试异常: " + e.getMessage());
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    @Override
    public Map<String, Object> sendTestPrint(HardwareConfig config) {
        Map<String, Object> result = new HashMap<>();

        try {
            log.info("发送测试打印: 设备名称={}", config.getDeviceName());

            // 构建测试打印内容
            String testContent = buildTestPrintContent();

            // 根据连接方式发送打印数据
            if ("NETWORK".equals(config.getConnectionType()) || "WIFI".equals(config.getConnectionType())) {
                String ip = config.getIpAddress();
                int port = config.getPort() != null ? Integer.parseInt(config.getPort()) : 9100;

                CommunicationProtocolFactory.SocketProtocol socketProtocol =
                        CommunicationProtocolFactory.getSocketProtocol();

                boolean success = socketProtocol.sendData(ip, port, testContent, CONNECTION_TIMEOUT);

                result.put("success", success);
                result.put("message", success ? "测试打印发送成功" : "测试打印发送失败");
                result.put("content", testContent);

            } else {
                result.put("success", false);
                result.put("message", "仅支持网络打印机测试打印");
            }

        } catch (Exception e) {
            log.error("发送测试打印异常: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "测试打印异常: " + e.getMessage());
        }

        return result;
    }

    @Override
    public Map<String, Object> sendTestScan(HardwareConfig config) {
        Map<String, Object> result = new HashMap<>();

        try {
            log.info("发送测试扫描: 设备名称={}", config.getDeviceName());

            if ("NETWORK".equals(config.getConnectionType())) {
                String ip = config.getIpAddress();
                int port = config.getPort() != null ? Integer.parseInt(config.getPort()) : 9101;

                // 发送扫描请求
                CommunicationProtocolFactory.SocketProtocol socketProtocol =
                        CommunicationProtocolFactory.getSocketProtocol();

                socketProtocol.connect(ip, port, CONNECTION_TIMEOUT);
                String scanResult = socketProtocol.receiveData(ip, port);

                result.put("success", true);
                result.put("message", "测试扫描成功");
                result.put("scanResult", scanResult);

            } else {
                result.put("success", false);
                result.put("message", "仅支持网络扫码枪测试扫描");
            }

        } catch (Exception e) {
            log.error("发送测试扫描异常: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "测试扫描异常: " + e.getMessage());
        }

        return result;
    }

    @Override
    public Map<String, Object> sendTestWeigh(HardwareConfig config) {
        Map<String, Object> result = new HashMap<>();

        try {
            log.info("发送测试称重: 设备名称={}", config.getDeviceName());

            if ("NETWORK".equals(config.getConnectionType())) {
                String ip = config.getIpAddress();
                int port = config.getPort() != null ? Integer.parseInt(config.getPort()) : 9102;

                // 发送称重请求
                CommunicationProtocolFactory.SocketProtocol socketProtocol =
                        CommunicationProtocolFactory.getSocketProtocol();

                socketProtocol.connect(ip, port, CONNECTION_TIMEOUT);
                String weightResult = socketProtocol.receiveData(ip, port);

                result.put("success", true);
                result.put("message", "测试称重成功");
                result.put("weightResult", weightResult);

            } else {
                result.put("success", false);
                result.put("message", "仅支持网络电子秤测试称重");
            }

        } catch (Exception e) {
            log.error("发送测试称重异常: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "测试称重异常: " + e.getMessage());
        }

        return result;
    }

    @Override
    public Map<String, Object> getDiagnostics(HardwareConfig config) {
        Map<String, Object> diagnostics = new HashMap<>();

        try {
            log.info("获取设备诊断信息: 设备名称={}", config.getDeviceName());

            // 基本信息
            diagnostics.put("deviceType", config.getDeviceType());
            diagnostics.put("deviceName", config.getDeviceName());
            diagnostics.put("connectionType", config.getConnectionType());
            diagnostics.put("ipAddress", config.getIpAddress());
            diagnostics.put("port", config.getPort());

            // 连接测试
            long startTime = System.currentTimeMillis();
            boolean connected = testGenericConnection(config);
            long responseTime = System.currentTimeMillis() - startTime;

            diagnostics.put("connected", connected);
            diagnostics.put("responseTime", responseTime + "ms");

            // 网络诊断
            if ("NETWORK".equals(config.getConnectionType()) || "WIFI".equals(config.getConnectionType())) {
                Map<String, Object> networkDiagnostics = getNetworkDiagnostics(config.getIpAddress());
                diagnostics.put("networkDiagnostics", networkDiagnostics);
            }

            // 设备状态
            diagnostics.put("status", connected ? "ONLINE" : "OFFLINE");
            diagnostics.put("lastCheckTime", System.currentTimeMillis());

        } catch (Exception e) {
            log.error("获取设备诊断信息异常: {}", e.getMessage(), e);
            diagnostics.put("error", e.getMessage());
        }

        return diagnostics;
    }

    /**
     * 测试Socket连接
     */
    private boolean testSocketConnection(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), CONNECTION_TIMEOUT);
            log.info("Socket连接成功: {}:{}", host, port);
            return true;
        } catch (Exception e) {
            log.error("Socket连接失败: {}:{} 错误={}", host, port, e.getMessage());
            return false;
        }
    }

    /**
     * 测试USB连接
     */
    private boolean testUsbConnection(String deviceModel) {
        try {
            CommunicationProtocolFactory.UsbProtocol usbProtocol =
                    CommunicationProtocolFactory.getUsbProtocol();
            return usbProtocol.connect(deviceModel);
        } catch (Exception e) {
            log.error("USB连接测试失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 测试串口连接
     */
    private boolean testSerialConnection(String portName, int baudRate) {
        try {
            CommunicationProtocolFactory.SerialProtocol serialProtocol =
                    CommunicationProtocolFactory.getSerialProtocol();
            return serialProtocol.connect(portName, baudRate);
        } catch (Exception e) {
            log.error("串口连接测试失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 测试通用连接
     */
    private boolean testGenericConnection(HardwareConfig config) {
        try {
            if ("NETWORK".equals(config.getConnectionType()) || "WIFI".equals(config.getConnectionType())) {
                String ip = config.getIpAddress();
                int port = config.getPort() != null ? Integer.parseInt(config.getPort()) : 80;
                return testSocketConnection(ip, port);
            } else if ("SERIAL".equals(config.getConnectionType())) {
                return testSerialConnection(config.getPort(),
                        config.getBaudRate() != null ? Integer.parseInt(config.getBaudRate()) : 9600);
            } else if ("USB".equals(config.getConnectionType())) {
                return testUsbConnection(config.getDeviceModel());
            }
            return false;
        } catch (Exception e) {
            log.error("通用连接测试失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 构建测试打印内容
     */
    private String buildTestPrintContent() {
        StringBuilder content = new StringBuilder();
        content.append("\u001B@"); // 初始化打印机
        content.append("\u001Ba\u0001"); // 居中对齐
        content.append("================================\n");
        content.append("        设备测试打印\n");
        content.append("================================\n");
        content.append("测试时间: ").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new java.util.Date())).append("\n");
        content.append("设备状态: 正常\n");
        content.append("================================\n\n");
        content.append("\u001Di"); // 切纸
        return content.toString();
    }

    /**
     * 获取网络诊断信息
     */
    private Map<String, Object> getNetworkDiagnostics(String ipAddress) {
        Map<String, Object> networkDiagnostics = new HashMap<>();

        try {
            // Ping测试
            boolean reachable = java.net.InetAddress.getByName(ipAddress).isReachable(5000);
            networkDiagnostics.put("pingable", reachable);

            // DNS解析
            try {
                java.net.InetAddress inetAddress = java.net.InetAddress.getByName(ipAddress);
                networkDiagnostics.put("resolvedHost", inetAddress.getHostName());
            } catch (Exception e) {
                networkDiagnostics.put("dnsResolutionError", e.getMessage());
            }

        } catch (Exception e) {
            networkDiagnostics.put("error", e.getMessage());
        }

        return networkDiagnostics;
    }
}
