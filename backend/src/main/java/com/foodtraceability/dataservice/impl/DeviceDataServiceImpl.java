package com.foodtraceability.dataservice.impl;

import com.foodtraceability.dataservice.DeviceDataService;
import com.foodtraceability.dto.DeviceVO;
import com.foodtraceability.entity.Device;
import com.foodtraceability.mapper.DeviceMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 设备数据服务实现类
 */
@Service
public class DeviceDataServiceImpl implements DeviceDataService {

    private static final Logger log = LoggerFactory.getLogger(DeviceDataServiceImpl.class);
    private static final String CACHE_NAME = "device";
    private static final long CACHE_EXPIRE_SECONDS = 3600; // 1小时

    private final DeviceMapper deviceMapper;

    public DeviceDataServiceImpl(DeviceMapper deviceMapper) {
        this.deviceMapper = deviceMapper;
    }

    @Override
    @Cacheable(value = CACHE_NAME, key = "#deviceIds", unless = "#result == null || #result.isEmpty()")
    public Map<Long, DeviceVO> batchGetDeviceBasicInfo(List<Long> deviceIds) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Device> devices = deviceMapper.selectBatchIds(deviceIds);
        return devices.stream()
                .collect(Collectors.toMap(
                        Device::getDeviceId,
                        this::convertToVO,
                        (v1, v2) -> v1,
                        LinkedHashMap::new
                ));
    }

    @Override
    @Cacheable(value = CACHE_NAME, key = "'basic:' + #deviceId", unless = "#result == null")
    public DeviceVO getDeviceBasicInfo(Long deviceId) {
        if (deviceId == null) {
            return null;
        }

        Device device = deviceMapper.selectById(deviceId);
        return convertToVO(device);
    }

    @Override
    @CacheEvict(value = CACHE_NAME, key = "'basic:' + #deviceId")
    public void clearDeviceCache(Long deviceId) {
        log.debug("清除设备缓存: deviceId={}", deviceId);
    }

    @Override
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void clearDeviceBatchCache(List<Long> deviceIds) {
        log.debug("批量清除设备缓存: count={}", deviceIds != null ? deviceIds.size() : 0);
    }

    @Override
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void clearAllDeviceCache() {
        log.info("清除所有设备缓存");
    }

    /**
     * 将实体转换为VO
     */
    private DeviceVO convertToVO(Device device) {
        if (device == null) {
            return null;
        }

        DeviceVO vo = new DeviceVO();
        vo.setDeviceId(device.getDeviceId());
        vo.setDeviceCode(device.getDeviceCode());
        vo.setDeviceName(device.getDeviceName());
        vo.setDeviceType(device.getDeviceType());
        vo.setDeviceTypeName(getDeviceTypeName(device.getDeviceType()));
        vo.setDeviceModel(device.getDeviceModel());
        vo.setManufacturer(device.getManufacturer());
        vo.setSerialNo(device.getSerialNo());
        vo.setConnectionType(device.getConnectionType());
        vo.setConnectionTypeName(getConnectionTypeName(device.getConnectionType()));
        vo.setLocation(device.getLocation());
        vo.setStoreId(device.getStoreId());
        vo.setStatus(device.getStatus());
        vo.setStatusName(getStatusName(device.getStatus()));
        vo.setLastHeartbeatTime(device.getLastHeartbeatTime());
        vo.setFirmwareVersion(device.getFirmwareVersion());
        vo.setCreateTime(device.getCreateTime());

        return vo;
    }

    /**
     * 获取设备类型名称
     */
    private String getDeviceTypeName(Integer deviceType) {
        if (deviceType == null) return "未知";
        return switch (deviceType) {
            case 1 -> "打印机";
            case 2 -> "扫码枪";
            case 3 -> "称重秤";
            case 4 -> "取餐柜";
            case 5 -> "其他";
            default -> "未知";
        };
    }

    /**
     * 获取连接类型名称
     */
    private String getConnectionTypeName(Integer connectionType) {
        if (connectionType == null) return "未知";
        return switch (connectionType) {
            case 1 -> "USB";
            case 2 -> "串口";
            case 3 -> "网络";
            case 4 -> "蓝牙";
            default -> "未知";
        };
    }

    /**
     * 获取状态名称
     */
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
