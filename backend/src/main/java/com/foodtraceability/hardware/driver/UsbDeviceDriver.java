package com.foodtraceability.hardware.driver;

import com.foodtraceability.entity.HardwareConfig;
import com.foodtraceability.hardware.AbstractHardwareDriver;
import com.foodtraceability.util.CommunicationProtocolFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.Map;

/**
 * USB设备驱动实现
 * <p>
 * 仅实现基础 HardwareDriver 接口，专注于USB设备的连接管理和生命周期控制。
 * 不强制实现文本/二进制通信协议（遵循接口隔离原则）。
 * </p>
 *
 * <p>设计说明：</p>
 * <ul>
 *   <li>USB设备通常使用专用协议库（如libusb、HidApi等）</li>
 *   <li>通信方式取决于具体设备类型（打印机/扫描仪/摄像头）</li>
 *   <li>未来可根据需要扩展 TextBasedDriver 或 BinaryDriver 接口</li>
 * </ul>
 *
 * @see AbstractHardwareDriver 基础驱动抽象类
 */
@Component
public class UsbDeviceDriver extends AbstractHardwareDriver {
    private CommunicationProtocolFactory.UsbProtocol usbProtocol;
    private String deviceModel;

    public UsbDeviceDriver() {
        super("USB", "USB");
        this.usbProtocol = CommunicationProtocolFactory.getUsbProtocol();
    }

    @Override
    public boolean connect(HardwareConfig config) {
        validateConfig(config);
        try {
            this.config = config;
            this.deviceModel = config.getDeviceModel();
            if (deviceModel == null || deviceModel.isEmpty()) {
                deviceModel = config.getDeviceName() != null ? config.getDeviceName() : "UNKNOWN";
            }
            String deviceType = config.getDeviceType();
            boolean success;
            if ("PRINTER".equalsIgnoreCase(deviceType)) {
                success = usbProtocol.connectPrinter(deviceModel);
            } else if ("SCANNER".equalsIgnoreCase(deviceType)) {
                success = usbProtocol.connectScanner(deviceModel);
            } else if ("CAMERA".equalsIgnoreCase(deviceType)) {
                success = usbProtocol.connectCamera(deviceModel);
            } else {
                success = usbProtocol.connect(deviceModel);
            }
            if (success) {
                connected.set(true);
                updateActivityTime();
                log.info("USB驱动连接成功: model={} type={}", deviceModel, deviceType);
            } else {
                log.error("USB驱动连接失败: model={} type={}", deviceModel, deviceType);
            }
            return success;
        } catch (Exception e) {
            log.error("USB驱动连接异常: model={} error={}", deviceModel, e.getMessage());
            return false;
        }
    }

    @Override
    public void disconnect() {
        try {
            if (usbProtocol != null && deviceModel != null) {
                log.info("USB驱动断开连接: model={}", deviceModel);
            }
        } finally {
            connected.set(false);
            usbProtocol = null;
            deviceModel = null;
            log.info("USB驱动已断开连接并清理资源");
        }
    }

    @Override
    public Map<String, Object> getDriverInfo() {
        Map<String, Object> info = super.getDriverInfo();
        info.put("deviceModel", deviceModel);
        return info;
    }

    @Override
    public boolean testConnection() {
        boolean result = isConnected();
        log.info("USB连接测试: model={} result={}", deviceModel, result ? "成功" : "失败");
        return result;
    }

    private void checkConnected() {
        if (!isConnected()) {
            throw new IllegalStateException("USB设备未连接: " + deviceModel);
        }
    }
}
