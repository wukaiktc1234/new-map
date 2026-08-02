package com.foodtraceability.simulator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 设备模拟器
 * 用于在没有实际硬件的情况下测试设备通信功能
 * 支持模拟打印机、扫码枪、电子秤、摄像头等设备
 *
 * 安全说明：仅在开发/测试环境（dev/test profile）激活，生产环境（prod）不会加载此 Bean。
 */
@Component
@Profile({"dev", "test"})
public class DeviceSimulator {

    private static final Logger log = LoggerFactory.getLogger(DeviceSimulator.class);

    // 模拟设备配置
    private final Map<String, SimulatedDevice> simulatedDevices = new ConcurrentHashMap<>();

    // 线程池用于处理并发请求
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    // 模拟服务状态
    private volatile boolean running = false;

    // 模拟服务端口
    private static final int PRINTER_PORT = 9100;
    private static final int SCANNER_PORT = 9101;
    private static final int SCALE_PORT = 9102;
    private static final int KDS_PORT = 8080;
    private static final int CAMERA_PORT = 554;

    /**
     * 启动所有模拟设备服务
     */
    public void startAllSimulators() {
        if (running) {
            log.warn("设备模拟器已在运行中");
            return;
        }

        running = true;
        log.info("========== 启动设备模拟器 ==========");

        // 启动打印机模拟器
        startPrinterSimulator(PRINTER_PORT);

        // 启动扫码枪模拟器
        startScannerSimulator(SCANNER_PORT);

        // 启动电子秤模拟器
        startScaleSimulator(SCALE_PORT);

        // 启动KDS模拟器
        startKdsSimulator(KDS_PORT);

        log.info("设备模拟器启动完成，共启动 {} 个模拟设备", simulatedDevices.size());
        log.info("====================================");
    }

    /**
     * 停止所有模拟设备服务
     */
    public void stopAllSimulators() {
        running = false;
        log.info("开始停止设备模拟器...");

        for (SimulatedDevice device : simulatedDevices.values()) {
            try {
                device.stop();
            } catch (Exception e) {
                log.error("停止模拟设备失败: {}", device.getName(), e);
            }
        }

        simulatedDevices.clear();
        executorService.shutdown();
        log.info("设备模拟器已停止");
    }

