package com.foodtraceability.service.device.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.common.Result;
import com.foodtraceability.dataservice.DeviceDataService;
import com.foodtraceability.dto.*;
import com.foodtraceability.entity.Device;
import com.foodtraceability.entity.DeviceStatusLog;
import com.foodtraceability.mapper.DeviceMapper;
import com.foodtraceability.mapper.DeviceStatusLogMapper;
import com.foodtraceability.service.DeviceService;
import com.foodtraceability.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 设备服务实现类
 */
@Service
public class DeviceServiceImpl extends ServiceImpl<DeviceMapper, Device> implements DeviceService {

    private static final Logger log = LoggerFactory.getLogger(DeviceServiceImpl.class);

    private final DeviceMapper deviceMapper;
    private final DeviceStatusLogMapper deviceStatusLogMapper;
    private final DeviceDataService deviceDataService;
    /** 库存服务：用于设备→仓储跨模块流转（设备故障时查询相关库存预警） */
    private final InventoryService inventoryService;

    public DeviceServiceImpl(DeviceMapper deviceMapper,
                            DeviceStatusLogMapper deviceStatusLogMapper,
                            DeviceDataService deviceDataService,
                            @Lazy InventoryService inventoryService) {
        this.deviceMapper = deviceMapper;
        this.deviceStatusLogMapper = deviceStatusLogMapper;
        this.deviceDataService = deviceDataService;
        this.inventoryService = inventoryService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<DeviceVO> createDevice(DeviceCreateDTO dto) {
        try {
            // 检查设备编号是否已存在
            LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Device::getDeviceCode, dto.getDeviceCode());
            if (deviceMapper.selectCount(wrapper) > 0) {
                return Result.error("设备编号已存在");
            }

            Device device = new Device();
            BeanUtils.copyProperties(dto, device);
            device.setStatus(0); // 默认离线
            device.setConnectionType(dto.getConnectionType() != null ? dto.getConnectionType() : 1);
            deviceMapper.insert(device);

            // 清除缓存
            deviceDataService.clearAllDeviceCache();

            log.info("创建设备成功: deviceCode={}, deviceName={}", dto.getDeviceCode(), dto.getDeviceName());

            // 记录状态日志
            recordStatusLog(device.getDeviceId(), null, 0, 1, "设备创建");

            return Result.success(deviceDataService.getDeviceBasicInfo(device.getDeviceId()), "创建设备成功");
        } catch (Exception e) {
            log.error("创建设备失败", e);
            return Result.error("创建设备失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<DeviceVO> updateDevice(Long deviceId, DeviceUpdateDTO dto) {
        try {
            Device existingDevice = deviceMapper.selectById(deviceId);
            if (existingDevice == null) {
                return Result.error("设备不存在");
            }

            Device device = new Device();
            BeanUtils.copyProperties(dto, device);
            device.setDeviceId(deviceId);
            deviceMapper.updateById(device);

            // 清除缓存
            deviceDataService.clearDeviceCache(deviceId);

            log.info("更新设备成功: deviceId={}", deviceId);

            // 如果有配置变更，记录日志
            if (dto.getConnectionParams() != null || dto.getConfigJson() != null) {
                recordStatusLog(deviceId, existingDevice.getStatus(), existingDevice.getStatus(), 5, "配置变更");
            }

            return Result.success(deviceDataService.getDeviceBasicInfo(deviceId), "更新设备成功");
        } catch (Exception e) {
            log.error("更新设备失败: deviceId={}", deviceId, e);
            return Result.error("更新设备失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteDevice(Long deviceId) {
        try {
            Device device = deviceMapper.selectById(deviceId);
            if (device == null) {
                return Result.error("设备不存在");
            }

            int result = deviceMapper.deleteById(deviceId);
            if (result > 0) {
                // 清除缓存
                deviceDataService.clearDeviceCache(deviceId);
                log.info("删除设备成功: deviceId={}", deviceId);
                return Result.success(null, "删除设备成功");
            }
            return Result.error("删除设备失败");
        } catch (Exception e) {
            log.error("删除设备失败: deviceId={}", deviceId, e);
            return Result.error("删除设备失败：" + e.getMessage());
        }
    }

    @Override
    public Result<DeviceVO> getDeviceById(Long deviceId) {
        try {
            DeviceVO vo = deviceDataService.getDeviceBasicInfo(deviceId);
            if (vo == null) {
                return Result.error("设备不存在");
            }
            return Result.success(vo, "查询设备成功");
        } catch (Exception e) {
            log.error("查询设备失败: deviceId={}", deviceId, e);
            return Result.error("查询设备失败：" + e.getMessage());
        }
    }

    @Override
    public Result<IPage<DeviceVO>> getDevicePage(Page<Device> page, DeviceQueryDTO queryDto) {
        try {
            LambdaQueryWrapper<Device> wrapper = buildQueryWrapper(queryDto);
            wrapper.orderByDesc(Device::getCreateTime);

            IPage<Device> devicePage = deviceMapper.selectPage(page, wrapper);

            // 转换为VO
            IPage<DeviceVO> voPage = devicePage.convert(this::convertToVO);

            return Result.success(voPage, "查询设备列表成功");
        } catch (Exception e) {
            log.error("分页查询设备失败", e);
            return Result.error("查询设备列表失败：" + e.getMessage());
        }
    }

    @Override
    public Result<List<DeviceVO>> getDeviceList(DeviceQueryDTO queryDto) {
        try {
            LambdaQueryWrapper<Device> wrapper = buildQueryWrapper(queryDto);
            wrapper.orderByDesc(Device::getCreateTime);

            List<Device> devices = deviceMapper.selectList(wrapper);
            List<DeviceVO> voList = devices.stream()
                    .map(this::convertToVO)
                    .collect(Collectors.toList());

            return Result.success(voList, "查询设备列表成功");
        } catch (Exception e) {
            log.error("查询设备列表失败", e);
            return Result.error("查询设备列表失败：" + e.getMessage());
        }
    }

    @Override
    public Result<List<DeviceVO>> getOnlineDevices() {
        try {
            LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
            // 数据库 status 列为 varchar，需用字符串 "1" 进行比较（避免 varchar = integer 类型不匹配）
            wrapper.eq(Device::getStatus, "1"); // 在线状态
            wrapper.orderByDesc(Device::getLastOnlineTime);

            List<Device> devices = deviceMapper.selectList(wrapper);
            List<DeviceVO> voList = devices.stream()
                    .map(this::convertToVO)
                    .collect(Collectors.toList());

            return Result.success(voList, "查询在线设备成功");
        } catch (Exception e) {
            log.error("查询在线设备失败", e);
            return Result.error("查询在线设备失败：" + e.getMessage());
        }
    }

    @Override
    public Result<List<DeviceVO>> getDevicesByType(Integer deviceType) {
        try {
            LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Device::getDeviceType, deviceType);
            wrapper.orderByDesc(Device::getCreateTime);

            List<Device> devices = deviceMapper.selectList(wrapper);
            List<DeviceVO> voList = devices.stream()
                    .map(this::convertToVO)
                    .collect(Collectors.toList());

            return Result.success(voList, "按类型查询设备成功");
        } catch (Exception e) {
            log.error("按类型查询设备失败: deviceType={}", deviceType, e);
            return Result.error("按类型查询设备失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateDeviceStatus(Long deviceId, Integer status) {
        try {
            Device device = deviceMapper.selectById(deviceId);
            if (device == null) {
                return Result.error("设备不存在");
            }

            Integer oldStatus = device.getStatus();
            device.setStatus(status);
            device.setUpdateTime(LocalDateTime.now());
            deviceMapper.updateById(device);

            // 清除缓存
            deviceDataService.clearDeviceCache(deviceId);

            // 记录状态变更日志
            int eventType = determineEventType(oldStatus, status);
            recordStatusLog(deviceId, oldStatus, status, eventType, getStatusChangeMessage(oldStatus, status));

            log.info("更新设备状态成功: deviceId={}, oldStatus={}, newStatus={}", deviceId, oldStatus, status);
            return Result.success(null, "更新设备状态成功");
        } catch (Exception e) {
            log.error("更新设备状态失败: deviceId={}", deviceId, e);
            return Result.error("更新设备状态失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateHeartbeat(Long deviceId) {
        try {
            Device device = deviceMapper.selectById(deviceId);
            if (device == null) {
                return Result.error("设备不存在");
            }

            Integer oldStatus = device.getStatus();
            LocalDateTime now = LocalDateTime.now();
            device.setLastHeartbeatTime(now);

            // 如果之前是离线状态，更新为在线
            if (oldStatus == null || oldStatus == 0) {
                device.setStatus(1); // 在线
            }

            device.setUpdateTime(now);
            deviceMapper.updateById(device);

            // 清除缓存
            deviceDataService.clearDeviceCache(deviceId);

            // 状态从离线变更为上线时记录日志
            if (oldStatus != null && oldStatus == 0 && device.getStatus() == 1) {
                recordStatusLog(deviceId, 0, 1, 1, "设备上线");
                log.info("设备上线: deviceId={}", deviceId);
            }

            return Result.success(null, "心跳更新成功");
        } catch (Exception e) {
            log.error("更新设备心跳失败: deviceId={}", deviceId, e);
            return Result.error("更新心跳失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> toggleDeviceEnabled(Long deviceId, boolean enabled) {
        try {
            Device device = deviceMapper.selectById(deviceId);
            if (device == null) {
                return Result.error("设备不存在");
            }

            // enabled=true 表示启用（非维护中），enabled=false 表示停用（维护中）
            Integer newStatus = enabled ? 0 : 3; // 0-离线, 3-维护中
            Integer oldStatus = device.getStatus();

            device.setStatus(newStatus);
            device.setUpdateTime(LocalDateTime.now());
            deviceMapper.updateById(device);

            // 清除缓存
            deviceDataService.clearDeviceCache(deviceId);

            // 记录日志
            String message = enabled ? "设备启用" : "设备停用（维护中）";
            recordStatusLog(deviceId, oldStatus, newStatus, 5, message);

            log.info("{}设备成功: deviceId={}", enabled ? "启用" : "停用", deviceId);
            return Result.success(null, message + "成功");
        } catch (Exception e) {
            log.error("{}设备失败: deviceId={}", enabled ? "启用" : "停用", deviceId, e);
            return Result.error((enabled ? "启用" : "停用") + "设备失败：" + e.getMessage());
        }
    }

    /**
     * 设备故障时查询相关门店库存预警（设备→仓储跨模块流转）
     * <p>当设备（如称重秤/扫码枪）故障时，调用方可通过此方法获取门店低库存列表，
     * 用于在设备故障期间提示人工补货或备货。</p>
     *
     * @param deviceId 故障设备ID
     * @return 低库存列表
     */
    public Result<List<com.foodtraceability.entity.Inventory>> getDeviceStoreLowStock(Long deviceId) {
        try {
            Device device = deviceMapper.selectById(deviceId);
            if (device == null) {
                return Result.error("设备不存在");
            }
            // 设备→仓储流转：通过 InventoryService.getLowStockList 查询低库存
            // warehouseId 为 null 时查询所有仓库
            List<com.foodtraceability.entity.Inventory> lowStockList = inventoryService.getLowStockList(null);
            log.info("设备故障库存预警查询: deviceId={}, storeId={}, lowStockCount={}",
                    deviceId, device.getStoreId(), lowStockList.size());
            return Result.success(lowStockList, "查询设备门店库存预警成功");
        } catch (Exception e) {
            log.error("查询设备门店库存预警失败: deviceId={}", deviceId, e);
            return Result.error("查询设备门店库存预警失败：" + e.getMessage());
        }
    }

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<Device> buildQueryWrapper(DeviceQueryDTO queryDto) {
        LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();

        if (queryDto == null) {
            return wrapper;
        }

        if (StringUtils.hasText(queryDto.getDeviceCode())) {
            wrapper.like(Device::getDeviceCode, queryDto.getDeviceCode());
        }
        if (StringUtils.hasText(queryDto.getDeviceName())) {
            wrapper.like(Device::getDeviceName, queryDto.getDeviceName());
        }
        if (queryDto.getDeviceType() != null) {
            wrapper.eq(Device::getDeviceType, queryDto.getDeviceType());
        }
        if (queryDto.getStoreId() != null) {
            wrapper.eq(Device::getStoreId, queryDto.getStoreId());
        }
        if (queryDto.getStatus() != null) {
            // 数据库 status 列为 varchar，将 Integer 转为字符串进行比较，避免类型不匹配
            wrapper.eq(Device::getStatus, String.valueOf(queryDto.getStatus()));
        }
        if (queryDto.getConnectionType() != null) {
            wrapper.eq(Device::getConnectionType, queryDto.getConnectionType());
        }

        return wrapper;
    }

    /**
     * 转换为VO
     */
    private DeviceVO convertToVO(Device device) {
        return deviceDataService.getDeviceBasicInfo(device.getDeviceId());
    }

    /**
     * 记录状态变更日志
     */
    private void recordStatusLog(Long deviceId, Integer oldStatus, Integer newStatus,
                                  Integer eventType, String message) {
        try {
            DeviceStatusLog log = new DeviceStatusLog();
            log.setDeviceId(deviceId);
            log.setOldStatus(oldStatus);
            log.setNewStatus(newStatus);
            log.setEventType(eventType);
            log.setMessage(message);
            log.setCreateTime(LocalDateTime.now());
            deviceStatusLogMapper.insert(log);
        } catch (Exception e) {
            log.error("记录设备状态日志失败: deviceId={}", deviceId, e);
        }
    }

    /**
     * 根据状态变更确定事件类型
     */
    private int determineEventType(Integer oldStatus, Integer newStatus) {
        if (newStatus == 1) return 1;      // 上线
        if (newStatus == 0) return 2;      // 离线
        if (newStatus == 2) return 3;      // 故障
        if (newStatus == 3) return 5;      // 维护中
        if (oldStatus != null && oldStatus == 2 && newStatus == 1) return 4; // 恢复
        return 5; // 配置变更
    }

    /**
     * 获取状态变更消息
     */
    private String getStatusChangeMessage(Integer oldStatus, Integer newStatus) {
        return String.format("状态从[%s]变更为[%s]",
                getStatusName(oldStatus), getStatusName(newStatus));
    }

    private String getStatusName(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "离线";
            case 1 -> "在线";
            case 2 -> "故障";
            case 3 -> "维护中";
            default -> "未知";
        };
    }
}
