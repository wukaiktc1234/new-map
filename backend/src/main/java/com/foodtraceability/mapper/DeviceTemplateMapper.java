package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.DeviceTemplate;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DeviceTemplateMapper extends BaseMapper<DeviceTemplate> {
    
    List<DeviceTemplate> selectByDeviceId(@Param("deviceId") Long deviceId);
    
    List<DeviceTemplate> selectByDeviceIdAndType(@Param("deviceId") Long deviceId, @Param("templateType") String templateType);
    
    DeviceTemplate selectDefaultTemplate(@Param("deviceId") Long deviceId, @Param("templateType") String templateType);
    
    int updateDefaultTemplate(@Param("deviceId") Long deviceId, @Param("templateType") String templateType, @Param("id") Long id);
}