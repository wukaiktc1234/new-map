package com.foodtraceability.hardware;

import com.foodtraceability.entity.HardwareConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DriverRegistry {
    private static final Logger log = LoggerFactory.getLogger(DriverRegistry.class);
    private final Map<String, HardwareDriver> drivers = new ConcurrentHashMap<>();
    private final Map<String, Class<? extends HardwareDriver>> driverTypes = new ConcurrentHashMap<>();
    private final List<HardwareDriver> autoDetectedDrivers;

    /**
     * 构造函数注入（禁止 @Autowired 字段注入）
     * @param autoDetectedDrivers 自动检测的硬件驱动列表，可选依赖
     */
    public DriverRegistry(@Nullable List<HardwareDriver> autoDetectedDrivers) {
        this.autoDetectedDrivers = autoDetectedDrivers;
    }

    @PostConstruct
    public void init() {
        log.info("硬件驱动注册中心初始化");
        if (autoDetectedDrivers != null) {
            for (HardwareDriver driver : autoDetectedDrivers) {
                registerDriver(driver);
            }
            log.info("自动检测并注册了{}个硬件驱动", autoDetectedDrivers.size());
        }
    }

    @PreDestroy
    public void destroy() {
        log.info("硬件驱动注册中心关闭，断开所有驱动连接");
        for (Map.Entry<String, HardwareDriver> entry : drivers.entrySet()) {
            try {
                entry.getValue().disconnect();
                log.info("驱动已断开: {}", entry.getKey());
            } catch (Exception e) {
                log.warn("断开驱动失败: {} 错误={}", entry.getKey(), e.getMessage());
            }
        }
        drivers.clear();
    }

    /**
     * 注册硬件驱动实例
     * <p>将驱动添加到注册表中，使用驱动类型和连接类型组合作为唯一key。</p>
     *
     * @param driver 要注册的驱动实例，不能为null
     * @throws IllegalArgumentException 当 driver 为null时抛出
     */
    public void registerDriver(HardwareDriver driver) {
        if (driver == null) {
            throw new IllegalArgumentException("驱动实例不能为null");
        }
        String key = buildKey(driver.getDriverType(), driver.getSupportedConnectionType());

        // 重复注册检测：如果同一key已存在则记录警告
        if (drivers.containsKey(key)) {
            log.warn("检测到重复注册: key={} 已存在驱动[{}]，将被新驱动[{}]",
                key, drivers.get(key).getClass().getSimpleName(), driver.getClass().getSimpleName());
        }

        drivers.put(key, driver);
        driverTypes.put(driver.getDriverType(), driver.getClass());
        log.info("注册硬件驱动: type={} connectionType={} class={}",
            driver.getDriverType(), driver.getSupportedConnectionType(), driver.getClass().getSimpleName());
    }

    public void unregisterDriver(String driverType, String connectionType) {
        String key = buildKey(driverType, connectionType);
        HardwareDriver removed = drivers.remove(key);
        if (removed != null) {
            try {
                removed.disconnect();
            } catch (Exception e) {
                log.warn("断开驱动失败: {}", key);
            }
            driverTypes.remove(driverType);
            log.info("注销硬件驱动: {}", key);
        }
    }

    public HardwareDriver getDriver(String driverType, String connectionType) {
        return drivers.get(buildKey(driverType, connectionType));
    }

    public HardwareDriver getDriverOrThrow(String driverType, String connectionType) {
        HardwareDriver driver = getDriver(driverType, connectionType);
        if (driver == null) {
            throw new NoSuchElementException("未找到驱动: type=" + driverType + ", connectionType=" + connectionType);
        }
        return driver;
    }

    /**
     * 根据设备配置获取匹配的驱动实例
     * <p>通过配置中的连接类型或设备类型推断结果，在已注册驱动中查找匹配项。</p>
     *
     * @param config 设备配置信息
     * @return 匹配的驱动实例；无法匹配时返回null（不再硬编码默认值）
     */
    public HardwareDriver getDriverByConfig(HardwareConfig config) {
        if (config == null || config.getDeviceType() == null) {
            return null;
        }
        String connectionType = config.getConnectionType();

        // 当连接类型为空时，尝试根据设备类型推断合理的连接类型
        if (connectionType == null || connectionType.isEmpty()) {
            connectionType = inferConnectionType(config.getDeviceType());
            if (connectionType == null) {
                log.warn("无法为设备类型[{}]推断连接类型，请明确指定connectionType。"
                    + "当前已注册的连接类型: {}",
                    config.getDeviceType(), getRegisteredDriverTypes());
                return null;
            }
            log.info("根据设备类型[{}]自动推断连接类型为: {}", config.getDeviceType(), connectionType);
        }

        for (HardwareDriver driver : drivers.values()) {
            if (driver.getSupportedConnectionType().equalsIgnoreCase(connectionType)) {
                return driver;
            }
        }
        log.debug("未找到匹配连接类型[{}]的驱动", connectionType);
        return null;
    }

    /**
     * 根据设备类型推断合理的连接类型
     * <p>基于常见硬件设备的典型连接方式进行推断。
     * 此方法仅作为兜底策略，建议调用方明确指定connectionType。</p>
     *
     * @param deviceType 设备类型字符串（如 "PRINTER"、"SCANNER"、"SCALE" 等）
     * @return 推断的连接类型；无法推断时返回null
     */
    private String inferConnectionType(String deviceType) {
        if (deviceType == null) {
            return null;
        }
        String upperType = deviceType.toUpperCase();
        // 根据常见设备类型推断默认连接方式
        switch (upperType) {
            case "PRINTER":
                // 打印机通常支持网络和USB/串口连接，优先尝试USB/SERIAL
                return tryFindAvailableDriver("SERIAL") != null ? "SERIAL"
                    : tryFindAvailableDriver("USB") != null ? "USB"
                    : tryFindAvailableDriver("NETWORK");
            case "SCANNER":
                // 扫描仪通常使用USB或串口连接
                return tryFindAvailableDriver("USB") != null ? "USB"
                    : tryFindAvailableDriver("SERIAL") != null ? "SERIAL"
                    : null;
            case "SCALE":
            case "DISPLAY":
                // 电子秤和显示屏通常使用串口连接
                return tryFindAvailableDriver("SERIAL") != null ? "SERIAL" : null;
            default:
                // 未知设备类型：不进行猜测，返回null让调用方处理
                return null;
        }
    }

    /**
     * 检查是否存在指定连接类型的可用驱动
     *
     * @param connectionType 要检查的连接类型
     * @return 存在时返回该连接类型；不存在时返回null
     */
    private String tryFindAvailableDriver(String connectionType) {
        for (HardwareDriver driver : drivers.values()) {
            if (driver.getSupportedConnectionType().equalsIgnoreCase(connectionType)) {
                return connectionType;
            }
        }
        return null;
    }

    public List<HardwareDriver> getDriversByType(String driverType) {
        List<HardwareDriver> result = new ArrayList<>();
        for (Map.Entry<String, HardwareDriver> entry : drivers.entrySet()) {
            if (entry.getKey().startsWith(driverType + ":")) {
                result.add(entry.getValue());
            }
        }
        return result;
    }

    public List<HardwareDriver> getAllDrivers() {
        return new ArrayList<>(drivers.values());
    }

    public Set<String> getRegisteredDriverTypes() {
        return Collections.unmodifiableSet(driverTypes.keySet());
    }

    public Map<String, Object> getRegistryStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("totalDrivers", drivers.size());
        status.put("registeredTypes", new ArrayList<>(driverTypes.keySet()));
        List<Map<String, Object>> driverInfos = new ArrayList<>();
        for (Map.Entry<String, HardwareDriver> entry : drivers.entrySet()) {
            driverInfos.add(entry.getValue().getDriverInfo());
        }
        status.put("drivers", driverInfos);
        return status;
    }

    private String buildKey(String driverType, String connectionType) {
        return driverType + ":" + (connectionType != null ? connectionType.toUpperCase() : "UNKNOWN");
    }
}
