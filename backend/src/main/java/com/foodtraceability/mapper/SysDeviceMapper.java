package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.SysDevice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 系统设备Mapper接口
 */
@Mapper
public interface SysDeviceMapper extends BaseMapper<SysDevice> {

    /**
     * 根据门店ID查询设备
     */
    @Select("SELECT * FROM sys_devices WHERE store_id = #{storeId} ORDER BY create_time DESC")
    List<SysDevice> selectByStoreId(@Param("storeId") Long storeId);

    /**
     * 根据设备类型查询
     */
    @Select("SELECT * FROM sys_devices WHERE device_type = #{deviceType} AND status = 'online'")
    List<SysDevice> selectByType(@Param("deviceType") String deviceType);

    /**
     * 根据门店和设备类型查询
     */
    @Select("SELECT * FROM sys_devices WHERE store_id = #{storeId} AND device_type = #{deviceType}")
    List<SysDevice> selectByStoreAndType(@Param("storeId") Long storeId, @Param("deviceType") String deviceType);
}
