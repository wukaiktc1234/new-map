package com.foodtraceability.mapper.schedule;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.schedule.ScheduleShiftType;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 班次类型配置Mapper接口
 */
@Mapper
public interface ScheduleShiftTypeMapper extends BaseMapper<ScheduleShiftType> {

    /**
     * 查询门店的所有班次类型(含停用)
     * @param storeId 门店ID
     * @return 班次类型列表
     */
    List<ScheduleShiftType> selectAllByStoreId(Long storeId);

    /**
     * 查询门店的启用班次类型
     * @param storeId 门店ID
     * @return 启用的班次类型列表
     */
    List<ScheduleShiftType> selectActiveByStoreId(Long storeId);

    /**
     * 按排序顺序查询门店班次类型
     * @param storeId 门店ID
     * @return 按sort_order排序的班次类型列表
     */
    List<ScheduleShiftType> selectByStoreIdOrderBySort(Long storeId);

    /**
     * 根据班次编码查询
     * @param storeId 门店ID
     * @param shiftCode 班次编码
     * @return 班次类型
     */
    ScheduleShiftType selectByStoreAndCode(
            Long storeId,
            String shiftCode);
}