    /**
     * 启动打印机模拟器
     */
    public void startPrinterSimulator(int port) {
        String deviceName = "SimulatedPrinter-" + port;
        
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            SimulatedDevice device = new SimulatedDevice(
                deviceName,
                "PRINTER",
                "localhost",
                port,
                () -> handlePrinterConnection(serverSocket)
            );
            
            simulatedDevices.put(deviceName, device);
            executorService.submit(device);
            
            log.info("打印机模拟器启动成功: 端口={}", port);
        } catch (IOException e) {
            log.error("打印机模拟器启动失败: 端口={} 错误={}", port, e.getMessage());
        }
    }

    /**
     * 启动扫码枪模拟器
     */
    public void startScannerSimulator(int port) {
        String deviceName = "SimulatedScanner-" + port;
        
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            SimulatedDevice device = new SimulatedDevice(
                deviceName,
                "SCANNER",
                "localhost",
                port,
                () -> handleScannerConnection(serverSocket)
            );
            
            simulatedDevices.put(deviceName, device);
            executorService.submit(device);
            
            log.info("扫码枪模拟器启动成功: 端口={}", port);
        } catch (IOException e) {
            log.error("扫码枪模拟器启动失败: 端口={} 错误={}", port, e.getMessage());
        }
    }

    /**
     * 启动电子秤模拟器
     */
    public void startScaleSimulator(int port) {
        String deviceName = "SimulatedScale-" + port;
        
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            SimulatedDevice device = new SimulatedDevice(
                deviceName,
                "SCALE",
                "localhost",
                port,
                () -> handleScaleConnection(serverSocket)
            );
            
            simulatedDevices.put(deviceName, device);
            executorService.submit(device);
            
            log.info("电子秤模拟器启动成功: 端口={}", port);
        } catch (IOException e) {
            log.error("电子秤模拟器启动失败: 端口={} 错误={}", port, e.getMessage());
        }
    }

    /**
     * 启动KDS模拟器
     */
    public void startKdsSimulator(int port) {
        String deviceName = "SimulatedKDS-" + port;
        
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            SimulatedDevice device = new SimulatedDevice(
                deviceName,
                "KDS",
                "localhost",
                port,
                () -> handleKdsConnection(serverSocket)
            );
            
            simulatedDevices.put(deviceName, device);
            executorService.submit(device);
            
            log.info("KDS模拟器启动成功: 端口={}", port);
        } catch (IOException e) {
            log.error("KDS模拟器启动失败: 端口={} 错误={}", port, e.getMessage());
        }
    }

    /**
     * 处理打印机连接
     */
    private void handlePrinterConnection(ServerSocket serverSocket) {
        while (running && !serverSocket.isClosed()) {
            try (Socket clientSocket = serverSocket.accept();
                 InputStream in = clientSocket.getInputStream()) {

                log.info("打印机模拟器接收到连接: {}", clientSocket.getInetAddress());

                // 读取打印数据
                byte[] buffer = new byte[4096];
                int bytesRead;
                StringBuilder printData = new StringBuilder();

                while ((bytesRead = in.read(buffer)) != -1) {
                    printData.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
                }

                log.info("========== 模拟打印内容 ==========");
                log.info("数据长度: {} 字节", printData.length());
                log.info("打印内容:\n{}", printData.toString());
                log.info("================================");

                // 模拟打印成功响应
                log.info("模拟打印完成");

            } catch (IOException e) {
                if (running) {
                    log.error("打印机模拟器连接处理异常: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * 处理扫码枪连接
     */
    private void handleScannerConnection(ServerSocket serverSocket) {
        while (running && !serverSocket.isClosed()) {
            try (Socket clientSocket = serverSocket.accept();
                 OutputStream out = clientSocket.getOutputStream();
                 BufferedReader in = new BufferedReader(
                     new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8))) {

                log.info("扫码枪模拟器接收到连接: {}", clientSocket.getInetAddress());

                // 读取请求
                String request = in.readLine();
                log.info("扫码枪模拟器收到请求: {}", request);

                // 模拟扫码结果
                String scanResult;
                if (request != null && request.contains("QRCODE")) {
                    // 模拟二维码扫描结果
                    scanResult = generateSimulatedQRCode();
                } else {
                    // 模拟条形码扫描结果
                    scanResult = generateSimulatedBarcode();
                }

                // 发送扫描结果
                out.write(scanResult.getBytes(StandardCharsets.UTF_8));
                out.flush();

                log.info("扫码枪模拟器返回结果: {}", scanResult);

            } catch (IOException e) {
                if (running) {
                    log.error("扫码枪模拟器连接处理异常: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * 处理电子秤连接
     */
    private void handleScaleConnection(ServerSocket serverSocket) {
        while (running && !serverSocket.isClosed()) {
            try (Socket clientSocket = serverSocket.accept();
                 OutputStream out = clientSocket.getOutputStream();
                 BufferedReader in = new BufferedReader(
                     new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8))) {

                log.info("电子秤模拟器接收到连接: {}", clientSocket.getInetAddress());

                // 读取请求
                String request = in.readLine();
                log.info("电子秤模拟器收到请求: {}", request);

                // 模拟称重结果
                String weightResult = generateSimulatedWeight();

                // 发送称重结果
                out.write(weightResult.getBytes(StandardCharsets.UTF_8));
                out.flush();

                log.info("电子秤模拟器返回结果: {}", weightResult);

            } catch (IOException e) {
                if (running) {
                    log.error("电子秤模拟器连接处理异常: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * 处理KDS连接
     */
    private void handleKdsConnection(ServerSocket serverSocket) {
        while (running && !serverSocket.isClosed()) {
            try (Socket clientSocket = serverSocket.accept();
                 OutputStream out = clientSocket.getOutputStream();
                 BufferedReader in = new BufferedReader(
                     new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8))) {

                log.info("KDS模拟器接收到连接: {}", clientSocket.getInetAddress());

                // 读取订单数据
                StringBuilder orderData = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null && !line.isEmpty()) {
                    orderData.append(line);
                }

                log.info("KDS模拟器收到订单: {}", orderData);

                // 发送确认响应
                String response = "{\"success\":true,\"message\":\"订单已接收\",\"timestamp\":" + 
                    System.currentTimeMillis() + "}";
                out.write(response.getBytes(StandardCharsets.UTF_8));
                out.flush();

                log.info("KDS模拟器返回确认: {}", response);

            } catch (IOException e) {
                if (running) {
                    log.error("KDS模拟器连接处理异常: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * 生成模拟条形码
     */
    private String generateSimulatedBarcode() {
        // 生成13位EAN-13条形码
        Random random = new Random();
        StringBuilder barcode = new StringBuilder();
        for (int i = 0; i < 13; i++) {
            barcode.append(random.nextInt(10));
        }
        return barcode.toString();
    }

    /**
     * 生成模拟二维码
     */
    private String generateSimulatedQRCode() {
        // 生成追溯码二维码
        String traceCode = "TR" + System.currentTimeMillis();
        return "https://trace.example.com/product/" + traceCode;
    }

    /**
     * 生成模拟重量
     */
    private String generateSimulatedWeight() {
        // 生成随机重量（0.000 - 10.000 kg）
        Random random = new Random();
        double weight = random.nextDouble() * 10.0;
        return String.format("W:%.3fkg", weight);
    }

    /**
     * 获取模拟设备状态
     */
    public Map<String, SimulatedDevice> getSimulatedDevices() {
        return new HashMap<>(simulatedDevices);
    }

    /**
     * 检查模拟器是否运行中
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * 模拟设备类
     */
    public static class SimulatedDevice implements Runnable {
        private final String name;
        private final String type;
        private final String host;
        private final int port;
        private final Runnable handler;
        private volatile boolean stopped = false;

        public SimulatedDevice(String name, String type, String host, int port, Runnable handler) {
            this.name = name;
            this.type = type;
            this.host = host;
            this.port = port;
            this.handler = handler;
        }

        @Override
        public void run() {
            log.info("模拟设备启动: {} ({}://{}:{})", name, type, host, port);
            handler.run();
        }

        public void stop() {
            stopped = true;
            log.info("模拟设备停止: {}", name);
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public boolean isStopped() {
            return stopped;
        }
    }
}
