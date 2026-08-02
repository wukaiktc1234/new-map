package com.foodtraceability.service.impl;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.DeviceQueryDTO;
import com.foodtraceability.dto.DeviceVO;
import com.foodtraceability.entity.Device;
import com.foodtraceability.entity.DeviceStatus;
import com.foodtraceability.entity.HardwareConfig;
import com.foodtraceability.entity.TestResult;
import com.foodtraceability.mapper.DeviceMapper;
import com.foodtraceability.service.DeviceConnectionService;
import com.foodtraceability.service.DeviceService;
import com.foodtraceability.service.DeviceStatusMonitorService;
import com.foodtraceability.util.CommunicationProtocolFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 设备状态监控服务实现类
 * 负责监控设备状态变化并实时推送到前端
 */
@Service
public class DeviceStatusMonitorServiceImpl implements DeviceStatusMonitorService {

    private static final Logger log = LoggerFactory.getLogger(DeviceStatusMonitorServiceImpl.class);

    // 监控状态
    private final AtomicBoolean monitoring = new AtomicBoolean(false);

    // 设备状态缓存
    private final Map<Long, DeviceStatus> deviceStatusCache = new ConcurrentHashMap<>();

    // 设备配置缓存
    private final Map<Long, HardwareConfig> deviceConfigCache = new ConcurrentHashMap<>();

    // WebSocket消息模板
    private final SimpMessagingTemplate messagingTemplate;

    // 设备服务
    private final DeviceService deviceService;

    // 设备连接服务
    private final DeviceConnectionService deviceConnectionService;

    // 设备Mapper
    private final DeviceMapper deviceMapper;

    // JSON解析器
    private final ObjectMapper objectMapper;

