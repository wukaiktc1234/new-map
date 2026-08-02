package com.foodtraceability.mapper.schedule;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.schedule.ScheduleConflict;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 排班冲突Mapper接口
 */
@Repository
public interface ScheduleConflictMapper extends BaseMapper<ScheduleConflict> {

    /**
     * 按方案ID查询冲突列表
     *
     * @param planId 方案ID
     * @param level 冲突级别筛选（可选）
     * @return 冲突列表
     */
    List<ScheduleConflict> selectByPlanId(@Param("planId") String planId,
                                           @Param("level") String level);

    /**
     * 删除方案的所有冲突记录（物理删除，用于重新检查前清理）
     *
     * @param planId 方案ID
     * @return 删除的记录数
     */
    Integer deleteByPlanId(@Param("planId") String planId);
}
