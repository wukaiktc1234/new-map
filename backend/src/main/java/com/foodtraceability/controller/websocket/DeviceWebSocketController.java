package com.foodtraceability.controller.websocket;

import com.foodtraceability.entity.DeviceStatus;
import com.foodtraceability.service.DeviceStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * 设备WebSocket控制器
 * 用于实时推送设备状态变化和通知
 */
@Controller
public class DeviceWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(DeviceWebSocketController.class);


    public DeviceWebSocketController(SimpMessagingTemplate messagingTemplate, DeviceStatusService deviceStatusService) {
        this.messagingTemplate = messagingTemplate;
        this.deviceStatusService = deviceStatusService;
    }

    private final SimpMessagingTemplate messagingTemplate;

    private final DeviceStatusService deviceStatusService;

    /**
     * 客户端订阅设备状态变化
     * @param deviceType 设备类型
     * @return 设备当前状态
     */
    @MessageMapping("/device/subscribe")
    @SendTo("/topic/device/status")
    public DeviceStatus subscribeDeviceStatus(String deviceType) {
        log.info("客户端订阅设备状态: {}", deviceType);
        return deviceStatusService.getDeviceDetailedStatus(deviceType);
    }

    /**
     * 推送设备状态变化
     * @param deviceStatus 设备状态
     */
    public void pushDeviceStatusChange(DeviceStatus deviceStatus) {
        messagingTemplate.convertAndSend("/topic/device/status", deviceStatus);
        log.info("推送设备状态变化: 设备类型={}, 状态={}", deviceStatus.getDeviceType(), deviceStatus.isOnline() ? "ONLINE" : "OFFLINE");
    }

    /**
     * 推送设备连接状态变化
     * @param deviceType 设备类型
     * @param isConnected 是否连接
     */
    public void pushDeviceConnectionChange(String deviceType, boolean isConnected) {
        DeviceStatus status = new DeviceStatus();
        status.setDeviceType(deviceType);
        status.setOnline(isConnected);
        messagingTemplate.convertAndSend("/topic/device/connection", status);
        log.info("推送设备连接状态变化: 设备类型={}, 连接状态={}", deviceType, isConnected);
    }

    /**
     * 推送打印任务状态变化
     * @param taskId 任务ID
     * @param status 任务状态
     */
    public void pushPrintTaskStatusChange(String taskId, String status) {
        messagingTemplate.convertAndSend("/topic/print/task/" + taskId, status);
        log.info("推送打印任务状态变化: 任务ID={}, 状态={}", taskId, status);
    }
}
