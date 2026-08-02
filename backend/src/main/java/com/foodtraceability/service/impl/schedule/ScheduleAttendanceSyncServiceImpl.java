package com.foodtraceability.service.impl.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.dto.schedule.AttendanceDiffDetailVO;
import com.foodtraceability.dto.schedule.AttendanceDiffReportVO;
import com.foodtraceability.dto.schedule.AttendanceSyncVO;
import com.foodtraceability.entity.schedule.ScheduleAttendanceSync;
import com.foodtraceability.entity.schedule.ScheduleEntry;
import com.foodtraceability.entity.schedule.SchedulePlan;
import com.foodtraceability.mapper.schedule.ScheduleAttendanceSyncMapper;
import com.foodtraceability.mapper.schedule.ScheduleEntryMapper;
import com.foodtraceability.mapper.schedule.SchedulePlanMapper;
import com.foodtraceability.service.AttendanceRecordService;
import com.foodtraceability.service.schedule.ScheduleAttendanceSyncService;
import com.foodtraceability.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 排班考勤同步服务实现类
 * 实现排班数据同步到考勤系统、查询同步状态、生成差异报告等功能
 */
@Service
public class ScheduleAttendanceSyncServiceImpl implements ScheduleAttendanceSyncService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleAttendanceSyncServiceImpl.class);

    /** 日期格式化器 */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /** 日期时间格式化器 */
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ScheduleAttendanceSyncMapper attendanceSyncMapper;
    private final SchedulePlanMapper schedulePlanMapper;
    private final ScheduleEntryMapper scheduleEntryMapper;
    private final AttendanceRecordService attendanceRecordService;

    public ScheduleAttendanceSyncServiceImpl(ScheduleAttendanceSyncMapper attendanceSyncMapper,
                                              SchedulePlanMapper schedulePlanMapper,
                                              ScheduleEntryMapper scheduleEntryMapper,
                                              @Lazy AttendanceRecordService attendanceRecordService) {
        this.attendanceSyncMapper = attendanceSyncMapper;
        this.schedulePlanMapper = schedulePlanMapper;
        this.scheduleEntryMapper = scheduleEntryMapper;
        this.attendanceRecordService = attendanceRecordService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AttendanceSyncVO syncToAttendance(String planId) {
        // 校验方案存在
        SchedulePlan plan = schedulePlanMapper.selectById(planId);
        if (plan == null) {
            throw new RuntimeException("排班方案不存在：" + planId);
        }

        // 查询方案下的所有排班条目
        List<ScheduleEntry> entries = scheduleEntryMapper.selectByPlanIds(List.of(planId));
        int totalEntries = entries.size();

        // 创建同步记录
        ScheduleAttendanceSync syncEntity = new ScheduleAttendanceSync();
        syncEntity.setPlanId(planId);
        syncEntity.setSyncTime(LocalDateTime.now());
        syncEntity.setTotalEntries(totalEntries);

        // 调用考勤服务将排班数据同步到考勤记录
        int syncedCount = 0;
        String errorMessage = null;
        try {
            syncedCount = attendanceRecordService.syncFromSchedule(planId);
        } catch (Exception e) {
            errorMessage = e.getMessage();
            log.error("调用考勤服务同步失败: planId={}", planId, e);
        }
        int failedCount = totalEntries - syncedCount;
        syncEntity.setSyncedEntries(syncedCount);
        syncEntity.setFailedEntries(failedCount);
        // 同步状态：全部成功或部分成功为已同步，全部失败为失败
        if (totalEntries == 0 || syncedCount > 0) {
            syncEntity.setSyncStatus(ScheduleAttendanceSync.STATUS_SYNCED);
        } else {
            syncEntity.setSyncStatus(ScheduleAttendanceSync.STATUS_FAILED);
        }
        syncEntity.setErrorMessage(errorMessage);

        // 设置操作人信息
        Long currentUserId = SecurityUtils.getCurrentUserId();
        syncEntity.setOperatorId(currentUserId != null ? String.valueOf(currentUserId) : null);
        syncEntity.setOperatorName(SecurityUtils.getCurrentUsername());
        syncEntity.setCreateTime(LocalDateTime.now());
        syncEntity.setUpdateTime(LocalDateTime.now());
        syncEntity.setDeleted(0);

        attendanceSyncMapper.insert(syncEntity);

        // 更新方案的同步状态
        if (ScheduleAttendanceSync.STATUS_FAILED.equals(syncEntity.getSyncStatus())) {
            plan.setSyncStatus(SchedulePlan.SYNC_STATUS_FAILED);
        } else {
            plan.setSyncStatus(SchedulePlan.SYNC_STATUS_SYNCED);
        }
        plan.setSyncToAttendanceTime(LocalDateTime.now());
        plan.setSyncMessage("同步完成：成功 " + syncedCount + " 条，失败 " + failedCount + " 条");
        schedulePlanMapper.updateById(plan);

        log.info("同步排班到考勤系统完成: planId={}, total={}, synced={}, failed={}",
                planId, totalEntries, syncedCount, failedCount);

        return convertToVO(syncEntity);
    }

    @Override
    public AttendanceSyncVO getSyncStatus(String planId) {
        // 查询最新同步记录
        ScheduleAttendanceSync latest = attendanceSyncMapper.selectLatestByPlanId(planId);
        if (latest != null) {
            return convertToVO(latest);
        }

        // 无同步记录，返回默认未同步状态
        AttendanceSyncVO vo = new AttendanceSyncVO();
        vo.setPlanId(planId);
        vo.setSyncStatus(ScheduleAttendanceSync.STATUS_NOT_SYNCED);
        vo.setTotalEntries(0);
        vo.setSyncedEntries(0);
        vo.setFailedEntries(0);
        return vo;
    }

    @Override
    public AttendanceDiffReportVO getDiffReport(String planId, String startDate, String endDate) {
        // 校验方案存在
        SchedulePlan plan = schedulePlanMapper.selectById(planId);
        if (plan == null) {
            throw new RuntimeException("排班方案不存在：" + planId);
        }

        // 解析日期范围（默认使用方案周期）
        LocalDate start = startDate != null ? LocalDate.parse(startDate, DATE_FORMATTER) : plan.getStartDate();
        LocalDate end = endDate != null ? LocalDate.parse(endDate, DATE_FORMATTER) : plan.getEndDate();

        // 查询排班条目
        List<ScheduleEntry> entries = scheduleEntryMapper.selectByPlanAndDateRange(planId, start, end);

        // 构建差异报告
        AttendanceDiffReportVO report = new AttendanceDiffReportVO();
        report.setReportId(UUID.randomUUID().toString());
        report.setPlanId(planId);
        report.setPeriodStart(start.format(DATE_FORMATTER));
        report.setPeriodEnd(end.format(DATE_FORMATTER));
        report.setGenerateTime(LocalDateTime.now().format(DATETIME_FORMATTER));

        // 构建汇总
        AttendanceDiffReportVO.Summary summary = new AttendanceDiffReportVO.Summary();
        summary.setTotalEntries(entries.size());
        // TODO: 实现与考勤系统的实际对接，对比排班与打卡数据
        // 当前为框架实现，所有差异统计均为0
        summary.setCheckedIn(0);
        summary.setNotCheckedIn(0);
        summary.setAbnormal(0);
        summary.setLateCount(0);
        summary.setEarlyLeaveCount(0);
        summary.setMissingCount(0);
        report.setSummary(summary);

        // 差异明细列表（当前为空）
        report.setDetails(new ArrayList<>());

        log.info("生成考勤差异报告: planId={}, period={} ~ {}, entries={}",
                planId, start, end, entries.size());

        return report;
    }

    @Override
    public List<AttendanceSyncVO> getSyncHistory(String planId) {
        // 查询方案的所有同步记录（按时间倒序）
        LambdaQueryWrapper<ScheduleAttendanceSync> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScheduleAttendanceSync::getPlanId, planId)
               .orderByDesc(ScheduleAttendanceSync::getSyncTime);

        List<ScheduleAttendanceSync> list = attendanceSyncMapper.selectList(wrapper);
        return list.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    // ==================== 私有辅助方法 ====================

    /**
     * Entity → VO 转换
     * @param entity 同步记录实体
     * @return 视图对象
     */
    private AttendanceSyncVO convertToVO(ScheduleAttendanceSync entity) {
        if (entity == null) {
            return null;
        }

        AttendanceSyncVO vo = new AttendanceSyncVO();
        vo.setSyncId(entity.getSyncId());
        vo.setPlanId(entity.getPlanId());
        vo.setSyncStatus(entity.getSyncStatus());
        if (entity.getSyncTime() != null) {
            vo.setSyncTime(entity.getSyncTime().format(DATETIME_FORMATTER));
        }
        vo.setTotalEntries(entity.getTotalEntries());
        vo.setSyncedEntries(entity.getSyncedEntries());
        vo.setFailedEntries(entity.getFailedEntries());
        vo.setErrorMessage(entity.getErrorMessage());
        vo.setOperatorId(entity.getOperatorId());
        vo.setOperatorName(entity.getOperatorName());
        if (entity.getCreateTime() != null) {
            vo.setCreateTime(entity.getCreateTime().format(DATETIME_FORMATTER));
        }

        return vo;
    }
}
