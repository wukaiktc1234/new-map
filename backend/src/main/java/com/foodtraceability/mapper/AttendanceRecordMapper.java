package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.AttendanceRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 考勤记录Mapper接口
 */
@Mapper
public interface AttendanceRecordMapper extends BaseMapper<AttendanceRecord> {

    /**
     * 查询员工在指定日期范围内的考勤记录
     * @param employeeId 员工ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 考勤记录列表
     */
    List<AttendanceRecord> selectByEmployeeAndDateRange(
            @Param("employeeId") String employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 统计员工月度考勤汇总
     * @param employeeId 员工ID
     * @param yearMonth 年月（格式：2026-04）
     * @return 考勤统计数据
     */
    Map<String, Object> selectMonthlySummary(
            @Param("employeeId") String employeeId,
            @Param("yearMonth") String yearMonth);

    /**
     * 批量查询员工某日的考勤记录
     * @param employeeIds 员工ID列表
     * @param date 日期
     * @return 考勤记录列表
     */
    List<AttendanceRecord> selectByEmployeesAndDate(
            @Param("employeeIds") List<String> employeeIds,
            @Param("date") LocalDate date);

    /**
     * 检查员工某日是否已有考勤记录
     * @param employeeId 员工ID
     * @param date 日期
     * @return 记录数
     */
    int countByEmployeeAndDate(
            @Param("employeeId") String employeeId,
            @Param("date") LocalDate date);
}
