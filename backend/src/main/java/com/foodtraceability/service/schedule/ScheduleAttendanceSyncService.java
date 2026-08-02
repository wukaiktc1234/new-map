package com.foodtraceability.service.schedule;

import com.foodtraceability.dto.schedule.AttendanceDiffReportVO;
import com.foodtraceability.dto.schedule.AttendanceSyncVO;

import java.util.List;

/**
 * 排班考勤同步服务接口
 * 提供排班数据同步到考勤系统、查询同步状态、生成差异报告等功能
 *
 * <p>同步状态：
 * <ul>
 *   <li>not_synced: 未同步</li>
 *   <li>synced: 已同步</li>
 *   <li>failed: 同步失败</li>
 * </ul>
 */
public interface ScheduleAttendanceSyncService {

    /**
     * 同步排班数据到考勤系统
     * 业务逻辑：将方案下所有排班条目同步到考勤系统，并记录同步结果
     * @param planId 方案ID
     * @return 同步结果
     */
    AttendanceSyncVO syncToAttendance(String planId);

    /**
     * 获取方案的同步状态
     * @param planId 方案ID
     * @return 同步状态信息（不存在返回默认未同步状态）
     */
    AttendanceSyncVO getSyncStatus(String planId);

    /**
     * 获取考勤差异报告
     * 业务逻辑：对比排班数据与实际考勤打卡数据，识别迟到/早退/缺卡等差异
     * @param planId 方案ID
     * @param startDate 统计周期开始日期（可选，YYYY-MM-DD）
     * @param endDate 统计周期结束日期（可选，YYYY-MM-DD）
     * @return 差异报告（含汇总和明细列表）
     */
    AttendanceDiffReportVO getDiffReport(String planId, String startDate, String endDate);

    /**
     * 获取方案的同步历史
     * @param planId 方案ID
     * @return 同步记录列表（按时间倒序）
     */
    List<AttendanceSyncVO> getSyncHistory(String planId);
}
