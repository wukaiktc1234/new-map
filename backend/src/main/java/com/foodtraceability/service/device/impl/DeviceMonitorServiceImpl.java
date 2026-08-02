package com.foodtraceability.service.device.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.common.Result;
import com.foodtraceability.entity.Device;
import com.foodtraceability.entity.DeviceAlert;
import com.foodtraceability.entity.DeviceStatusLog;
import com.foodtraceability.dto.DeviceAlertVO;
import com.foodtraceability.mapper.*;
import com.foodtraceability.service.device.DeviceAlertService;
import com.foodtraceability.service.device.DeviceMonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 设备监控服务实现类
 * 负责设备状态监控、离线检测、告警触发等
 */
@Service
public class DeviceMonitorServiceImpl implements DeviceMonitorService {

    private static final Logger log = LoggerFactory.getLogger(DeviceMonitorServiceImpl.class);

    /** 离线判定阈值（分钟），超过此时间无心跳则标记为离线 */
    private static final long OFFLINE_THRESHOLD_MINUTES = 5;

    private final DeviceMapper deviceMapper;
    private final DeviceStatusLogMapper deviceStatusLogMapper;
    private final DeviceAlertMapper deviceAlertMapper;
    private final DeviceAlertService deviceAlertService;

    public DeviceMonitorServiceImpl(DeviceMapper deviceMapper,
                                   DeviceStatusLogMapper deviceStatusLogMapper,
                                   DeviceAlertMapper deviceAlertMapper,
                                   DeviceAlertService deviceAlertService) {
        this.deviceMapper = deviceMapper;
        this.deviceStatusLogMapper = deviceStatusLogMapper;
        this.deviceAlertMapper = deviceAlertMapper;
        this.deviceAlertService = deviceAlertService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Map<String, Object>> checkDeviceStatus() {
        try {
            log.info("开始执行设备状态检查...");

            // 获取所有在线的设备
            LambdaQueryWrapper<Device> onlineWrapper = new LambdaQueryWrapper<>();
            // 数据库 status 列为 varchar，需用字符串 "1" 进行比较
            onlineWrapper.eq(Device::getStatus, "1"); // 在线状态

            List<Device> onlineDevices = deviceMapper.selectList(onlineWrapper);

            LocalDateTime now = LocalDateTime.now();
            int offlineCount = 0;
            int alertCount = 0;

            for (Device device : onlineDevices) {
                if (device.getLastHeartbeatTime() == null) {
                    // 无心跳时间，直接标记为离线
                    markDeviceOffline(device.getDeviceId());
                    offlineCount++;
                    continue;
                }

                // 计算距上次心跳的时间差（分钟）
                long minutesSinceLastHeartbeat = ChronoUnit.MINUTES.between(
                        device.getLastHeartbeatTime(), now);

                if (minutesSinceLastHeartbeat > OFFLINE_THRESHOLD_MINUTES) {
                    // 超过阈值，标记为离线并创建告警
                    markDeviceOffline(device.getDeviceId());

                    // 创建离线超时告警
                    Result<DeviceAlertVO> alertResult = deviceAlertService.createAlert(
                            device.getDeviceId(),
                            1,  // 告警类型：离线超时
                            2   // 告警级别：警告
                    );

                    if (alertResult.isSuccess()) {
                        alertCount++;
                    }

                    offlineCount++;
                    log.warn("设备离线: deviceId={}, deviceName={}, 距上次心跳={}分钟",
                            device.getDeviceId(), device.getDeviceName(), minutesSinceLastHeartbeat);
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("checkTime", now.toString());
            result.put("checkedDevices", onlineDevices.size());
            result.put("offlineDevices", offlineCount);
            result.put("newAlerts", alertCount);

            log.info("设备状态检查完成: 检查{}台设备, 发现{}台离线, 创建{}条告警",
                    onlineDevices.size(), offlineCount, alertCount);

            return Result.success(result, "设备状态检查完成");
        } catch (Exception e) {
            log.error("设备状态检查失败", e);
            return Result.error("设备状态检查失败：" + e.getMessage());
        }
    }

    @Override
    public Result<Boolean> checkDeviceOffline(Long deviceId) {
        try {
            Device device = deviceMapper.selectById(deviceId);
            if (device == null) {
                return Result.error("设备不存在");
            }

            if (device.getStatus() == null || device.getStatus() != 1) {
                return Result.success(false, "设备当前不在线");
            }

            if (device.getLastHeartbeatTime() == null) {
                return Result.success(true, "设备无心跳记录");
            }

            LocalDateTime now = LocalDateTime.now();
            long minutesSinceHeartbeat = ChronoUnit.MINUTES.between(
                    device.getLastHeartbeatTime(), now);

            boolean isOffline = minutesSinceHeartbeat > OFFLINE_THRESHOLD_MINUTES;

            return Result.success(isOffline,
                    isOffline ? String.format("设备已离线，距上次心跳%d分钟", minutesSinceHeartbeat)
                               : "设备正常在线");
        } catch (Exception e) {
            log.error("检查设备离线状态失败: deviceId={}", deviceId, e);
            return Result.error("检查失败：" + e.getMessage());
        }
    }

    @Override
    public Result<List<Long>> getDevicesForCheck() {
        try {
            LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
            // 数据库 status 列为 varchar，需用字符串 "1" 进行比较
            wrapper.eq(Device::getStatus, "1"); // 只检查在线设备
            wrapper.select(Device::getDeviceId);

            List<Device> devices = deviceMapper.selectList(wrapper);
            List<Long> deviceIds = devices.stream()
                    .map(Device::getDeviceId)
                    .collect(Collectors.toList());

            return Result.success(deviceIds, "获取待检查设备成功");
        } catch (Exception e) {
            log.error("获取待检查设备失败", e);
            return Result.error("获取失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> handleDeviceOnline(Long deviceId) {
        try {
            Device device = deviceMapper.selectById(deviceId);
            if (device == null) {
                return Result.error("设备不存在");
            }

            Integer oldStatus = device.getStatus();
            device.setStatus(1); // 在线
            device.setLastHeartbeatTime(LocalDateTime.now());
            device.setUpdateTime(LocalDateTime.now());
            deviceMapper.updateById(device);

            // 记录状态日志
            recordStatusLog(deviceId, oldStatus, 1, 1, "设备上线");

            // 如果之前有未处理的离线告警，自动处理
            resolveOfflineAlerts(deviceId);

            log.info("处理设备上线: deviceId={}, deviceName={}", deviceId, device.getDeviceName());
            return Result.success(null, "设备已上线");
        } catch (Exception e) {
            log.error("处理设备上线失败: deviceId={}", deviceId, e);
            return Result.error("处理失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> handleDeviceOffline(Long deviceId) {
        try {
            markDeviceOffline(deviceId);

            // 创建离线告警
            deviceAlertService.createAlert(deviceId, 1, 2);

            log.info("处理设备离线: deviceId={}", deviceId);
            return Result.success(null, "设备已标记为离线");
        } catch (Exception e) {
            log.error("处理设备离线失败: deviceId={}", deviceId, e);
            return Result.error("处理失败：" + e.getMessage());
        }
    }

    @Override
    public Result<Map<String, Long>> getDeviceStatistics() {
        try {
            Map<String, Long> statistics = new HashMap<>();

            // 总设备数
            LambdaQueryWrapper<Device> totalWrapper = new LambdaQueryWrapper<>();
            statistics.put("total", deviceMapper.selectCount(totalWrapper));

            // 各状态数量
            for (int status = 0; status <= 3; status++) {
                LambdaQueryWrapper<Device> statusWrapper = new LambdaQueryWrapper<>();
                // 数据库 status 列为 varchar，将 int 转为字符串进行比较
                statusWrapper.eq(Device::getStatus, String.valueOf(status));
                Long count = deviceMapper.selectCount(statusWrapper);

                String key = switch (status) {
                    case 0 -> "offline";
                    case 1 -> "online";
                    case 2 -> "fault";
                    case 3 -> "maintenance";
                    default -> "unknown_" + status;
                };
                statistics.put(key, count);
            }

            // 未处理的告警数量
            LambdaQueryWrapper<DeviceAlert> alertWrapper = new LambdaQueryWrapper<>();
            alertWrapper.eq(DeviceAlert::getIsHandled, false);
            statistics.put("unhandledAlerts", deviceAlertMapper.selectCount(alertWrapper));

            return Result.success(statistics, "获取设备统计信息成功");
        } catch (Exception e) {
            log.error("获取设备统计信息失败", e);
            return Result.error("获取失败：" + e.getMessage());
        }
    }

    @Override
    public Result<List<Map<String, Object>>> getRecentStatusChanges(int minutes, int limit) {
        try {
            LocalDateTime since = LocalDateTime.now().minusMinutes(minutes);

            LambdaQueryWrapper<DeviceStatusLog> wrapper = new LambdaQueryWrapper<>();
            wrapper.ge(DeviceStatusLog::getCreateTime, since)
                   .orderByDesc(DeviceStatusLog::getCreateTime)
                   .last("LIMIT " + limit);

            List<DeviceStatusLog> logs = deviceStatusLogMapper.selectList(wrapper);

            List<Map<String, Object>> result = logs.stream().map(log -> {
                Map<String, Object> item = new HashMap<>();
                item.put("logId", log.getLogId());
                item.put("deviceId", log.getDeviceId());
                item.put("oldStatus", log.getOldStatus());
                item.put("newStatus", log.getNewStatus());
                item.put("eventType", log.getEventType());
                item.put("message", log.getMessage());
                item.put("createTime", log.getCreateTime());
                return item;
            }).collect(Collectors.toList());

            return Result.success(result, "查询最近状态变更成功");
        } catch (Exception e) {
            log.error("查询最近状态变更失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 标记设备为离线
     */
    private void markDeviceOffline(Long deviceId) {
        try {
            Device device = deviceMapper.selectById(deviceId);
            if (device != null && (device.getStatus() == null || device.getStatus() == 1)) {
                Integer oldStatus = device.getStatus();
                device.setStatus(0); // 离线
                device.setUpdateTime(LocalDateTime.now());
                deviceMapper.updateById(device);

                // 记录日志
                recordStatusLog(deviceId, oldStatus, 0, 2, "设备离线（超时无心跳）");
            }
        } catch (Exception e) {
            log.error("标记设备离线失败: deviceId={}", deviceId, e);
        }
    }

    /**
     * 记录状态变更日志
     */
    private void recordStatusLog(Long deviceId, Integer oldStatus, Integer newStatus,
                                 Integer eventType, String message) {
        try {
            DeviceStatusLog log = new DeviceStatusLog();
            log.setDeviceId(deviceId);
            log.setOldStatus(oldStatus);
            log.setNewStatus(newStatus);
            log.setEventType(eventType);
            log.setMessage(message);
            log.setCreateTime(LocalDateTime.now());
            deviceStatusLogMapper.insert(log);
        } catch (Exception e) {
            log.error("记录状态日志失败: deviceId={}", deviceId, e);
        }
    }

    /**
     * 解决该设备的离线告警
     */
    private void resolveOfflineAlerts(Long deviceId) {
        try {
            LambdaQueryWrapper<DeviceAlert> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(DeviceAlert::getDeviceId, deviceId)
                   .eq(DeviceAlert::getAlertType, 1) // 离线超时
                   .eq(DeviceAlert::getIsHandled, false);

            List<DeviceAlert> alerts = deviceAlertMapper.selectList(wrapper);
            for (DeviceAlert alert : alerts) {
                deviceAlertService.handleAlert(alert.getAlertId(), "设备已上线，自动关闭");
            }
        } catch (Exception e) {
            log.error("解决离线告警失败: deviceId={}", deviceId, e);
        }
    }
}
