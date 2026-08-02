package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.HardwareConfigVersion;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 设备配置版本Mapper接口
 * 用于处理设备配置版本的数据库操作
 */
public interface HardwareConfigVersionMapper extends BaseMapper<HardwareConfigVersion> {
    
    /**
     * 根据硬件配置ID获取版本列表
     * @param hardwareId 硬件配置ID
     * @return 版本列表
     */
    List<HardwareConfigVersion> selectVersionsByHardwareId(@Param("hardwareId") Long hardwareId);
    
    /**
     * 获取指定硬件配置的最新版本号
     * @param hardwareId 硬件配置ID
     * @return 最新版本号，如果没有版本则返回0
     */
    Integer selectMaxVersionByHardwareId(@Param("hardwareId") Long hardwareId);
    
    /**
     * 根据ID获取版本详情
     * @param id 版本ID
     * @return 版本详情
     */
    HardwareConfigVersion selectVersionById(@Param("id") Long id);
}