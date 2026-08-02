package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.DeviceData;
import org.apache.ibatis.annotations.Mapper;

/**
 * 设备数据Mapper接口
 * 负责设备数据的数据库操作
 */
@Mapper
public interface DeviceDataMapper extends BaseMapper<DeviceData> {
    
    // 可以添加自定义的SQL查询方法
}
