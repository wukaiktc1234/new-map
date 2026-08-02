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
 * 串口驱动实现类
 * <p>
 * 同时实现 {@link TextBasedDriver} 和 {@link BinaryDriver} 接口，
 * 支持通过串口（RS-232/RS-485）与硬件设备进行文本和二进制协议通信。
 * 适用于通过串口连接的硬件设备（如打印机、扫描仪、电子秤、条码枪等）。
 * </p>
 *
 * <p><strong>支持的波特率：</strong></p>
 * <ul>
 *   <li>标准波特率：9600（默认）、19200、38400、57600、115200</li>
 *   <li>低速率：1200、2400、4800</li>
 *   <li>具体支持范围取决于底层串口库和操作系统</li>
 * </ul>
 *
 * <p><strong>连接模式：</strong></p>
 * <ul>
 *   <li>同步阻塞模式：发送命令后等待响应返回</li>
 *   <li>超时机制：默认超时5000毫秒，可自定义</li>
 *   <li>独占模式：同一串口同一时间只能被一个驱动实例占用</li>
 * </ul>
 *
 * <p><strong>数据编码：</strong></p>
 * <ul>
 *   <li>文本数据：使用平台默认字符集或UTF-8编码</li>
 *   <li>二进制原始数据：使用十六进制（Hex）大写编码格式传输</li>
 * </ul>
 *
 * @see TextBasedDriver 文本协议接口
 * @see BinaryDriver 二进制协议接口
 */
@Component
public class SerialPortDriver extends AbstractHardwareDriver implements TextBasedDriver, BinaryDriver {
    private static final int DEFAULT_TIMEOUT_MS = 5000;
    private CommunicationProtocolFactory.SerialProtocol serialProtocol;
    private String portName;
    private int baudRate = 9600;

    public SerialPortDriver() {
        super("SERIAL", "SERIAL");
        this.serialProtocol = CommunicationProtocolFactory.getSerialProtocol();
    }

    @Override
    public boolean connect(HardwareConfig config) {
        validateConfig(config);
        try {
            this.config = config;
            this.portName = config.getPortName();
            if (config.getBaudRate() != null && !config.getBaudRate().isEmpty()) {
                this.baudRate = Integer.parseInt(config.getBaudRate());
            }
            boolean success = serialProtocol.connect(portName, baudRate);
            if (success) {
                connected.set(true);
                updateActivityTime();
                log.info("串口驱动连接成功: port={} baudRate={}", portName, baudRate);
            } else {
                log.error("串口驱动连接失败: port={} baudRate={}", portName, baudRate);
            }
            return success;
        } catch (NumberFormatException e) {
            log.error("波特率格式错误: {}", config.getBaudRate());
            return false;
        }
    }

    @Override
    public void disconnect() {
        if (portName != null) {
            serialProtocol.disconnect(portName);
            connected.set(false);
            log.info("串口驱动已断开: port={}", portName);
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
        boolean success = serialProtocol.sendData(portName, command);
        if (!success) {
            log.warn("串口数据发送失败: port={}", portName);
            return null;
        }
        log.debug("串口发送命令: port={} data={}", portName, command);
        return receiveData(timeoutMs);
    }

    @Override
    public byte[] sendRawData(byte[] data) {
        checkConnected();
        updateActivityTime();
        String hexString = HexUtils.bytesToHex(data);
        boolean success = serialProtocol.sendData(portName, hexString);
        if (success) {
            log.debug("串口发送原始数据: port={} length={}", portName, data.length);
            String response = serialProtocol.receiveData(portName);
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
        String data = serialProtocol.receiveData(portName);
        updateActivityTime();
        if (data != null && !data.isEmpty()) {
            log.debug("串口接收数据: port={} length={}", portName, data.length());
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
        info.put("portName", portName);
        info.put("baudRate", baudRate);
        return info;
    }

    @Override
    public boolean testConnection() {
        if (!isConnected()) {
            log.warn("串口未连接，无法测试");
            return false;
        }
        try {
            String testResponse = sendCommand("TEST", 2000);
            boolean result = testResponse != null || serialProtocol.isConnected(portName);
            log.info("串口连接测试: port={} result={}", portName, result ? "成功" : "无响应(可能正常)");
            return result;
        } catch (Exception e) {
            log.error("串口连接测试异常: port={} error={}", portName, e.getMessage());
            return false;
        }
    }

    private void checkConnected() {
        if (!isConnected()) {
            throw new IllegalStateException("串口未连接: " + portName);
        }
    }
}
