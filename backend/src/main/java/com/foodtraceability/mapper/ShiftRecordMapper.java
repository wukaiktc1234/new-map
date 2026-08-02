package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 交接班记录Mapper接口
 */
@Mapper
public interface ShiftRecordMapper extends BaseMapper<com.foodtraceability.entity.ShiftRecord> {
}
