package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.HardwareOperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 硬件设备操作日志Mapper接口
 */
@Mapper
public interface HardwareOperationLogMapper extends BaseMapper<HardwareOperationLog> {
}