package com.foodtraceability.dataservice.schedule;

import com.foodtraceability.dto.schedule.ScheduleShiftTypeVO;

import java.util.List;
import java.util.Map;

/**
 * 班次类型数据服务接口
 * 提供班次类型数据的缓存和批量查询功能
 */
public interface ScheduleShiftTypeDataService {

    /**
     * 批量获取班次类型基本信息
     * @param shiftTypeIds 班次类型ID列表
     * @return 班次类型ID到基本信息的映射
     */
    Map<String, ScheduleShiftTypeVO> batchGetShiftTypeBasicInfo(List<Long> shiftTypeIds);

    /**
     * 获取单个班次类型基本信息
     * @param shiftTypeId 班次类型ID
     * @return 班次类型基本信息
     */
    ScheduleShiftTypeVO getShiftTypeBasicInfo(Long shiftTypeId);

    /**
     * 获取门店所有启用的班次类型(从缓存或数据库)
     * @param storeId 门店ID
     * @return 启用的班次类型列表(按排序)
     */
    List<ScheduleShiftTypeVO> getActiveShiftTypesByStore(Long storeId);

    /**
     * 获取门店所有班次类型含停用(从缓存或数据库)
     * @param storeId 门店ID
     * @return 所有班次类型列表(按排序)
     */
    List<ScheduleShiftTypeVO> getAllShiftTypesByStore(Long storeId);

    /**
     * 清除指定班次类型的缓存
     * @param shiftTypeId 班次类型ID
     */
    void clearShiftTypeCache(String shiftTypeId);

    /**
     * 批量清除班次类型缓存
     * @param shiftTypeIds 班次类型ID列表
     */
    void clearShiftTypeBatchCache(List<Long> shiftTypeIds);

    /**
     * 清除门店下所有班次类型的缓存
     * @param storeId 门店ID
     */
    void clearStoreShiftTypeCache(Long storeId);
}