    /**
     * 构造函数注入（禁止 @Autowired 字段注入）
     * @param deviceService 设备服务
     * @param deviceConnectionService 设备连接服务
     * @param deviceMapper 设备Mapper
     * @param objectMapper JSON解析器
     * @param messagingTemplate WebSocket消息模板，可选依赖
     */
    public DeviceStatusMonitorServiceImpl(DeviceService deviceService, DeviceConnectionService deviceConnectionService, DeviceMapper deviceMapper, ObjectMapper objectMapper, @Nullable SimpMessagingTemplate messagingTemplate) {
        this.deviceService = deviceService;
        this.deviceConnectionService = deviceConnectionService;
        this.deviceMapper = deviceMapper;
        this.objectMapper = objectMapper;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 启动设备状态监控
     */
    @Override
    public void startMonitoring() {
        if (monitoring.compareAndSet(false, true)) {
            log.info("设备状态监控已启动");

            // 加载所有在线设备到监控列表
            loadAllDevicesToMonitor();
        } else {
            log.warn("设备状态监控已在运行中");
        }
    }

    /**
     * 停止设备状态监控
     */
    @Override
    public void stopMonitoring() {
        if (monitoring.compareAndSet(true, false)) {
            log.info("设备状态监控已停止");

            // 清空缓存
            deviceStatusCache.clear();
            deviceConfigCache.clear();
        } else {
            log.warn("设备状态监控未在运行");
        }
    }

    /**
     * 添加设备到监控列表
     * @param deviceId 设备ID
     */
    @Override
    public void addDeviceToMonitor(Long deviceId) {
        try {
            // 获取设备信息（使用getById获取完整的Device实体以获得连接参数）
            Device device = deviceService.getById(deviceId);
            if (device == null) {
                log.warn("设备不存在: deviceId={}", deviceId);
                return;
            }

            // 转换为硬件配置
            HardwareConfig config = convertToHardwareConfig(device);
            deviceConfigCache.put(deviceId, config);

            // 初始化设备状态
            DeviceStatus status = new DeviceStatus();
            status.setDeviceId(deviceId);
            status.setDeviceName(device.getDeviceName());
            status.setDeviceType(device.getDeviceType() != null ? String.valueOf(device.getDeviceType()) : null);
            status.setIpAddress(device.getConnectionConfig());
            status.setOnline(false);
            status.setLastCheckTime(new Date());

            deviceStatusCache.put(deviceId, status);

            log.info("设备已添加到监控列表: deviceId={} deviceName={}", deviceId, device.getDeviceName());

        } catch (Exception e) {
            log.error("添加设备到监控列表失败: deviceId={} 错误={}", deviceId, e.getMessage(), e);
        }
    }

    /**
     * 从监控列表中移除设备
     * @param deviceId 设备ID
     */
    @Override
    public void removeDeviceFromMonitor(Long deviceId) {
        deviceStatusCache.remove(deviceId);
        deviceConfigCache.remove(deviceId);
        log.info("设备已从监控列表移除: deviceId={}", deviceId);
    }

    /**
     * 获取设备当前状态
     * @param deviceId 设备ID
     * @return 设备状态
     */
    @Override
    public DeviceStatus getDeviceStatus(Long deviceId) {
        return deviceStatusCache.get(deviceId);
    }

    /**
     * 获取所有设备状态
     * @return 设备状态映射
     */
    @Override
    public Map<Long, DeviceStatus> getAllDeviceStatus() {
        return new HashMap<>(deviceStatusCache);
    }

    /**
     * 手动刷新设备状态
     * @param deviceId 设备ID
     * @return 设备状态
     */
    @Override
    @Transactional(readOnly = true)
    public DeviceStatus refreshDeviceStatus(Long deviceId) {
        try {
            HardwareConfig config = deviceConfigCache.get(deviceId);
            if (config == null) {
                log.warn("设备配置不存在: deviceId={}", deviceId);
                return null;
            }

            // 测试设备连接
            boolean connected = testDeviceConnection(config);

            // 更新设备状态
            DeviceStatus status = deviceStatusCache.get(deviceId);
            if (status != null) {
                boolean oldStatus = status.isOnline();
                status.setOnline(connected);
                status.setLastCheckTime(new Date());

                // 如果状态发生变化，推送通知
                if (oldStatus != connected) {
                    sendDeviceStatusNotification(deviceId, status);
                    // 更新数据库中的设备状态
                    updateDeviceStatusInDatabase(deviceId, connected);
                }
            }

            return status;

        } catch (Exception e) {
            log.error("刷新设备状态失败: deviceId={} 错误={}", deviceId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 批量刷新设备状态
     * @param deviceIds 设备ID列表
     * @return 设备状态映射
     */
    @Override
    public Map<Long, DeviceStatus> batchRefreshDeviceStatus(List<Long> deviceIds) {
        Map<Long, DeviceStatus> resultMap = new HashMap<>();

        for (Long deviceId : deviceIds) {
            DeviceStatus status = refreshDeviceStatus(deviceId);
            if (status != null) {
                resultMap.put(deviceId, status);
            }
        }

        return resultMap;
    }

    /**
     * 测试设备连接
     * @param config 设备配置
     * @return 测试结果
     */
    @Override
    public boolean testDeviceConnection(HardwareConfig config) {
        try {
            TestResult result = deviceConnectionService.testConnectionDetailed(config);
            return result != null && result.getSuccess();
        } catch (Exception e) {
            log.error("测试设备连接失败: 错误={}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 获取监控状态
     * @return 是否正在监控
     */
    @Override
    public boolean isMonitoring() {
        return monitoring.get();
    }

    /**
     * 获取监控设备数量
     * @return 监控设备数量
     */
    @Override
    public int getMonitoredDeviceCount() {
        return deviceStatusCache.size();
    }

    /**
     * 定时刷新所有设备状态（每30秒执行一次）
     */
    @Scheduled(fixedRate = 30000)
    public void scheduledRefreshAllDeviceStatus() {
        if (!monitoring.get()) {
            return;
        }

        try {
            log.debug("开始定时刷新所有设备状态");

            List<Long> deviceIds = new ArrayList<>(deviceStatusCache.keySet());
            Map<Long, DeviceStatus> updatedStatusMap = batchRefreshDeviceStatus(deviceIds);

            log.debug("定时刷新设备状态完成: 共{}个设备，更新{}个",
                    deviceIds.size(), updatedStatusMap.size());

        } catch (Exception e) {
            log.error("定时刷新设备状态失败: 错误={}", e.getMessage(), e);
        }
    }

    /**
     * 加载所有设备到监控列表
     */
    private void loadAllDevicesToMonitor() {
        try {
            // 使用DeviceQueryDTO查询所有设备
            Result<List<DeviceVO>> result = deviceService.getDeviceList(null);
            if (result == null || result.getData() == null) {
                log.warn("没有设备可加载");
                return;
            }

            List<DeviceVO> deviceVOs = result.getData();
            for (DeviceVO deviceVO : deviceVOs) {
                addDeviceToMonitor(deviceVO.getDeviceId());
            }

            log.info("已加载{}个设备到监控列表", deviceVOs.size());

        } catch (Exception e) {
            log.error("加载设备到监控列表失败: 错误={}", e.getMessage(), e);
        }
    }

    /**
     * 发送设备状态通知
     * @param deviceId 设备ID
     * @param status 设备状态
     */
    private void sendDeviceStatusNotification(Long deviceId, DeviceStatus status) {
        try {
            if (messagingTemplate != null) {
                Map<String, Object> message = new HashMap<>();
                message.put("deviceId", deviceId);
                message.put("deviceName", status.getDeviceName());
                message.put("deviceType", status.getDeviceType());
                message.put("online", status.isOnline());
                message.put("timestamp", System.currentTimeMillis());

                // 推送到WebSocket
                messagingTemplate.convertAndSend("/topic/device-status", message);

                log.info("设备状态通知已发送: deviceId={} online={}", deviceId, status.isOnline());
            }
        } catch (Exception e) {
            log.error("发送设备状态通知失败: deviceId={} 错误={}", deviceId, e.getMessage(), e);
        }
    }

    /**
     * 更新数据库中的设备状态
     * @param deviceId 设备ID
     * @param online 在线状态
     */
    private void updateDeviceStatusInDatabase(Long deviceId, boolean online) {
        try {
            // 将boolean转换为Integer：true=1(在线), false=0(离线)
            Integer status = online ? 1 : 0;
            deviceService.updateDeviceStatus(deviceId, status);
        } catch (Exception e) {
            log.error("更新设备状态到数据库失败: deviceId={} 错误={}", deviceId, e.getMessage(), e);
        }
    }

    /**
     * 转换为硬件配置
     * @param device 设备实体
     * @return 硬件配置
     */
    private HardwareConfig convertToHardwareConfig(Device device) {
        HardwareConfig config = new HardwareConfig();
        config.setId(device.getId());
        config.setDeviceType(device.getDeviceType() != null ? String.valueOf(device.getDeviceType()) : null);
        config.setDeviceName(device.getDeviceName());
        config.setDeviceModel(device.getModel());
        config.setConnectionType(device.getConnectionType() != null ? String.valueOf(device.getConnectionType()) : null);
        config.setConfigJson(device.getConnectionConfig());
        
        // 从connectionConfig中解析IP和端口
        if (device.getConnectionConfig() != null) {
            try {
                String configJson = device.getConnectionConfig();
                JsonNode jsonNode = objectMapper.readTree(configJson);
                
                // 处理数组格式的JSON
                if (jsonNode.isArray()) {
                    jsonNode = jsonNode.get(0);
                    log.debug("检测到数组格式的JSON，使用第一个元素");
                }
                
                if (jsonNode.has("ipAddress")) {
                    String ipAddress = jsonNode.get("ipAddress").asText();
                    config.setIpAddress(ipAddress);
                    log.debug("解析到IP地址: {}", ipAddress);
                }
                
                if (jsonNode.has("port")) {
                    String port = jsonNode.get("port").asText();
                    config.setPort(port);
                    log.debug("解析到端口: {}", port);
                }
                
                if (jsonNode.has("baudRate")) {
                    String baudRate = jsonNode.get("baudRate").asText();
                    config.setBaudRate(baudRate);
                    log.debug("解析到波特率: {}", baudRate);
                }
            } catch (Exception e) {
                log.debug("解析设备配置失败: {}", e.getMessage());
            }
        }
        
        return config;
    }
}
