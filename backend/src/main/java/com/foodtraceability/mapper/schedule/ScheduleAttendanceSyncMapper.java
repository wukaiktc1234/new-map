package com.foodtraceability.mapper.schedule;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.schedule.ScheduleAttendanceSync;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * 排班考勤同步Mapper接口
 */
@Repository
public interface ScheduleAttendanceSyncMapper extends BaseMapper<ScheduleAttendanceSync> {

    /**
     * 按方案ID查询最新同步记录
     *
     * @param planId 方案ID
     * @return 最新同步记录
     */
    ScheduleAttendanceSync selectLatestByPlanId(@Param("planId") String planId);
}
