package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.DeviceAlert;
import org.apache.ibatis.annotations.Mapper;

/**
 * 设备告警Mapper接口
 */
@Mapper
public interface DeviceAlertMapper extends BaseMapper<DeviceAlert> {
}
