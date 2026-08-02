package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.TraceCodeLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 追溯码操作日志Mapper接口
 * 用于追溯码操作日志的数据访问操作
 */
@Mapper
public interface TraceCodeLogMapper extends BaseMapper<TraceCodeLog> {
}
