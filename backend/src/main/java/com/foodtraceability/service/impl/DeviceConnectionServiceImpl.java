package com.foodtraceability.service.impl;

import com.foodtraceability.entity.DeviceStatus;
import com.foodtraceability.entity.HardwareConfig;
import com.foodtraceability.entity.TestResult;
import com.foodtraceability.service.DeviceConnectionService;
import com.foodtraceability.util.CommunicationProtocolFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 设备连接服务实现类
 * 负责处理各种设备类型的连接测试和通信
 */
@Service
public class DeviceConnectionServiceImpl implements DeviceConnectionService {

    private static final Logger log = LoggerFactory.getLogger(DeviceConnectionServiceImpl.class);

    // 连接超时时间（毫秒）
    private static final int CONNECTION_TIMEOUT = 5000;

    @Override
    public Boolean testConnection(HardwareConfig config) {
        try {
            TestResult result = testConnectionDetailed(config);
            return result.getSuccess();
        } catch (Exception e) {
            log.error("测试设备连接失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public TestResult testConnectionDetailed(HardwareConfig config) {
        TestResult result = new TestResult();
        result.setDeviceType(config.getDeviceType());
        result.setDeviceName(config.getDeviceName());
        result.setConnectionType(config.getConnectionType());
        result.setIpAddress(config.getIpAddress());
        result.setPort(config.getPort());

        log.info("=== 开始测试设备连接 ===");
        log.info("设备类型: {}", config.getDeviceType());
        log.info("设备名称: {}", config.getDeviceName());
        log.info("连接方式: {}", config.getConnectionType());
        log.info("IP地址: [{}]", config.getIpAddress());
        log.info("端口: [{}]", config.getPort());
        log.info("配置JSON: [{}]", config.getConfigJson());

        long startTime = System.currentTimeMillis();

        try {
            // 根据设备类型选择测试方法
            boolean success = false;
            String message = "";

            switch (config.getDeviceType()) {
                case "PRINTER":
                    success = testPrinterConnection(config);
                    message = success ? "打印机连接成功" : "打印机连接失败";
                    break;
                case "SCANNER":
                    success = testScannerConnection(config);
                    message = success ? "扫码枪连接成功" : "扫码枪连接失败";
                    break;
                case "CAMERA":
                    success = testCameraConnection(config);
                    message = success ? "摄像头连接成功" : "摄像头连接失败";
                    break;
                case "KDS":
                    success = testKdsConnection(config);
                    message = success ? "KDS连接成功" : "KDS连接失败";
                    break;
                case "POS":
                    success = testPosConnection(config);
                    message = success ? "POS机连接成功" : "POS机连接失败";
                    break;
                case "DISPLAY":
                    success = testDisplayConnection(config);
                    message = success ? "客显屏连接成功" : "客显屏连接失败";
                    break;
                default:
                    success = testGenericConnection(config);
                    message = success ? "设备连接成功" : "设备连接失败";
                    break;
            }

            long responseTime = System.currentTimeMillis() - startTime;

            result.setSuccess(success);
            result.setMessage(message);
            result.setResponseTime(responseTime);

            if (success) {
                result.setDetails("设备响应时间: " + responseTime + "ms");
                log.info("设备连接测试成功: 设备名称={} 响应时间={}ms", config.getDeviceName(), responseTime);
            } else {
                result.setErrorMessage(message);
                log.error("设备连接测试失败: 设备名称={} IP={} 端口={}", 
                    config.getDeviceName(), config.getIpAddress(), config.getPort());
            }

        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            result.setSuccess(false);
            result.setMessage("连接测试异常");
            result.setErrorMessage(e.getMessage());
            result.setResponseTime(responseTime);
            log.error("设备连接测试异常: 设备名称={} 错误={}", config.getDeviceName(), e.getMessage(), e);
        }

        return result;
    }

    @Override
    public Boolean testScannerConnection(HardwareConfig config) {
        try {
            // 根据连接方式选择测试方法
            if ("SERIAL".equals(config.getConnectionType())) {
                return testSerialScanner(config);
            } else if ("USB".equals(config.getConnectionType())) {
                return testUsbScanner(config);
            } else if ("NETWORK".equals(config.getConnectionType())) {
                return testNetworkScanner(config);
            } else {
                log.warn("不支持的扫码枪连接方式: {}", config.getConnectionType());
                return false;
            }
        } catch (Exception e) {
            log.error("测试扫码枪连接失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Boolean testPrinterConnection(HardwareConfig config) {
        try {
            log.info("=== 开始测试打印机连接 ===");
            log.info("连接方式: {}", config.getConnectionType());
            log.info("IP地址: [{}]", config.getIpAddress());
            log.info("端口: [{}]", config.getPort());
            
            // 根据连接方式选择测试方法
            if ("NETWORK".equals(config.getConnectionType()) || "WIFI".equals(config.getConnectionType())) {
                String ip = config.getIpAddress();
                log.info("检测到网络/WIFI连接类型");
                
                if (ip == null || ip.trim().isEmpty()) {
                    log.error("IP地址为空，无法测试网络连接");
                    return false;
                }
                
                log.info("IP地址不为空，开始测试连接");
                
                // 检查端口是否是WSD标识符
                String port = config.getPort();
                boolean isWsdPort = port != null && port.startsWith("WSD-");
                
                if (isWsdPort) {
                    log.info("检测到WSD设备标识符: {}", port);
                    // 对于WSD设备，尝试两种连接方式
                    
                    // 方式1: 尝试WSD协议连接
                    if (testWsdPrinterConnection(ip, config)) {
                        log.info("WSD协议连接成功");
                        return true;
                    }
                    
                    // 方式2: 尝试Raw Socket连接（EPSON打印机通常支持9100端口）
                    log.info("WSD协议连接失败，尝试Raw Socket连接（端口9100）");
                    boolean socketResult = testSocketPrinterConnection(ip, 9100, config);
                    log.info("Raw Socket连接测试结果: {}", socketResult);
                    return socketResult;
                }
                
                // 尝试Socket连接
                int socketPort = port != null ? Integer.parseInt(port) : 9100;
                log.info("尝试Socket连接: IP={} 端口={}", ip, socketPort);
                boolean socketResult = testSocketPrinterConnection(ip, socketPort, config);
                log.info("Socket连接测试结果: {}", socketResult);
                return socketResult;
            } else if ("USB".equals(config.getConnectionType())) {
                log.info("检测到USB连接类型");
                return testUsbPrinter(config);
            } else if ("SERIAL".equals(config.getConnectionType())) {
                log.info("检测到串口连接类型");
                return testSerialPrinter(config);
            } else if ("BLUETOOTH".equals(config.getConnectionType())) {
                log.info("检测到蓝牙连接类型");
                return testBluetoothPrinter(config);
            } else {
                log.warn("未知的连接方式: {}，尝试打印队列连接", config.getConnectionType());
                // 尝试打印队列连接
                return testPrintQueueConnection(config);
            }
        } catch (Exception e) {
            log.error("测试打印机连接失败: 连接方式={} IP={} 端口={} 错误={}", 
                config.getConnectionType(), config.getIpAddress(), config.getPort(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Boolean testWsdPrinterConnection(String ip, HardwareConfig config) {
        try {
            log.info("测试WSD打印机连接: IP={}", ip);

            // 使用WSD协议测试连接
            CommunicationProtocolFactory.WsdProtocol wsdProtocol =
                CommunicationProtocolFactory.getWsdProtocol();

            boolean connected = wsdProtocol.connect(ip, 5357); // WSD默认端口

            if (connected) {
                log.info("WSD打印机连接成功: IP={}", ip);
                return true;
            } else {
                log.warn("WSD打印机连接失败: IP={}", ip);
                return false;
            }
        } catch (Exception e) {
            log.error("WSD打印机连接异常: IP={} 错误={}", ip, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Boolean testSocketPrinterConnection(String ip, int port, HardwareConfig config) {
        try {
            log.info("测试Socket打印机连接: IP={} 端口={}", ip, port);

            // 使用Socket协议测试连接
            CommunicationProtocolFactory.SocketProtocol socketProtocol =
                CommunicationProtocolFactory.getSocketProtocol();

            boolean connected = socketProtocol.connect(ip, port, CONNECTION_TIMEOUT);

            if (connected) {
                log.info("Socket打印机连接成功: IP={} 端口={}", ip, port);
                return true;
            } else {
                log.warn("Socket打印机连接失败: IP={} 端口={}", ip, port);
                return false;
            }
        } catch (Exception e) {
            log.error("Socket打印机连接异常: IP={} 端口={} 错误={}", ip, port, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Boolean testPrintQueueConnection(HardwareConfig config) {
        try {
            log.info("测试打印队列连接: 设备名称={}", config.getDeviceName());

            // 使用打印队列协议测试连接
            CommunicationProtocolFactory.PrintQueueProtocol printQueueProtocol =
                CommunicationProtocolFactory.getPrintQueueProtocol();

            boolean connected = printQueueProtocol.connect(config.getDeviceName());

            if (connected) {
                log.info("打印队列连接成功: 设备名称={}", config.getDeviceName());
                return true;
            } else {
                log.warn("打印队列连接失败: 设备名称={}", config.getDeviceName());
                return false;
            }
        } catch (Exception e) {
            log.error("打印队列连接异常: 设备名称={} 错误={}", config.getDeviceName(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Boolean testCameraConnection(HardwareConfig config) {
        try {
            log.info("测试摄像头连接: 设备名称={} IP={} 端口={}",
                    config.getDeviceName(), config.getIpAddress(), config.getPort());

            // 根据连接方式选择测试方法
            if ("NETWORK".equals(config.getConnectionType())) {
                return testNetworkCamera(config);
            } else if ("USB".equals(config.getConnectionType())) {
                return testUsbCamera(config);
            } else {
                log.warn("不支持的摄像头连接方式: {}", config.getConnectionType());
                return false;
            }
        } catch (Exception e) {
            log.error("测试摄像头连接失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Boolean sendWsdPrintCommand(String ip, String printData) {
        try {
            log.info("发送WSD打印命令: IP={} 数据长度={}", ip, printData.length());

            CommunicationProtocolFactory.WsdProtocol wsdProtocol =
                CommunicationProtocolFactory.getWsdProtocol();

            return wsdProtocol.sendCommand(ip, printData);
        } catch (Exception e) {
            log.error("发送WSD打印命令失败: IP={} 错误={}", ip, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Boolean sendSocketPrintCommand(String ip, int port, String printData) {
        try {
            log.info("发送Socket打印命令: IP={} 端口={} 数据长度={}", ip, port, printData.length());

            CommunicationProtocolFactory.SocketProtocol socketProtocol =
                CommunicationProtocolFactory.getSocketProtocol();

            return socketProtocol.sendData(ip, port, printData, CONNECTION_TIMEOUT);
        } catch (Exception e) {
            log.error("发送Socket打印命令失败: IP={} 端口={} 错误={}", ip, port, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 测试串口扫码枪连接
     */
    private boolean testSerialScanner(HardwareConfig config) {
        try {
            log.info("测试串口扫码枪连接: 端口={} 波特率={}",
                    config.getPort(), config.getBaudRate());

            CommunicationProtocolFactory.SerialProtocol serialProtocol =
                CommunicationProtocolFactory.getSerialProtocol();

            String port = config.getPort();
            int baudRate = config.getBaudRate() != null ?
                Integer.parseInt(config.getBaudRate()) : 9600;

            boolean connected = serialProtocol.connect(port, baudRate);

            if (connected) {
                log.info("串口扫码枪连接成功: 端口={}", port);
                return true;
            } else {
                log.warn("串口扫码枪连接失败: 端口={}", port);
                return false;
            }
        } catch (Exception e) {
            log.error("串口扫码枪连接异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 测试USB扫码枪连接
     */
    private boolean testUsbScanner(HardwareConfig config) {
        try {
            log.info("测试USB扫码枪连接");

            CommunicationProtocolFactory.UsbProtocol usbProtocol =
                CommunicationProtocolFactory.getUsbProtocol();

            boolean connected = usbProtocol.connectScanner(config.getDeviceModel());

            if (connected) {
                log.info("USB扫码枪连接成功");
                return true;
            } else {
                log.warn("USB扫码枪连接失败");
                return false;
            }
        } catch (Exception e) {
            log.error("USB扫码枪连接异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 测试网络扫码枪连接
     */
    private boolean testNetworkScanner(HardwareConfig config) {
        try {
            log.info("测试网络扫码枪连接: IP={} 端口={}",
                    config.getIpAddress(), config.getPort());

            CommunicationProtocolFactory.SocketProtocol socketProtocol =
                CommunicationProtocolFactory.getSocketProtocol();

            String ip = config.getIpAddress();
            int port = config.getPort() != null ? Integer.parseInt(config.getPort()) : 80;

            boolean connected = socketProtocol.connect(ip, port, CONNECTION_TIMEOUT);

            if (connected) {
                log.info("网络扫码枪连接成功: IP={} 端口={}", ip, port);
                return true;
            } else {
                log.warn("网络扫码枪连接失败: IP={} 端口={}", ip, port);
                return false;
            }
        } catch (Exception e) {
            log.error("网络扫码枪连接异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 测试USB打印机连接
     */
    private boolean testUsbPrinter(HardwareConfig config) {
        try {
            log.info("测试USB打印机连接");

            CommunicationProtocolFactory.UsbProtocol usbProtocol =
                CommunicationProtocolFactory.getUsbProtocol();

            boolean connected = usbProtocol.connectPrinter(config.getDeviceModel());

            if (connected) {
                log.info("USB打印机连接成功");
                return true;
            } else {
                log.warn("USB打印机连接失败");
                return false;
            }
        } catch (Exception e) {
            log.error("USB打印机连接异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 测试串口打印机连接
     */
    private boolean testSerialPrinter(HardwareConfig config) {
        try {
            log.info("测试串口打印机连接: 端口={} 波特率={}",
                    config.getPort(), config.getBaudRate());

            CommunicationProtocolFactory.SerialProtocol serialProtocol =
                CommunicationProtocolFactory.getSerialProtocol();

            String port = config.getPort();
            int baudRate = config.getBaudRate() != null ?
                Integer.parseInt(config.getBaudRate()) : 9600;

            boolean connected = serialProtocol.connect(port, baudRate);

            if (connected) {
                log.info("串口打印机连接成功: 端口={}", port);
                return true;
            } else {
                log.warn("串口打印机连接失败: 端口={}", port);
                return false;
            }
        } catch (Exception e) {
            log.error("串口打印机连接异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 测试蓝牙打印机连接
     */
    private boolean testBluetoothPrinter(HardwareConfig config) {
        try {
            log.info("测试蓝牙打印机连接");

            CommunicationProtocolFactory.BluetoothProtocol bluetoothProtocol =
                CommunicationProtocolFactory.getBluetoothProtocol();

            String macAddress = extractMacAddress(config.getConfigJson());

            boolean connected = bluetoothProtocol.connect(macAddress);

            if (connected) {
                log.info("蓝牙打印机连接成功");
                return true;
            } else {
                log.warn("蓝牙打印机连接失败");
                return false;
            }
        } catch (Exception e) {
            log.error("蓝牙打印机连接异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 测试网络摄像头连接
     */
    private boolean testNetworkCamera(HardwareConfig config) {
        try {
            log.info("测试网络摄像头连接: IP={} 端口={}",
                    config.getIpAddress(), config.getPort());

            CommunicationProtocolFactory.RtspProtocol rtspProtocol =
                CommunicationProtocolFactory.getRtspProtocol();

            String ip = config.getIpAddress();
            int port = config.getPort() != null ? Integer.parseInt(config.getPort()) : 554;

            boolean connected = rtspProtocol.connect(ip, port);

            if (connected) {
                log.info("网络摄像头连接成功: IP={} 端口={}", ip, port);
                return true;
            } else {
                log.warn("网络摄像头连接失败: IP={} 端口={}", ip, port);
                return false;
            }
        } catch (Exception e) {
            log.error("网络摄像头连接异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 测试USB摄像头连接
     */
    private boolean testUsbCamera(HardwareConfig config) {
        try {
            log.info("测试USB摄像头连接");

            CommunicationProtocolFactory.UsbProtocol usbProtocol =
                CommunicationProtocolFactory.getUsbProtocol();

            boolean connected = usbProtocol.connectCamera(config.getDeviceModel());

            if (connected) {
                log.info("USB摄像头连接成功");
                return true;
            } else {
                log.warn("USB摄像头连接失败");
                return false;
            }
        } catch (Exception e) {
            log.error("USB摄像头连接异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 测试KDS连接
     */
    private boolean testKdsConnection(HardwareConfig config) {
        try {
            log.info("测试KDS连接: IP={} 端口={}",
                    config.getIpAddress(), config.getPort());

            CommunicationProtocolFactory.SocketProtocol socketProtocol =
                CommunicationProtocolFactory.getSocketProtocol();

            String ip = config.getIpAddress();
            int port = config.getPort() != null ? Integer.parseInt(config.getPort()) : 8080;

            boolean connected = socketProtocol.connect(ip, port, CONNECTION_TIMEOUT);

            if (connected) {
                log.info("KDS连接成功: IP={} 端口={}", ip, port);
                return true;
            } else {
                log.warn("KDS连接失败: IP={} 端口={}", ip, port);
                return false;
            }
        } catch (Exception e) {
            log.error("KDS连接异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 测试POS机连接
     */
    private boolean testPosConnection(HardwareConfig config) {
        try {
            log.info("测试POS机连接: IP={} 端口={}",
                    config.getIpAddress(), config.getPort());

            CommunicationProtocolFactory.SocketProtocol socketProtocol =
                CommunicationProtocolFactory.getSocketProtocol();

            String ip = config.getIpAddress();
            int port = config.getPort() != null ? Integer.parseInt(config.getPort()) : 8080;

            boolean connected = socketProtocol.connect(ip, port, CONNECTION_TIMEOUT);

            if (connected) {
                log.info("POS机连接成功: IP={} 端口={}", ip, port);
                return true;
            } else {
                log.warn("POS机连接失败: IP={} 端口={}", ip, port);
                return false;
            }
        } catch (Exception e) {
            log.error("POS机连接异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 测试客显屏连接
     */
    private boolean testDisplayConnection(HardwareConfig config) {
        try {
            log.info("测试客显屏连接: IP={} 端口={}",
                    config.getIpAddress(), config.getPort());

            if ("SERIAL".equals(config.getConnectionType())) {
                CommunicationProtocolFactory.SerialProtocol serialProtocol =
                    CommunicationProtocolFactory.getSerialProtocol();

                String port = config.getPort();
                int baudRate = config.getBaudRate() != null ?
                    Integer.parseInt(config.getBaudRate()) : 9600;

                boolean connected = serialProtocol.connect(port, baudRate);

                if (connected) {
                    log.info("串口客显屏连接成功: 端口={}", port);
                    return true;
                }
            } else if ("NETWORK".equals(config.getConnectionType())) {
                CommunicationProtocolFactory.SocketProtocol socketProtocol =
                    CommunicationProtocolFactory.getSocketProtocol();

                String ip = config.getIpAddress();
                int port = config.getPort() != null ? Integer.parseInt(config.getPort()) : 5000;

                boolean connected = socketProtocol.connect(ip, port, CONNECTION_TIMEOUT);

                if (connected) {
                    log.info("网络客显屏连接成功: IP={} 端口={}", ip, port);
                    return true;
                }
            }

            log.warn("客显屏连接失败");
            return false;
        } catch (Exception e) {
            log.error("客显屏连接异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 测试通用设备连接
     */
    private boolean testGenericConnection(HardwareConfig config) {
        try {
            log.info("测试通用设备连接: 连接方式={}", config.getConnectionType());

            // 根据连接方式选择测试方法
            if ("NETWORK".equals(config.getConnectionType())) {
                CommunicationProtocolFactory.SocketProtocol socketProtocol =
                    CommunicationProtocolFactory.getSocketProtocol();

                String ip = config.getIpAddress();
                int port = config.getPort() != null ? Integer.parseInt(config.getPort()) : 80;

                return socketProtocol.connect(ip, port, CONNECTION_TIMEOUT);
            } else if ("SERIAL".equals(config.getConnectionType())) {
                CommunicationProtocolFactory.SerialProtocol serialProtocol =
                    CommunicationProtocolFactory.getSerialProtocol();

                String port = config.getPort();
                int baudRate = config.getBaudRate() != null ?
                    Integer.parseInt(config.getBaudRate()) : 9600;

                return serialProtocol.connect(port, baudRate);
            } else if ("USB".equals(config.getConnectionType())) {
                CommunicationProtocolFactory.UsbProtocol usbProtocol =
                    CommunicationProtocolFactory.getUsbProtocol();

                return usbProtocol.connect(config.getDeviceModel());
            } else if ("BLUETOOTH".equals(config.getConnectionType())) {
                CommunicationProtocolFactory.BluetoothProtocol bluetoothProtocol =
                    CommunicationProtocolFactory.getBluetoothProtocol();

                String macAddress = extractMacAddress(config.getConfigJson());
                return bluetoothProtocol.connect(macAddress);
            }

            log.warn("不支持的连接方式: {}", config.getConnectionType());
            return false;
        } catch (Exception e) {
            log.error("通用设备连接异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 从配置JSON中提取MAC地址
     */
    private String extractMacAddress(String configJson) {
        if (configJson == null || configJson.isEmpty()) {
            return null;
        }

        // 改进的MAC地址提取逻辑，避免匹配WSD设备标识符
        // MAC地址格式：XX:XX:XX:XX:XX:XX 或 XX-XX-XX-XX-XX-XX
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "\\b([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})\\b"
        );
        java.util.regex.Matcher matcher = pattern.matcher(configJson);

        if (matcher.find()) {
            String macAddress = matcher.group(0);
            // 确保不是WSD设备标识符（WSD-开头）
            if (!macAddress.startsWith("WSD-")) {
                return macAddress;
            }
        }

        return null;
    }
}
