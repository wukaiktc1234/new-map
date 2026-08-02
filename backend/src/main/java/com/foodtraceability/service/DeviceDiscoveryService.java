package com.foodtraceability.service;

import com.foodtraceability.entity.HardwareConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 设备发现服务
 * 负责自动发现网络中的设备
 */
@Service
public class DeviceDiscoveryService {
    
    private static final Logger log = LoggerFactory.getLogger(DeviceDiscoveryService.class);
    
    // 设备发现线程池
    private final ExecutorService discoveryExecutor;
    
    // 设备发现超时时间（毫秒）
    private static final int DISCOVERY_TIMEOUT = 5000;
    
    // 设备发现端口
    private static final int DISCOVERY_PORT = 1900;
    
    // 设备发现组播地址
    private static final String MULTICAST_ADDRESS = "239.255.255.250";
    
    // 设备发现状态
    private final AtomicBoolean isDiscovering;

    // 设备配置服务
    private final HardwareConfigService hardwareConfigService;

    // 设备标识管理器
    private final DeviceIdentificationManager deviceIdentificationManager;

    /**
     * 构造函数注入（禁止 @Autowired 字段注入）
     * @param hardwareConfigService 设备配置服务
     * @param deviceIdentificationManager 设备标识管理器
     */
    public DeviceDiscoveryService(HardwareConfigService hardwareConfigService, DeviceIdentificationManager deviceIdentificationManager) {
        this.hardwareConfigService = hardwareConfigService;
        this.deviceIdentificationManager = deviceIdentificationManager;
        this.discoveryExecutor = Executors.newFixedThreadPool(
                Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
                r -> {
                    Thread thread = new Thread(r, "device-discovery-");
                    thread.setDaemon(true);
                    return thread;
                }
        );
        this.isDiscovering = new AtomicBoolean(false);
        log.info("设备发现服务初始化完成");
    }
    
    /**
     * 开始设备自动发现
     */
    public void startDiscovery() {
        if (isDiscovering.compareAndSet(false, true)) {
            log.info("开始设备自动发现");
            discoveryExecutor.submit(this::discoverDevices);
        } else {
            log.warn("设备发现已在运行中");
        }
    }
    
    /**
     * 停止设备自动发现
     */
    public void stopDiscovery() {
        if (isDiscovering.compareAndSet(true, false)) {
            log.info("停止设备自动发现");
        }
    }
    
    /**
     * 设备发现实现
     */
    private void discoverDevices() {
        MulticastSocket socket = null;
        try {
            // 创建组播套接字
            socket = new MulticastSocket(DISCOVERY_PORT);
            socket.setSoTimeout(DISCOVERY_TIMEOUT);
            
            // 加入组播组
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(group);
            
            // 发送设备发现请求
            String discoveryMessage = "M-SEARCH * HTTP/1.1\r\n" +
                    "HOST: 239.255.255.250:1900\r\n" +
                    "MAN: \"ssdp:discover\"\r\n" +
                    "MX: 3\r\n" +
                    "ST: ssdp:all\r\n\r\n";
            
            DatagramPacket sendPacket = new DatagramPacket(
                    discoveryMessage.getBytes(StandardCharsets.UTF_8),
                    discoveryMessage.length(),
                    group,
                    DISCOVERY_PORT
            );
            socket.send(sendPacket);
            
            log.info("发送设备发现请求");
            
            // 接收设备响应
            byte[] buffer = new byte[4096];
            List<HardwareConfig> discoveredDevices = new ArrayList<>();
            
            while (isDiscovering.get()) {
                try {
                    DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                    socket.receive(receivePacket);
                    
                    String response = new String(receivePacket.getData(), 0, receivePacket.getLength(), StandardCharsets.UTF_8);
                    log.debug("收到设备响应: {}", response);
                    
                    // 解析设备响应
                    HardwareConfig deviceConfig = parseDeviceResponse(response, receivePacket.getAddress());
                    if (deviceConfig != null) {
                        discoveredDevices.add(deviceConfig);
                        log.info("发现设备: {} 类型: {} IP: {}", 
                                deviceConfig.getDeviceName(), 
                                deviceConfig.getDeviceType(), 
                                deviceConfig.getIpAddress());
                    }
                } catch (SocketTimeoutException e) {
                    // 超时继续，直到停止发现
                    continue;
                }
            }
            
            // 处理发现的设备
            processDiscoveredDevices(discoveredDevices);
            
        } catch (IOException e) {
            log.error("设备发现异常: {}", e.getMessage(), e);
        } finally {
            if (socket != null) {
                try {
                    socket.leaveGroup(InetAddress.getByName(MULTICAST_ADDRESS));
                    socket.close();
                } catch (IOException e) {
                    log.error("关闭发现套接字异常: {}", e.getMessage(), e);
                }
            }
            isDiscovering.set(false);
            log.info("设备发现结束");
        }
    }
    
    /**
     * 解析设备响应
     * @param response 设备响应
     * @param deviceAddress 设备地址
     * @return 设备配置
     */
    private HardwareConfig parseDeviceResponse(String response, InetAddress deviceAddress) {
        // 简单的设备响应解析，实际项目中应使用更复杂的解析逻辑
        if (response.contains("UPnP") || response.contains("printer") || response.contains("scanner")) {
            HardwareConfig config = new HardwareConfig();
            config.setIpAddress(deviceAddress.getHostAddress());
            config.setConnectionType("NETWORK");
            
            // 解析设备类型
            if (response.contains("printer")) {
                config.setDeviceType("PRINTER");
                config.setDeviceName("自动发现打印机");
                config.setDeviceModel("Unknown Printer");
                config.setPort("9100");
            } else if (response.contains("scanner")) {
                config.setDeviceType("SCANNER");
                config.setDeviceName("自动发现扫码器");
                config.setDeviceModel("Unknown Scanner");
                config.setPort("5555");
            } else {
                config.setDeviceType("UNKNOWN");
                config.setDeviceName("未知设备");
                config.setDeviceModel("Unknown Device");
            }
            
            config.setStatus(0); // 未连接状态
            config.setConfigJson("{}");
            config.setStoreId(1L); // 默认门店ID
            config.setCreatedBy("system");
            config.setUpdatedBy("system");
            config.setCreatedAt(new java.util.Date());
            config.setUpdatedAt(new java.util.Date());
            
            return config;
        }
        return null;
    }
    
    /**
     * 处理发现的设备
     * @param discoveredDevices 发现的设备列表
     */
    private void processDiscoveredDevices(List<HardwareConfig> discoveredDevices) {
        for (HardwareConfig device : discoveredDevices) {
            // 检查设备是否已存在
            String deviceId = deviceIdentificationManager.generateDeviceId(device);
            if (!deviceIdentificationManager.isDeviceIdRegistered(deviceId)) {
                // 保存设备配置
                hardwareConfigService.saveConfig(device, null);
                // 注册设备标识
                deviceIdentificationManager.registerDeviceId(deviceId, device);
                log.info("保存新发现的设备: {} ID: {}", device.getDeviceName(), device.getId());
            }
        }
    }
    
    /**
     * 获取当前发现状态
     * @return 发现状态
     */
    public boolean isDiscovering() {
        return isDiscovering.get();
    }
    
    /**
     * 关闭设备发现服务
     */
    public void shutdown() {
        stopDiscovery();
        discoveryExecutor.shutdown();
        try {
            if (!discoveryExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                discoveryExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            discoveryExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("设备发现服务已关闭");
    }
}