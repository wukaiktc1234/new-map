package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.DeviceStatusHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 设备状态历史记录Mapper接口
 */
@Mapper
public interface DeviceStatusHistoryMapper extends BaseMapper<DeviceStatusHistory> {
    
    /**
     * 按设备类型查询设备状态历史记录
     * @param deviceType 设备类型
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param storeId 门店ID
     * @param pageSize 每页大小
     * @param pageNum 页码
     * @return 历史记录列表
     */
    List<DeviceStatusHistory> selectByDeviceType(
            @Param("deviceType") String deviceType,
            @Param("startTime") Date startTime,
            @Param("endTime") Date endTime,
            @Param("storeId") Long storeId,
            @Param("pageSize") Integer pageSize,
            @Param("pageNum") Integer pageNum);
    
    /**
     * 查询指定设备的状态历史记录
     * @param deviceId 设备ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageSize 每页大小
     * @param pageNum 页码
     * @return 历史记录列表
     */
    List<DeviceStatusHistory> selectByDeviceId(
            @Param("deviceId") Long deviceId,
            @Param("startTime") Date startTime,
            @Param("endTime") Date endTime,
            @Param("pageSize") Integer pageSize,
            @Param("pageNum") Integer pageNum);
    
    /**
     * 删除指定时间之前的历史记录
     * @param beforeTime 指定时间
     * @return 删除结果
     */
    int deleteBeforeTime(@Param("beforeTime") Date beforeTime);
    
    /**
     * 统计设备状态变化次数
     * @param deviceType 设备类型
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param storeId 门店ID
     * @return 变化次数
     */
    int countStatusChanges(
            @Param("deviceType") String deviceType,
            @Param("startTime") Date startTime,
            @Param("endTime") Date endTime,
            @Param("storeId") Long storeId);
}
