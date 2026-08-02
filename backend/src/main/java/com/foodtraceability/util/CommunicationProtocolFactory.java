package com.foodtraceability.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通信协议工厂类
 * 负责创建和管理各种通信协议的实现
 */
public class CommunicationProtocolFactory {

    private static final Logger log = LoggerFactory.getLogger(CommunicationProtocolFactory.class);

    // 连接超时时间（毫秒）
    private static final int DEFAULT_TIMEOUT = 5000;

    // Socket连接池
    private static final Map<String, Socket> socketPool = new ConcurrentHashMap<>();

    // 串口连接池
    private static final Map<String, SerialPortWrapper> serialPortPool = new ConcurrentHashMap<>();

    /**
     * Socket协议实现类
     */
    public static class SocketProtocol {
        private static final Logger log = LoggerFactory.getLogger(SocketProtocol.class);

        /**
         * 连接Socket
         * @param host 主机地址
         * @param port 端口号
         * @param timeout 超时时间（毫秒）
         * @return 连接是否成功
         */
        public boolean connect(String host, int port, int timeout) {
            Socket socket = null;
            try {
                String key = host + ":" + port;

                // 检查连接池中是否已有连接
                if (socketPool.containsKey(key)) {
                    Socket existingSocket = socketPool.get(key);
                    if (!existingSocket.isClosed() && existingSocket.isConnected()) {
                        log.debug("使用现有Socket连接: {}", key);
                        return true;
                    }
                }

                // 创建新连接
                socket = new Socket();
                socket.connect(new InetSocketAddress(host, port), timeout);
                socket.setSoTimeout(timeout);
                socket.setKeepAlive(true);

                socketPool.put(key, socket);
                log.info("Socket连接成功: {}:{} 超时={}ms", host, port, timeout);
                return true;

            } catch (SocketTimeoutException e) {
                log.error("Socket连接超时: {}:{} 超时={}ms", host, port, timeout);
                return false;
            } catch (IOException e) {
                log.error("Socket连接失败: {}:{} 错误={}", host, port, e.getMessage(), e);
                return false;
            }
        }

        /**
         * 发送数据
         * @param host 主机地址
         * @param port 端口号
         * @param data 数据
         * @param timeout 超时时间（毫秒）
         * @return 发送是否成功
         */
        public boolean sendData(String host, int port, String data, int timeout) {
            String key = host + ":" + port;
            Socket socket = socketPool.get(key);

            if (socket == null || socket.isClosed()) {
                log.warn("Socket连接不存在或已关闭，尝试重新连接: {}", key);
                if (!connect(host, port, timeout)) {
                    return false;
                }
                socket = socketPool.get(key);
            }

            try {
                OutputStream outputStream = socket.getOutputStream();
                byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
                outputStream.write(bytes);
                outputStream.flush();
                log.info("发送数据成功: {}:{} 数据长度={} 字节数={}", host, port, data.length(), bytes.length);
                log.debug("发送的数据内容: {}", data);
                return true;
            } catch (IOException e) {
                log.error("发送数据失败: {}:{} 错误={}", host, port, e.getMessage(), e);
                return false;
            }
        }

        /**
         * 接收数据
         * @param host 主机地址
         * @param port 端口号
         * @return 接收到的数据
         */
        public String receiveData(String host, int port) {
            String key = host + ":" + port;
            Socket socket = socketPool.get(key);

            if (socket == null || socket.isClosed()) {
                log.warn("Socket连接不存在或已关闭: {}", key);
                return null;
            }

            try {
                InputStream inputStream = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                log.debug("接收数据成功: {}:{} 数据长度={}", host, port, response.length());
                return response.toString();
            } catch (IOException e) {
                log.error("接收数据失败: {}:{} 错误={}", host, port, e.getMessage(), e);
                return null;
            }
        }

        /**
         * 断开连接
         * @param host 主机地址
         * @param port 端口号
         */
        public void disconnect(String host, int port) {
            String key = host + ":" + port;
            Socket socket = socketPool.remove(key);

            if (socket != null) {
                try {
                    socket.close();
                    log.info("Socket连接已断开: {}", key);
                } catch (IOException e) {
                    log.error("断开Socket连接失败: {} 错误={}", key, e.getMessage(), e);
                }
            }
        }

        /**
         * 检查连接状态
         * @param host 主机地址
         * @param port 端口号
         * @return 连接状态
         */
        public boolean isConnected(String host, int port) {
            String key = host + ":" + port;
            Socket socket = socketPool.get(key);
            return socket != null && !socket.isClosed() && socket.isConnected();
        }
    }

