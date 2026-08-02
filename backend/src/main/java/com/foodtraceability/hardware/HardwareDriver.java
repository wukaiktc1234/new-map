package com.foodtraceability.hardware;

import com.foodtraceability.entity.HardwareConfig;
import java.util.Map;

/**
 * 硬件驱动基础接口
 * <p>
 * 定义所有硬件驱动的核心生命周期方法和信息查询方法。
 * 遵循接口隔离原则（ISP），仅包含所有驱动都必须实现的基础能力。
 * </p>
 *
 * <p>设计原则：</p>
 * <ul>
 *   <li>单一职责：只关注连接管理和设备信息</li>
 *   <li>接口隔离：文本/二进制通信能力通过子接口扩展</li>
 *   <li>开闭原则：新增协议类型只需扩展新子接口，无需修改此接口</li>
 * </ul>
 *
 * @see TextBasedDriver 文本协议驱动
 * @see BinaryDriver 二进制协议驱动
 */
public interface HardwareDriver {

    /**
     * 获取驱动类型标识
     * <p>驱动类型用于在驱动注册表中唯一标识驱动实现类，通常与连接方式对应。</p>
     *
     * @return 驱动类型字符串，如 "SERIAL"、"NETWORK"、"USB"，不可为null或空字符串
     */
    String getDriverType();

    /**
     * 获取支持的连接类型
     * <p>连接类型表示该驱动所支持的物理/逻辑连接方式，用于配置匹配和驱动选择。</p>
     *
     * @return 连接类型字符串，如 "SERIAL"、"NETWORK"、"USB"，不可为null或空字符串
     */
    String getSupportedConnectionType();

    /**
     * 连接到硬件设备
     * <p>根据提供的配置信息建立与硬件设备的物理或逻辑连接。
     * 连接成功后，{@link #isConnected()} 应返回 true。</p>
     *
     * @param config 设备配置信息，包含连接参数（如端口、IP地址等），不能为null
     * @return true 表示连接成功；false 表示连接失败（如端口被占用、网络不通等）
     * @throws IllegalArgumentException 当 config 参数不合法时抛出（由 {@link #validateConfig} 检查）
     */
    boolean connect(HardwareConfig config);

    /**
     * 断开硬件设备连接
     * <p>释放当前占用的资源并断开与设备的连接。调用后 {@link #isConnected()} 应返回 false。
     * 此方法应支持重复调用（幂等性）。</p>
     *
     * @throws IllegalStateException 当驱动未处于已连接状态时的可选行为
     */
    void disconnect();

    /**
     * 检查驱动是否已连接到硬件设备
     * <p>此方法应线程安全地返回当前连接状态。</p>
     *
     * @return true 表示当前已连接且可用；false 表示未连接或连接已断开
     */
    boolean isConnected();

    /**
     * 获取当前设备配置
     * <p>返回最近一次成功 {@link #connect(HardwareConfig)} 时使用的配置对象。</p>
     *
     * @return 设备配置对象；如果从未成功连接过则返回 null
     */
    HardwareConfig getDeviceConfig();

    /**
     * 获取驱动详细状态信息
     * <p>返回包含驱动运行时状态的只读快照，可用于监控和诊断。
     * 返回的Map至少包含以下key：</p>
     * <ul>
     *   <li>{@code driverType} - 驱动类型标识</li>
     *   <li>{@code supportedConnectionType} - 支持的连接类型</li>
     *   <li>{@code connected} (Boolean) - 当前连接状态</li>
     *   <li>{@code lastActivityTime} (long) - 最后活动时间戳（毫秒）</li>
     * </ul>
     * <p>子类可在此基础上追加额外信息（如端口号、波特率等）。</p>
     *
     * @return 包含驱动状态信息的Map，key为String类型，value可为任意类型；不会返回null
     */
    Map<String, Object> getDriverInfo();

    /**
     * 重置驱动状态
     * <p>执行以下操作以将驱动恢复到初始状态：</p>
     * <ol>
     *   <li>调用 {@link #disconnect()} 断开现有连接</li>
     *   <li>清除连接状态标志</li>
     *   <li>清空缓存的设备配置引用</li>
     * </ol>
     * <p>重置后可通过 {@link #connect(HardwareConfig)} 重新建立连接。</p>
     */
    void reset();

    /**
     * 测试设备连接是否正常工作
     * <p>向设备发送测试指令并验证响应，用于健康检查和故障诊断。
     * 此方法不应改变驱动的内部状态。</p>
     *
     * @return true 表示连接正常且设备有响应；false 表示无响应或连接异常
     */
    boolean testConnection();

    /**
     * 验证设备配置的有效性
     * <p>检查配置参数是否满足该驱动的最低要求，包括必填字段校验、
     * 连接类型匹配、参数范围验证等。建议在 {@link #connect(HardwareConfig)} 之前调用。</p>
     *
     * @param config 待验证的设备配置对象，不能为null
     * @return true 表示配置有效且可用于连接
     * @throws IllegalArgumentException 当以下情况时抛出：
     *         <ul>
     *           <li>config 为 null</li>
     *           <li>deviceType 为空或缺失</li>
     *           <li>connectionType 与驱动不支持的类型不匹配</li>
     *         </ul>
     */
    boolean validateConfig(HardwareConfig config);
}
