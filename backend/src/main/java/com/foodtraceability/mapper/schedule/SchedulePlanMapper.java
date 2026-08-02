package com.foodtraceability.mapper.schedule;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.schedule.SchedulePlan;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

/**
 * 排班方案Mapper接口
 */
@Mapper
public interface SchedulePlanMapper extends BaseMapper<SchedulePlan> {

    /**
     * 查询门店在指定日期范围内的排班方案
     * @param storeId 门店ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 排班方案列表
     */
    List<SchedulePlan> selectByStoreAndDateRange(
            Long storeId,
            LocalDate startDate,
            LocalDate endDate);

    /**
     * 查询门店的排班方案(按状态筛选)
     * @param storeId 门店ID
     * @param status 状态(可选)
     * @return 排班方案列表
     */
    List<SchedulePlan> selectByStoreAndStatus(
            Long storeId,
            String status);

    /**
     * 统计门店各状态的方案数量
     * @param storeId 门店ID
     * @return 统计数据 Map<status, count>
     */
    List<SchedulePlan> selectStatusCountByStore(Long storeId);
}
