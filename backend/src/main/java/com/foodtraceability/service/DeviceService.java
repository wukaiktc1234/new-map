package com.foodtraceability.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.DeviceCreateDTO;
import com.foodtraceability.dto.DeviceQueryDTO;
import com.foodtraceability.dto.DeviceUpdateDTO;
import com.foodtraceability.dto.DeviceVO;
import com.foodtraceability.entity.Device;

import java.util.List;
import java.util.Map;

public interface DeviceService extends IService<Device> {

    Result<DeviceVO> createDevice(DeviceCreateDTO dto);

    Result<DeviceVO> updateDevice(Long deviceId, DeviceUpdateDTO dto);

    Result<Void> deleteDevice(Long deviceId);

    Result<DeviceVO> getDeviceById(Long deviceId);

    Result<IPage<DeviceVO>> getDevicePage(Page<Device> page, DeviceQueryDTO queryDto);

    Result<List<DeviceVO>> getDeviceList(DeviceQueryDTO queryDto);

    Result<List<DeviceVO>> getOnlineDevices();

    Result<List<DeviceVO>> getDevicesByType(Integer deviceType);

    Result<Void> updateDeviceStatus(Long deviceId, Integer status);

    Result<Void> updateHeartbeat(Long deviceId);

    Result<Void> toggleDeviceEnabled(Long deviceId, boolean enabled);
}
