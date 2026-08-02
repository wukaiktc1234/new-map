package com.foodtraceability.hardware;

/**
 * 二进制协议驱动接口
 * <p>
 * 扩展基础驱动接口，提供基于原始字节的数据发送和接收能力。
 * 适用于需要二进制协议通信的硬件设备（如固件升级、图像传输等场景）。
 * </p>
 *
 * <p>与 {@link TextBasedDriver} 的区别：</p>
 * <ul>
 *   <li>本接口处理的是原始字节数组（byte[]），可包含任意二进制数据</li>
 *   <li>{@link TextBasedDriver} 处理的是文本字符串，仅适用于ASCII/UTF-8协议</li>
 *   <li>本接口适用于固件升级、图像采集、自定义二进制协议等场景</li>
 *   <li>文本接口适用于AT指令、ESC/POS打印指令等简单命令-响应模式</li>
 * </ul>
 *
 * <p><strong>Raw Data 编码规范：</strong></p>
 * <p>所有实现类在传输原始字节数据时，必须统一使用十六进制（Hex）编码格式，
 * 即将字节数组转换为大写十六进制字符串进行传输。接收时再将十六进制字符串
 * 还原为字节数组。此规范确保不同实现类的 raw data 行为一致。</p>
 *
 * <p>典型应用场景：</p>
 * <ul>
 *   <li>固件升级（传输二进制固件文件）</li>
 *   <li>图像/视频数据采集</li>
 *   <li>自定义二进制协议通信</li>
 *   <li>低级别硬件控制指令</li>
 * </ul>
 *
 * @see HardwareDriver 基础驱动接口
 * @see TextBasedDriver 文本协议驱动
 */
public interface BinaryDriver extends HardwareDriver {

    /**
     * 发送原始字节数据
     * <p>向设备发送二进制格式的原始字节数据。内部应使用十六进制（Hex）编码
     * 将字节数组转换为字符串后通过底层协议传输。</p>
     *
     * @param data 要发送的字节数组，不能为null；空数组表示发送空数据
     * @return 设备响应的字节数组（已从十六进制还原）；发送失败或无响应时返回空数组
     * @throws IllegalStateException 当驱动未连接时抛出
     */
    byte[] sendRawData(byte[] data);

    /**
     * 接收原始字节数据（使用默认超时时间）
     * <p>从设备接收二进制数据并将其转换为字节数组返回。
     * 默认超时时间由各实现类自行定义，通常为5000毫秒。</p>
     *
     * @return 接收到的字节数组（已从十六进制还原）；无数据时返回空数组
     * @throws IllegalStateException 当驱动未连接时抛出
     */
    byte[] receiveRawData();

    /**
     * 接收原始字节数据（指定超时时间）
     * <p>从设备接收二进制数据并在指定的超时时间内等待完成。
     * 适用于需要精确控制接收超时的场景。</p>
     *
     * @param timeoutMs 最大等待时间（毫秒），必须大于0
     * @return 接收到的字节数组（已从十六进制还原）；超时或无数据时返回空数组
     * @throws IllegalStateException 当驱动未连接时抛出
     */
    byte[] receiveRawData(int timeoutMs);
}
