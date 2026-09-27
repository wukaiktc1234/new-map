package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.LocationIdMap;
import org.apache.ibatis.annotations.Mapper;

/**
 * 旧 ID → location_id 映射桥 Mapper。
 */
@Mapper
public interface LocationIdMapMapper extends BaseMapper<LocationIdMap> {
}
