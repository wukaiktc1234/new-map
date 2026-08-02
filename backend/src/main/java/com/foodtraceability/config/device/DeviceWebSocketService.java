package com.foodtraceability.config.device;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 设备WebSocket消息推送服务
 * 用于向前端实时推送设备状态变更、告警通知、打印任务状态等
 */
@Component
public class DeviceWebSocketService {

    private static final Logger log = LoggerFactory.getLogger(DeviceWebSocketService.class);

    /** 目标前缀：设备状态变更 */
    public static final String TOPIC_DEVICE_STATUS = "/topic/device/status";

    /** 目标前缀：告警通知 */
    public static final String TOPIC_DEVICE_ALERT = "/topic/device/alert";

    /** 目标前缀：打印任务状态 */
    public static final String TOPIC_PRINT_TASK = "/topic/print/task";

    /** 目标前缀：系统通知 */
    public static final String TOPIC_SYSTEM_NOTIFICATION = "/topic/system/notification";

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public DeviceWebSocketService(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 推送设备状态变更
     * @param deviceId 设备ID
     * @param oldStatus 旧状态
     * @param newStatus 新状态
     * @param statusName 状态名称
     */
    public void pushDeviceStatusChange(Long deviceId, Integer oldStatus, Integer newStatus,
                                        String statusName) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "STATUS_CHANGE");
            message.put("deviceId", deviceId);
            message.put("oldStatus", oldStatus);
            message.put("newStatus", newStatus);
            message.put("statusName", statusName);
            message.put("timestamp", LocalDateTime.now().toString());

            messagingTemplate.convertAndSend(TOPIC_DEVICE_STATUS, message);
            log.debug("推送设备状态变更: deviceId={}, {} -> {}", deviceId, oldStatus, newStatus);
        } catch (Exception e) {
            log.error("推送设备状态变更失败: deviceId={}", deviceId, e);
        }
    }

    /**
     * 推送设备心跳更新
     * @param deviceId 设备ID
     * @param deviceName 设备名称
     * @param heartbeatTime 心跳时间
     */
    public void pushDeviceHeartbeat(Long deviceId, String deviceName, LocalDateTime heartbeatTime) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "HEARTBEAT");
            message.put("deviceId", deviceId);
            message.put("deviceName", deviceName);
            message.put("heartbeatTime", heartbeatTime != null ? heartbeatTime.toString() : null);
            message.put("timestamp", LocalDateTime.now().toString());

            messagingTemplate.convertAndSend(TOPIC_DEVICE_STATUS, message);
            log.debug("推送设备心跳: deviceId={}", deviceId);
        } catch (Exception e) {
            log.error("推送设备心跳失败: deviceId={}", deviceId, e);
        }
    }

    /**
     * 推送新告警通知
     * @param alertId 告警ID
     * @param deviceId 设备ID
     * @param alertType 告警类型
     * @param alertLevel 告警级别
     * @param alertMessage 告警消息
     */
    public void pushAlertNotification(Long alertId, Long deviceId, Integer alertType,
                                       Integer alertLevel, String alertMessage) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "NEW_ALERT");
            message.put("alertId", alertId);
            message.put("deviceId", deviceId);
            message.put("alertType", alertType);
            message.put("alertTypeName", getAlertTypeName(alertType));
            message.put("alertLevel", alertLevel);
            message.put("alertLevelName", getAlertLevelName(alertLevel));
            message.put("alertMessage", alertMessage);
            message.put("timestamp", LocalDateTime.now().toString());

            messagingTemplate.convertAndSend(TOPIC_DEVICE_ALERT, message);
            log.info("推送告警通知: alertId={}, type={}, level={}", alertId, alertType, alertLevel);
        } catch (Exception e) {
            log.error("推送告警通知失败: alertId={}", alertId, e);
        }
    }

    /**
     * 推送打印任务状态变更
     * @param taskId 任务ID
     * @param taskCode 任务编号
     * @param oldStatus 旧状态
     * @param newStatus 新状态
     * @param statusName 状态名称
     */
    public void pushPrintTaskStatusChange(Long taskId, String taskCode,
                                           Integer oldStatus, Integer newStatus,
                                           String statusName) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "TASK_STATUS_CHANGE");
            message.put("taskId", taskId);
            message.put("taskCode", taskCode);
            message.put("oldStatus", oldStatus);
            message.put("newStatus", newStatus);
            message.put("statusName", statusName);
            message.put("timestamp", LocalDateTime.now().toString());

            messagingTemplate.convertAndSend(TOPIC_PRINT_TASK, message);
            log.info("推送任务状态变更: taskId={}, taskCode={}", taskId, taskCode);
        } catch (Exception e) {
            log.error("推送任务状态变更失败: taskId={}", taskId, e);
        }
    }

    /**
     * 推送系统通知（如批量离线警告）
     * @param title 标题
     * @param content 内容
     * @param level 级别：info/warn/error
     */
    public void pushSystemNotification(String title, String content, String level) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "SYSTEM_NOTIFICATION");
            message.put("title", title);
            message.put("content", content);
            message.put("level", level);
            message.put("timestamp", LocalDateTime.now().toString());

            messagingTemplate.convertAndSend(TOPIC_SYSTEM_NOTIFICATION, message);
            log.info("推送系统通知: title={}, level={}", title, level);
        } catch (Exception e) {
            log.error("推送系统通知失败: title={}", title, e);
        }
    }

    /**
     * 向指定用户推送消息
     * @param username 用户名
     * @param destination 目标地址
     * @param payload 消息内容
     */
    public void sendToUser(String username, String destination, Object payload) {
        try {
            String userDestination = "/user/" + username + destination;
            messagingTemplate.convertAndSend(userDestination, payload);
            log.debug("向用户推送消息: user={}, dest={}", username, destination);
        } catch (Exception e) {
            log.error("向用户推送消息失败: user={}, dest={}", username, destination, e);
        }
    }

    /**
     * 获取告警类型名称
     */
    private String getAlertTypeName(Integer alertType) {
        if (alertType == null) return "未知";
        return switch (alertType) {
            case 1 -> "离线超时";
            case 2 -> "纸张缺";
            case 3 -> "碳带缺";
            case 4 -> "故障";
            case 5 -> "维护提醒";
            default -> "未知";
        };
    }

    /**
     * 获取告警级别名称
     */
    private String getAlertLevelName(Integer alertLevel) {
        if (alertLevel == null) return "未知";
        return switch (alertLevel) {
            case 1 -> "信息";
            case 2 -> "警告";
            case 3 -> "严重";
            case 4 -> "紧急";
            default -> "未知";
        };
    }
}
