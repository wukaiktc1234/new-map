package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.DeviceStatusLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 设备状态日志Mapper接口
 */
@Mapper
public interface DeviceStatusLogMapper extends BaseMapper<DeviceStatusLog> {
}
