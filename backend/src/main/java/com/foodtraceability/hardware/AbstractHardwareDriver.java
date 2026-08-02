package com.foodtraceability.hardware;

import com.foodtraceability.entity.HardwareConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 硬件驱动抽象基类
 * <p>
 * 提供 {@link HardwareDriver} 接口的通用实现，封装了连接状态管理、
 * 配置缓存、活动时间跟踪等基础功能。子类只需关注具体的连接/通信逻辑。
 * </p>
 *
 * <p>设计特点：</p>
 * <ul>
 *   <li><strong>线程安全</strong>：使用 {@link AtomicBoolean} 和 {@link AtomicLong} 保证并发安全</li>
 *   <li><strong>模板方法模式</strong>：定义了通用的生命周期骨架，子类实现具体细节</li>
 *   <li><strong>配置校验</strong>：内置 {@link #validateConfig(HardwareConfig)} 默认实现</li>
 *   <li><strong>活动追踪</strong>：自动记录最后活动时间，用于健康检查和超时检测</li>
 * </ul>
 *
 * <p>子类应实现：</p>
 * <ul>
 *   <li>{@link #connect(HardwareConfig)} - 建立具体连接</li>
 *   <li>{@link #disconnect()} - 断开并释放资源</li>
 *   <li>{@link #testConnection()} - 实现具体的连通性测试</li>
 * </ul>
 *
 * @see TextBasedDriver 文本协议子接口
 * @see BinaryDriver 二进制协议子接口
 */
public abstract class AbstractHardwareDriver implements HardwareDriver {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final AtomicBoolean connected = new AtomicBoolean(false);
    protected HardwareConfig config;
    private final String driverType;
    private final String supportedConnectionType;
    private final AtomicLong lastActivityTime = new AtomicLong(0);

    protected AbstractHardwareDriver(String driverType, String supportedConnectionType) {
        this.driverType = driverType;
        this.supportedConnectionType = supportedConnectionType;
    }

    @Override
    public String getDriverType() {
        return driverType;
    }

    @Override
    public String getSupportedConnectionType() {
        return supportedConnectionType;
    }

    @Override
    public boolean isConnected() {
        return connected.get();
    }

    @Override
    public HardwareConfig getDeviceConfig() {
        return config;
    }

    /**
     * 获取驱动详细状态信息
     * <p>返回包含驱动运行时状态的Map快照。子类可重写此方法以追加额外信息。</p>
     *
     * <p>返回的Map包含以下基础key：</p>
     * <ul>
     *   <li>{@code driverType} (String) - 驱动类型标识，如 "SERIAL"、"NETWORK"</li>
     *   <li>{@code supportedConnectionType} (String) - 支持的连接类型</li>
     *   <li>{@code connected} (Boolean) - 当前连接状态</li>
     *   <li>{@code lastActivityTime} (long) - 最后活动时间的毫秒时间戳（自1970-01-01起）</li>
     * </ul>
     * <p>如果当前已连接且有配置，还包含：</p>
     * <ul>
     *   <li>{@code deviceName} (String) - 设备名称</li>
     *   <li>{@code deviceType} (String) - 设备类型</li>
     * </ul>
     *
     * @return 包含驱动状态信息的Map，不会返回null
     */
    @Override
    public Map<String, Object> getDriverInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("driverType", driverType);
        info.put("supportedConnectionType", supportedConnectionType);
        info.put("connected", isConnected());
        info.put("lastActivityTime", lastActivityTime.get());
        if (config != null) {
            info.put("deviceName", config.getDeviceName());
            info.put("deviceType", config.getDeviceType());
        }
        return info;
    }

    /**
     * 重置驱动状态到初始状态
     * <p>执行以下操作将驱动完全恢复到未连接的初始状态：</p>
     * <ol>
     *   <li>调用 {@link #disconnect()} 断开当前连接（如有）</li>
     *   <li>将连接状态标志 {@code connected} 设置为 false</li>
     *   <li>清空缓存的设备配置引用（config 置为 null）</li>
     * </ol>
     * <p><strong>注意：</strong>此方法不重置驱动类型和连接类型等构造时确定的属性。
     * 重置后可通过 {@link #connect(HardwareConfig)} 重新建立连接。</p>
     */
    @Override
    public void reset() {
        disconnect();
        connected.set(false);
        config = null;
        log.info("{}驱动已重置", driverType);
    }

    /**
     * 更新最后活动时间戳
     * <p>将最后活动时间更新为当前系统时间（毫秒）。
     * 此方法应在每次成功的发送或接收操作后调用，用于：</p>
     * <ul>
     *   <li>健康检查：判断驱动是否长时间无活动</li>
     *   <li>超时检测：识别可能已断开的连接</li>
     *   <li>监控统计：记录驱动的活跃程度</li>
     * </ul>
     *
     * <p><strong>线程安全说明：</strong>此方法使用 {@link AtomicLong#set(long)} 更新时间戳，
     * 保证在多线程环境下的原子性和可见性。子类在重写时应确保同样具备线程安全性。</p>
     */
    protected void updateActivityTime() {
        this.lastActivityTime.set(System.currentTimeMillis());
    }

    /**
     * 验证设备配置的有效性
     * 实现 HardwareDriver 接口的 validateConfig 方法
     *
     * @param config 待验证的配置
     * @return 配置是否有效（验证通过返回true）
     * @throws IllegalArgumentException 配置参数不合法时抛出
     */
    @Override
    public boolean validateConfig(HardwareConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("设备配置不能为空");
        }
        if (config.getDeviceType() == null || config.getDeviceType().isEmpty()) {
            throw new IllegalArgumentException("设备类型不能为空");
        }
        if (config.getConnectionType() == null || !config.getConnectionType().equalsIgnoreCase(supportedConnectionType)) {
            throw new IllegalArgumentException("不支持的连接类型: " + config.getConnectionType()
                + ", 期望: " + supportedConnectionType);
        }
        return true;
    }
}
