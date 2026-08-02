package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.Device;
import org.apache.ibatis.annotations.Mapper;

/**
 * 设备Mapper接口
 */
@Mapper
public interface DeviceMapper extends BaseMapper<Device> {
}
