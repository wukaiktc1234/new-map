package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.DeviceDriverVersion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 设备驱动版本Mapper接口
 * 负责设备驱动版本的数据库操作
 */
@Mapper
public interface DeviceDriverVersionMapper extends BaseMapper<DeviceDriverVersion> {
    
    // 可以添加自定义的SQL查询方法
}