    /**
     * 串口协议实现类
     */
    public static class SerialProtocol {
        private static final Logger log = LoggerFactory.getLogger(SerialProtocol.class);

        /**
         * 连接串口
         * @param portName 串口名称（如COM1、/dev/ttyUSB0）
         * @param baudRate 波特率
         * @return 连接是否成功
         */
        public boolean connect(String portName, int baudRate) {
            try {
                // 检查连接池中是否已有连接
                if (serialPortPool.containsKey(portName)) {
                    SerialPortWrapper existingPort = serialPortPool.get(portName);
                    if (existingPort != null && existingPort.isOpen()) {
                        log.debug("使用现有串口连接: {}", portName);
                        return true;
                    }
                }

                // 创建新连接（模拟实现，实际需要使用jSerialComm或RXTX库）
                SerialPortWrapper serialPort = new SerialPortWrapper(portName, baudRate);
                if (serialPort.open()) {
                    serialPortPool.put(portName, serialPort);
                    log.info("串口连接成功: {} 波特率={}", portName, baudRate);
                    return true;
                } else {
                    log.error("串口连接失败: {} 波特率={}", portName, baudRate);
                    return false;
                }

            } catch (Exception e) {
                log.error("串口连接异常: {} 波特率={} 错误={}", portName, baudRate, e.getMessage(), e);
                return false;
            }
        }

        /**
         * 发送数据
         * @param portName 串口名称
         * @param data 数据
         * @return 发送是否成功
         */
        public boolean sendData(String portName, String data) {
            SerialPortWrapper serialPort = serialPortPool.get(portName);

            if (serialPort == null || !serialPort.isOpen()) {
                log.warn("串口连接不存在或已关闭: {}", portName);
                return false;
            }

            try {
                serialPort.write(data);
                log.debug("发送串口数据成功: {} 数据长度={}", portName, data.length());
                return true;
            } catch (Exception e) {
                log.error("发送串口数据失败: {} 错误={}", portName, e.getMessage(), e);
                return false;
            }
        }

        /**
         * 接收数据
         * @param portName 串口名称
         * @return 接收到的数据
         */
        public String receiveData(String portName) {
            SerialPortWrapper serialPort = serialPortPool.get(portName);

            if (serialPort == null || !serialPort.isOpen()) {
                log.warn("串口连接不存在或已关闭: {}", portName);
                return null;
            }

            try {
                String data = serialPort.read();
                log.debug("接收串口数据成功: {} 数据长度={}", portName, data != null ? data.length() : 0);
                return data;
            } catch (Exception e) {
                log.error("接收串口数据失败: {} 错误={}", portName, e.getMessage(), e);
                return null;
            }
        }

        /**
         * 断开连接
         * @param portName 串口名称
         */
        public void disconnect(String portName) {
            SerialPortWrapper serialPort = serialPortPool.remove(portName);

            if (serialPort != null) {
                serialPort.close();
                log.info("串口连接已断开: {}", portName);
            }
        }

        /**
         * 检查连接状态
         * @param portName 串口名称
         * @return 连接状态
         */
        public boolean isConnected(String portName) {
            SerialPortWrapper serialPort = serialPortPool.get(portName);
            return serialPort != null && serialPort.isOpen();
        }
    }

    /**
     * WSD协议实现类（Web Services for Devices）
     */
    public static class WsdProtocol {
        private static final Logger log = LoggerFactory.getLogger(WsdProtocol.class);

        /**
         * 连接WSD设备
         * @param ip IP地址
         * @param port 端口号
         * @return 连接是否成功
         */
        public boolean connect(String ip, int port) {
            try {
                // WSD使用HTTP协议进行设备发现和控制
                String url = String.format("http://%s:%d/", ip, port);

                // 发送WSD发现请求
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(DEFAULT_TIMEOUT);
                connection.setReadTimeout(DEFAULT_TIMEOUT);

                // 设置WSD相关的HTTP头
                connection.setRequestProperty("Content-Type", "application/soap+xml; charset=utf-8");
                connection.setRequestProperty("User-Agent", "FoodTraceability-WSD-Client/1.0");
                connection.setRequestProperty("Accept", "*/*");

                int responseCode = connection.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK ||
                    responseCode == HttpURLConnection.HTTP_NOT_FOUND || // 404也可能表示设备在线
                    responseCode == HttpURLConnection.HTTP_FORBIDDEN || // 403可能表示需要认证
                    responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) { // 401可能表示需要认证
                    log.info("WSD设备连接成功: IP={} 端口={} 响应码={}", ip, port, responseCode);
                    connection.disconnect();
                    return true;
                } else {
                    log.warn("WSD设备连接失败: IP={} 端口={} 响应码={}", ip, port, responseCode);
                    connection.disconnect();
                    return false;
                }

            } catch (SocketTimeoutException e) {
                log.error("WSD设备连接超时: IP={} 端口={}", ip, port);
                return false;
            } catch (Exception e) {
                log.error("WSD设备连接异常: IP={} 端口={} 错误={}", ip, port, e.getMessage(), e);
                return false;
            }
        }

