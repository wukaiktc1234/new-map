package com.foodtraceability.service;

import com.foodtraceability.common.Result;
import com.foodtraceability.entity.HardwareConfig;
import com.foodtraceability.hardware.DriverRegistry;
import com.foodtraceability.hardware.TextBasedDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 叫号系统硬件服务
 * <p>
 * 负责通过硬件驱动（串口音箱、LED显示屏等）实现叫号语音播报和显示屏联动。
 * 当硬件不可用时，前端应回退到浏览器 Web Speech API。
 * </p>
 */
@Service
public class CallNumberHardwareService {

    private static final Logger log = LoggerFactory.getLogger(CallNumberHardwareService.class);


    public CallNumberHardwareService(DriverRegistry driverRegistry) {
        this.driverRegistry = driverRegistry;
    }

    /** 硬件设备类型常量 */
    public static final String DEVICE_TYPE_SPEAKER = "SPEAKER";
    public static final String DEVICE_TYPE_DISPLAY = "DISPLAY";

    private final DriverRegistry driverRegistry;

    /**
     * 通过硬件语音设备播报叫号信息
     *
     * @param orderNumber 订单号（如 A001）
     * @param orderType  订单类型（堂食/外卖/自提/打包）
     * @return 包含成功状态和消息的结果
     */
    public Result<Map<String, Object>> broadcastVoice(String orderNumber, String orderType) {
        Map<String, Object> result = new HashMap<>();
        result.put("orderNumber", orderNumber);
        result.put("orderType", orderType);

        TextBasedDriver driver = getSpeakerDriver();
        if (driver == null) {
            log.warn("语音播报硬件不可用，将使用浏览器TTS回退: orderNumber={}", orderNumber);
            result.put("hardwareAvailable", false);
            result.put("message", "硬件不可用，请使用浏览器TTS");
            return Result.success(result);
        }

        try {
            // 构造语音指令文本：格式为 "请 A001 号顾客就餐"
            String voiceCommand = buildVoiceCommand(orderNumber, orderType);
            String response = driver.sendCommand(voiceCommand, 3000);

            log.info("硬件语音播报成功: orderNumber={}, response={}", orderNumber, response);
            result.put("hardwareAvailable", true);
            result.put("message", "语音播报已发送到硬件");
            result.put("response", response);
            return Result.success(result);
        } catch (Exception e) {
            log.error("硬件语音播报失败: orderNumber={}, error={}", orderNumber, e.getMessage(), e);
            result.put("hardwareAvailable", false);
            result.put("message", "硬件播报失败: " + e.getMessage());
            return Result.success(result); // 返回success但标记hardwareAvailable=false，让前端fallback
        }
    }

    /**
     * 向LED显示屏发送叫号信息
     *
     * @param orderNumber 订单号
     * @param orderType  订单类型
     * @param tableNumber 桌号（可选）
     * @return 包含发送结果的信息
     */
    public Result<Map<String, Object>> sendToDisplay(String orderNumber, String orderType, String tableNumber) {
        Map<String, Object> result = new HashMap<>();
        result.put("orderNumber", orderNumber);
        result.put("orderType", orderType);

        TextBasedDriver driver = getDisplayDriver();
        if (driver == null) {
            log.warn("LED显示屏硬件不可用: orderNumber={}", orderNumber);
            result.put("hardwareAvailable", false);
            result.put("message", "LED显示屏不可用");
            return Result.success(result);
        }

        try {
            // 构造显示文本：格式为 "请 A001 号顾客就餐"
            String displayText = buildDisplayText(orderNumber, orderType, tableNumber);
            String response = driver.sendCommand(displayText, 3000);

            log.info("LED显示屏信息发送成功: orderNumber={}, text={}", orderNumber, displayText);
            result.put("hardwareAvailable", true);
            result.put("message", "显示屏信息已发送");
            result.put("displayText", displayText);
            result.put("response", response);
            return Result.success(result);
        } catch (Exception e) {
            log.error("LED显示屏信息发送失败: orderNumber={}, error={}", orderNumber, e.getMessage(), e);
            result.put("hardwareAvailable", false);
            result.put("message", "显示屏发送失败: " + e.getMessage());
            return Result.success(result);
        }
    }

    /**
     * 获取硬件连接状态
     *
     * @return 各类硬件设备的连接状态
     */
    public Result<Map<String, Object>> getHardwareStatus() {
        Map<String, Object> status = new HashMap<>();

        boolean speakerAvailable = getSpeakerDriver() != null && getSpeakerDriver().isConnected();
        boolean displayAvailable = getDisplayDriver() != null && getDisplayDriver().isConnected();

        status.put("speakerAvailable", speakerAvailable);
        status.put("displayAvailable", displayAvailable);
        status.put("anyHardwareAvailable", speakerAvailable || displayAvailable);

        return Result.success(status);
    }

    /**
     * 获取语音播报驱动实例
     */
    private TextBasedDriver getSpeakerDriver() {
        HardwareConfig config = new HardwareConfig();
        config.setDeviceType(DEVICE_TYPE_SPEAKER);
        config.setConnectionType("SERIAL");
        var driver = driverRegistry.getDriverByConfig(config);
        if (driver instanceof TextBasedDriver textDriver) {
            return textDriver;
        }
        return null;
    }

    /**
     * 获取LED显示屏驱动实例
     */
    private TextBasedDriver getDisplayDriver() {
        HardwareConfig config = new HardwareConfig();
        config.setDeviceType(DEVICE_TYPE_DISPLAY);
        config.setConnectionType("SERIAL");
        var driver = driverRegistry.getDriverByConfig(config);
        if (driver instanceof TextBasedDriver textDriver) {
            return textDriver;
        }
        return null;
    }

    /**
     * 构造硬件语音指令文本
     * 格式: "请 A001 号顾客就餐" 或 "外卖订单 A001 请取餐"
     */
    private String buildVoiceCommand(String orderNumber, String orderType) {
        if ("外卖".equals(orderType)) {
            return "VOICE:TTS:" + orderNumber + ":外卖订单请取餐";
        } else if ("自提".equals(orderType)) {
            return "VOICE:TTS:" + orderNumber + ":自提订单请取餐";
        } else if ("打包".equals(orderType)) {
            return "VOICE:TTS:" + orderNumber + ":打包订单请领取";
        } else {
            // 默认堂食
            return "VOICE:TTS:" + orderNumber + ":请顾客就餐";
        }
    }

    /**
     * 构造LED显示屏显示文本
     * 格式: "请 A001 号顾客就餐"
     */
    private String buildDisplayText(String orderNumber, String orderType, String tableNumber) {
        StringBuilder sb = new StringBuilder();
        sb.append("DISPLAY:").append(orderNumber).append(":");

        if ("外卖".equals(orderType)) {
            sb.append("外卖订单 ").append(orderNumber).append(" 请取餐");
        } else if ("自提".equals(orderType)) {
            sb.append("自提订单 ").append(orderNumber).append(" 请取餐");
        } else if ("打包".equals(orderType)) {
            sb.append("打包订单 ").append(orderNumber).append(" 请领取");
        } else {
            sb.append("请 ").append(orderNumber).append(" 号顾客就餐");
        }

        if (tableNumber != null && !tableNumber.isEmpty()) {
            sb.append(" [").append(tableNumber).append("桌]");
        }

        return sb.toString();
    }
}
