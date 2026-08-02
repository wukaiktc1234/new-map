package com.foodtraceability.service.schedule;

import com.foodtraceability.dto.schedule.ConflictQueryDTO;
import com.foodtraceability.dto.schedule.ScheduleExportDTO;

/**
 * 排班数据导出服务接口
 * 提供排班数据、冲突、换班、考勤差异的导出功能
 *
 * <p>支持的格式：excel、csv
 * <p>注意：当前为框架实现，具体的 Excel/CSV 生成逻辑后续完善
 */
public interface ScheduleExportService {

    /**
     * 导出排班数据
     * @param exportDTO 导出参数（方案ID、格式、范围、日期区间）
     * @return 文件字节数组
     */
    byte[] exportSchedule(ScheduleExportDTO exportDTO);

    /**
     * 导出冲突数据
     * @param planId 方案ID
     * @param format 导出格式（excel/csv）
     * @return 文件字节数组
     */
    byte[] exportConflicts(String planId, String format);

    /**
     * 导出换班请求数据
     * @param queryDTO 查询条件（含方案ID、状态筛选）
     * @param format 导出格式（excel/csv）
     * @return 文件字节数组
     */
    byte[] exportSwapRequests(ConflictQueryDTO queryDTO, String format);

    /**
     * 导出考勤差异报告
     * @param planId 方案ID
     * @param format 导出格式（excel/csv）
     * @return 文件字节数组
     */
    byte[] exportAttendanceDiff(String planId, String format);
}