        /**
         * 发送WSD命令
         * @param ip IP地址
         * @param command 命令数据
         * @return 发送是否成功
         */
        public boolean sendCommand(String ip, String command) {
            try {
                String url = String.format("http://%s:5357/", ip);

                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(DEFAULT_TIMEOUT);
                connection.setReadTimeout(DEFAULT_TIMEOUT);
                connection.setDoOutput(true);

                // 设置WSD相关的HTTP头
                connection.setRequestProperty("Content-Type", "application/soap+xml");
                connection.setRequestProperty("User-Agent", "FoodTraceability-WSD-Client/1.0");

                // 发送命令
                OutputStream outputStream = connection.getOutputStream();
                outputStream.write(command.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();

                int responseCode = connection.getResponseCode();

                connection.disconnect();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    log.info("WSD命令发送成功: IP={}", ip);
                    return true;
                } else {
                    log.warn("WSD命令发送失败: IP={} 响应码={}", ip, responseCode);
                    return false;
                }

            } catch (Exception e) {
                log.error("发送WSD命令异常: IP={} 错误={}", ip, e.getMessage(), e);
                return false;
            }
        }
    }

    /**
     * RTSP协议实现类（Real Time Streaming Protocol）
     */
    public static class RtspProtocol {
        private static final Logger log = LoggerFactory.getLogger(RtspProtocol.class);

        /**
         * 连接RTSP设备（摄像头）
         * @param ip IP地址
         * @param port 端口号
         * @return 连接是否成功
         */
        public boolean connect(String ip, int port) {
            try {
                String url = String.format("rtsp://%s:%d/", ip, port);

                // 发送RTSP OPTIONS请求
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(ip, port), DEFAULT_TIMEOUT);

                OutputStream outputStream = socket.getOutputStream();
                String request = String.format("OPTIONS rtsp://%s:%d/ RTSP/1.0\r\n" +
                        "CSeq: 1\r\n" +
                        "User-Agent: FoodTraceability-RTSP-Client/1.0\r\n\r\n", ip, port);

                outputStream.write(request.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();

                // 读取响应
                InputStream inputStream = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                String responseLine = reader.readLine();

                socket.close();

                if (responseLine != null && responseLine.startsWith("RTSP/1.0 200")) {
                    log.info("RTSP设备连接成功: IP={} 端口={}", ip, port);
                    return true;
                } else {
                    log.warn("RTSP设备连接失败: IP={} 端口={} 响应={}", ip, port, responseLine);
                    return false;
                }

            } catch (Exception e) {
                log.error("RTSP设备连接异常: IP={} 端口={} 错误={}", ip, port, e.getMessage(), e);
                return false;
            }
        }
    }

    /**
     * USB协议实现类
     */
    public static class UsbProtocol {
        private static final Logger log = LoggerFactory.getLogger(UsbProtocol.class);

        /**
         * 连接USB设备
         * @param deviceModel 设备型号
         * @return 连接是否成功
         */
        public boolean connect(String deviceModel) {
            try {
                // 模拟USB设备连接
                // 实际实现需要使用javax.usb或jUSB库
                log.info("USB设备连接成功: 型号={}", deviceModel);
                return true;
            } catch (Exception e) {
                log.error("USB设备连接异常: 型号={} 错误={}", deviceModel, e.getMessage(), e);
                return false;
            }
        }

        /**
         * 连接USB打印机
         * @param deviceModel 设备型号
         * @return 连接是否成功
         */
        public boolean connectPrinter(String deviceModel) {
            try {
                log.info("USB打印机连接成功: 型号={}", deviceModel);
                return true;
            } catch (Exception e) {
                log.error("USB打印机连接异常: 型号={} 错误={}", deviceModel, e.getMessage(), e);
                return false;
            }
        }

        /**
         * 连接USB扫码枪
         * @param deviceModel 设备型号
         * @return 连接是否成功
         */
        public boolean connectScanner(String deviceModel) {
            try {
                log.info("USB扫码枪连接成功: 型号={}", deviceModel);
                return true;
            } catch (Exception e) {
                log.error("USB扫码枪连接异常: 型号={} 错误={}", deviceModel, e.getMessage(), e);
                return false;
            }
        }

        /**
         * 连接USB摄像头
         * @param deviceModel 设备型号
         * @return 连接是否成功
         */
        public boolean connectCamera(String deviceModel) {
            try {
                log.info("USB摄像头连接成功: 型号={}", deviceModel);
                return true;
            } catch (Exception e) {
                log.error("USB摄像头连接异常: 型号={} 错误={}", deviceModel, e.getMessage(), e);
                return false;
            }
        }
    }

    /**
     * 蓝牙协议实现类
     */
    public static class BluetoothProtocol {
        private static final Logger log = LoggerFactory.getLogger(BluetoothProtocol.class);

        /**
         * 连接蓝牙设备
         * @param macAddress MAC地址
         * @return 连接是否成功
         */
        public boolean connect(String macAddress) {
            try {
                // 模拟蓝牙设备连接
                // 实际实现需要使用BlueCove或其他蓝牙库
                log.info("蓝牙设备连接成功: MAC={}", macAddress);
                return true;
            } catch (Exception e) {
                log.error("蓝牙设备连接异常: MAC={} 错误={}", macAddress, e.getMessage(), e);
                return false;
            }
        }
    }

    /**
     * 打印队列协议实现类
     */
    public static class PrintQueueProtocol {
        private static final Logger log = LoggerFactory.getLogger(PrintQueueProtocol.class);

        /**
         * 连接打印队列
         * @param printerName 打印机名称
         * @return 连接是否成功
         */
        public boolean connect(String printerName) {
            try {
                // 模拟打印队列连接
                // 实际实现需要使用系统打印服务API
                log.info("打印队列连接成功: 打印机={}", printerName);
                return true;
            } catch (Exception e) {
                log.error("打印队列连接异常: 打印机={} 错误={}", printerName, e.getMessage(), e);
                return false;
            }
        }
    }

    /**
     * 串口包装类（模拟实现）
     */
    private static class SerialPortWrapper {
        private final String portName;
        private final int baudRate;
        private boolean open;

        public SerialPortWrapper(String portName, int baudRate) {
            this.portName = portName;
            this.baudRate = baudRate;
            this.open = false;
        }

        public boolean open() {
            // 模拟打开串口
            this.open = true;
            return true;
        }

        public void close() {
            this.open = false;
        }

        public boolean isOpen() {
            return open;
        }

        public void write(String data) {
            // 模拟写入数据
        }

        public String read() {
            // 模拟读取数据
            return "";
        }
    }

    // ========== 工厂方法 ==========

    /**
     * 获取Socket协议实例
     */
    public static SocketProtocol getSocketProtocol() {
        return new SocketProtocol();
    }

    /**
     * 获取串口协议实例
     */
    public static SerialProtocol getSerialProtocol() {
        return new SerialProtocol();
    }

    /**
     * 获取WSD协议实例
     */
    public static WsdProtocol getWsdProtocol() {
        return new WsdProtocol();
    }

    /**
     * 获取RTSP协议实例
     */
    public static RtspProtocol getRtspProtocol() {
        return new RtspProtocol();
    }

    /**
     * 获取USB协议实例
     */
    public static UsbProtocol getUsbProtocol() {
        return new UsbProtocol();
    }

    /**
     * 获取蓝牙协议实例
     */
    public static BluetoothProtocol getBluetoothProtocol() {
        return new BluetoothProtocol();
    }

    /**
     * 获取打印队列协议实例
     */
    public static PrintQueueProtocol getPrintQueueProtocol() {
        return new PrintQueueProtocol();
    }

    /**
     * 清理所有连接
     */
    public static void cleanup() {
        // 清理Socket连接
        for (Map.Entry<String, Socket> entry : socketPool.entrySet()) {
            try {
                entry.getValue().close();
            } catch (IOException e) {
                log.error("关闭Socket连接失败: {} 错误={}", entry.getKey(), e.getMessage());
            }
        }
        socketPool.clear();

        // 清理串口连接
        for (Map.Entry<String, SerialPortWrapper> entry : serialPortPool.entrySet()) {
            entry.getValue().close();
        }
        serialPortPool.clear();

        log.info("所有通信连接已清理");
    }
}
