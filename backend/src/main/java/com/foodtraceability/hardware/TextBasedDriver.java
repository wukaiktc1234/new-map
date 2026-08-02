package com.foodtraceability.hardware;

/**
 * 文本协议驱动接口
 * <p>
 * 扩展基础驱动接口，提供基于文本的命令发送和数据接收能力。
 * 适用于串口、网络等支持文本协议通信的硬件设备。
 * </p>
 *
 * <p>与 {@link BinaryDriver} 的区别：</p>
 * <ul>
 *   <li>本接口处理的数据为可读的文本字符串（ASCII/UTF-8编码）</li>
 *   <li>{@link BinaryDriver} 处理的是原始字节数据，适用于二进制协议</li>
 *   <li>文本协议通常用于简单的命令-响应模式（如AT指令、ESC/POS指令）</li>
 *   <li>二进制协议常用于固件升级、图像传输等需要精确字节控制的场景</li>
 * </ul>
 *
 * <p>典型应用场景：</p>
 * <ul>
 *   <li>串口设备（如打印机、扫描仪）</li>
 *   <li>网络设备（如TCP/IP打印机）</li>
 *   <li>任何基于ASCII文本协议的硬件</li>
 * </ul>
 *
 * @see HardwareDriver 基础驱动接口
 * @see BinaryDriver 二进制协议驱动
 */
public interface TextBasedDriver extends HardwareDriver {

    /**
     * 发送文本命令并等待响应（使用默认超时时间）
     * <p>向设备发送文本格式的命令字符串，同步等待设备返回响应数据。
     * 默认超时时间由各实现类自行定义，通常为5000毫秒。</p>
     *
     * @param command 要发送的命令字符串，不能为null或空字符串
     * @return 设备响应的文本数据；发送失败或超时时返回null
     * @throws IllegalStateException 当驱动未连接时抛出
     */
    String sendCommand(String command);

    /**
     * 发送文本命令并等待响应（指定超时时间）
     * <p>向设备发送文本格式的命令字符串，在指定的超时时间内等待设备响应。
     * 超时后无论是否收到完整响应都将立即返回。</p>
     *
     * @param command 要发送的命令字符串，不能为null或空字符串
     * @param timeoutMs 超时时间（毫秒），必须大于0
     * @return 设备响应的文本数据；发送失败或超时时返回null
     * @throws IllegalStateException 当驱动未连接时抛出
     * @throws IllegalArgumentException 当 timeoutMs 小于等于0时抛出
     */
    String sendCommand(String command, int timeoutMs);

    /**
     * 接收文本数据（使用默认超时时间）
     * <p>从设备的接收缓冲区读取文本数据。此方法为非阻塞式接收，
     * 如果缓冲区无数据则立即返回。默认超时由实现类定义。</p>
     *
     * @return 接收到的文本数据；无数据时返回null或空字符串
     * @throws IllegalStateException 当驱动未连接时抛出
     */
    String receiveData();

    /**
     * 接收文本数据（指定超时时间）
     * <p>从设备的接收缓冲区读取文本数据，最多等待指定的超时时间。
     * 适用于需要主动轮询设备数据的场景。</p>
     *
     * @param timeoutMs 最大等待时间（毫秒），必须大于0
     * @return 接收到的文本数据；超时或无数据时返回null或空字符串
     * @throws IllegalStateException 当驱动未连接时抛出
     */
    String receiveData(int timeoutMs);
}
