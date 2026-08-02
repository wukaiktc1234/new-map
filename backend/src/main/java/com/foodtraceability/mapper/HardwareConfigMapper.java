package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.HardwareConfig;
import org.apache.ibatis.annotations.Param;

public interface HardwareConfigMapper extends BaseMapper<HardwareConfig> {
    
    HardwareConfig selectConfig(@Param("storeId") String storeId, @Param("deviceType") String deviceType);
}