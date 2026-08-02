package com.foodtraceability.hardware.driver;

import com.foodtraceability.entity.HardwareConfig;
import com.foodtraceability.hardware.AbstractHardwareDriver;
import com.foodtraceability.hardware.BinaryDriver;
import com.foodtraceability.hardware.TextBasedDriver;
import com.foodtraceability.hardware.utils.HexUtils;
import com.foodtraceability.util.CommunicationProtocolFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.Map;

/**
 * 网络驱动实现类
 * <p>
 * 同时实现 {@link TextBasedDriver} 和 {@link BinaryDriver} 接口，
 * 支持通过TCP/IP网络协议与硬件设备进行文本和二进制协议通信。
 * 适用于通过网络（TCP/IP）连接的硬件设备（如网络打印机、网络扫描仪、
 * 网络电子秤等支持Socket通信的设备）。
 * </p>
 *
 * <p><strong>支持的协议：</strong></p>
 * <ul>
 *   <li>TCP Socket：基于TCP协议的可靠字节流传输</li>
 *   <li>默认端口：9100（标准打印端口），可配置</li>
 *   <li>IPv4/IPv4双栈：同时支持IPv4和IPv6地址</li>
 * </ul>
 *
 * <p><strong>超时策略：</strong></p>
 * <ul>
 *   <li>连接超时：默认5000毫秒，用于建立TCP连接的最大等待时间</li>
 *   <li>发送/接收超时：可通过参数自定义，用于单次操作的超时控制</li>
 *   <li>无响应处理：超时后返回null或空数据，不抛出异常</li>
 *   <li>心跳检测：建议上层定期调用 {@link #testConnection()} 检测连接状态</li>
 * </ul>
 *
 * <p><strong>数据编码：</strong></p>
 * <ul>
 *   <li>文本数据：使用UTF-8编码传输字符串</li>
 *   <li>二进制原始数据：使用十六进制（Hex）大写编码格式传输，与串口驱动保持一致</li>
 * </ul>
 *
 * @see TextBasedDriver 文本协议接口
 * @see BinaryDriver 二进制协议接口
 */
@Component
public class NetworkDriver extends AbstractHardwareDriver implements TextBasedDriver, BinaryDriver {
    private static final int DEFAULT_TIMEOUT_MS = 5000;
    private CommunicationProtocolFactory.SocketProtocol socketProtocol;
    private String ipAddress;
    private int port = 9100;

    public NetworkDriver() {
        super("NETWORK", "NETWORK");
        this.socketProtocol = CommunicationProtocolFactory.getSocketProtocol();
    }

    @Override
    public boolean connect(HardwareConfig config) {
        validateConfig(config);
        try {
            this.config = config;
            this.ipAddress = config.getIpAddress();
            if (this.ipAddress == null || this.ipAddress.isEmpty()) {
                log.error("网络设备IP地址不能为空");
                return false;
            }
            if (config.getPort() != null && !config.getPort().isEmpty()) {
                this.port = Integer.parseInt(config.getPort());
            }
            boolean success = socketProtocol.connect(ipAddress, port, DEFAULT_TIMEOUT_MS);
            if (success) {
                connected.set(true);
                updateActivityTime();
                log.info("网络驱动连接成功: ip:{} port:{}", ipAddress, port);
            } else {
                log.error("网络驱动连接失败: ip:{} port:{}", ipAddress, port);
            }
            return success;
        } catch (NumberFormatException e) {
            log.error("端口号格式错误: {}", config.getPort());
            return false;
        }
    }

    @Override
    public void disconnect() {
        if (ipAddress != null) {
            socketProtocol.disconnect(ipAddress, port);
            connected.set(false);
            log.info("网络驱动已断开: ip:{} port:{}", ipAddress, port);
        }
    }

    @Override
    public String sendCommand(String command) {
        return sendCommand(command, DEFAULT_TIMEOUT_MS);
    }

    @Override
    public String sendCommand(String command, int timeoutMs) {
        checkConnected();
        updateActivityTime();
        boolean success = socketProtocol.sendData(ipAddress, port, command, timeoutMs);
        if (!success) {
            log.warn("网络数据发送失败: ip:{} port:{}", ipAddress, port);
            return null;
        }
        log.debug("网络发送命令: ip:{} data={}", ipAddress, command);
        return socketProtocol.receiveData(ipAddress, port);
    }

    @Override
    public byte[] sendRawData(byte[] data) {
        checkConnected();
        updateActivityTime();
        String hexData = HexUtils.bytesToHex(data);
        boolean success = socketProtocol.sendData(ipAddress, port, hexData, DEFAULT_TIMEOUT_MS);
        if (success) {
            log.debug("网络发送原始数据: ip:{} length={}", ipAddress, data.length);
            String response = socketProtocol.receiveData(ipAddress, port);
            if (response != null) {
                return HexUtils.hexToBytes(response);
            }
        }
        return new byte[0];
    }

    @Override
    public String receiveData() {
        return receiveData(DEFAULT_TIMEOUT_MS);
    }

    @Override
    public String receiveData(int timeoutMs) {
        checkConnected();
        String data = socketProtocol.receiveData(ipAddress, port);
        updateActivityTime();
        if (data != null && !data.isEmpty()) {
            log.debug("网络接收数据: ip:{} length={}", ipAddress, data.length());
        }
        return data;
    }

    @Override
    public byte[] receiveRawData() {
        return receiveRawData(DEFAULT_TIMEOUT_MS);
    }

    @Override
    public byte[] receiveRawData(int timeoutMs) {
        String data = receiveData(timeoutMs);
        if (data != null && !data.isEmpty()) {
            return HexUtils.hexToBytes(data);
        }
        return new byte[0];
    }

    @Override
    public Map<String, Object> getDriverInfo() {
        Map<String, Object> info = super.getDriverInfo();
        info.put("ipAddress", ipAddress);
        info.put("port", port);
        return info;
    }

    @Override
    public boolean testConnection() {
        if (!isConnected()) {
            log.warn("网络未连接，无法测试");
            return false;
        }
        boolean result = socketProtocol.isConnected(ipAddress, port);
        log.info("网络连接测试: ip:{} port:{} result={}", ipAddress, port, result ? "成功" : "失败");
        return result;
    }

    private void checkConnected() {
        if (!isConnected()) {
            throw new IllegalStateException("网络未连接: " + ipAddress + ":" + port);
        }
    }
}
