package com.foodtraceability.mapper.schedule;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.schedule.ScheduleEntry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 排班条目Mapper接口
 */
@Mapper
public interface ScheduleEntryMapper extends BaseMapper<ScheduleEntry> {

    /**
     * 批量查询排班条目(按方案ID)
     * @param planIds 方案ID列表
     * @return 排班条目列表
     */
    List<ScheduleEntry> selectByPlanIds(@Param("planIds") List<String> planIds);

    /**
     * 查询方案在指定日期范围的条目
     * @param planId 方案ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 排班条目列表
     */
    List<ScheduleEntry> selectByPlanAndDateRange(
            String planId,
            LocalDate startDate,
            LocalDate endDate);

    /**
     * 查询员工在指定方案的排班条目
     * @param planId 方案ID
     * @param employeeIds 员工ID列表
     * @return 排班条目列表
     */
    List<ScheduleEntry> selectByPlanAndEmployees(
            String planId,
            @Param("employeeIds") List<Long> employeeIds);

    /**
     * 查询员工在指定日期的排班条目
     * @param employeeId 员工ID
     * @param workDate 工作日期
     * @return 排班条目
     */
    ScheduleEntry selectByEmployeeAndDate(
            Long employeeId,
            LocalDate workDate);

    /**
     * 统计方案的总工时(分钟)
     * @param planId 方案ID
     * @return 总工时(分钟)
     */
    Integer sumWorkHoursByPlan(String planId);

    /**
     * 统计方案涉及员工数
     * @param planId 方案ID
     * @return 员工数
     */
    Long countDistinctEmployeeByPlan(String planId);

    /**
     * 统计指定日期在岗员工数（排除休息班次 night_off）
     * @param workDate 工作日期
     * @return 在岗员工数
     */
    @Select("SELECT COUNT(DISTINCT employee_id) FROM schedule_entries " +
            "WHERE work_date = #{workDate} AND deleted = 0 " +
            "AND (shift_type IS NULL OR shift_type <> 'night_off')")
    long countOnDutyStaffByDate(@Param("workDate") LocalDate workDate);

    /**
     * 按门店统计指定日期在岗员工数
     * 通过 plan_id 关联 schedule_plans 表的 store_id
     * @param storeId 门店ID
     * @param workDate 工作日期
     * @return 在岗员工数
     */
    @Select("SELECT COUNT(DISTINCT se.employee_id) FROM schedule_entries se " +
            "INNER JOIN schedule_plans sp ON se.plan_id = sp.plan_id " +
            "WHERE se.work_date = #{workDate} AND se.deleted = 0 " +
            "AND (se.shift_type IS NULL OR se.shift_type <> 'night_off') " +
            "AND sp.store_id = #{storeId}")
    long countOnDutyStaffByStoreAndDate(@Param("storeId") Long storeId, @Param("workDate") LocalDate workDate);
}
