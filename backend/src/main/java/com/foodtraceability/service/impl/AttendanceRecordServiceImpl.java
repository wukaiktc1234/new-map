package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.*;
import com.foodtraceability.entity.AttendanceRecord;
import com.foodtraceability.entity.Employee;
import com.foodtraceability.entity.schedule.ScheduleEntry;
import com.foodtraceability.mapper.AttendanceRecordMapper;
import com.foodtraceability.mapper.EmployeeMapper;
import com.foodtraceability.mapper.schedule.ScheduleEntryMapper;
import com.foodtraceability.mapper.schedule.SchedulePlanMapper;
import com.foodtraceability.service.AttendanceRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 考勤管理服务实现类
 */
@Service
public class AttendanceRecordServiceImpl extends ServiceImpl<AttendanceRecordMapper, AttendanceRecord>
        implements AttendanceRecordService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceRecordServiceImpl.class);

    /** 标准上班时间（9:00） */
    private static final LocalTime STANDARD_CLOCK_IN = LocalTime.of(9, 0);
    /** 标准下班时间（18:00） */
    private static final LocalTime STANDARD_CLOCK_OUT = LocalTime.of(18, 0);
    /** 迟到宽限分钟数 */
    private static final int LATE_TOLERANCE_MINUTES = 10;
    /** 早退宽限分钟数 */
    private static final int EARLY_LEAVE_TOLERANCE_MINUTES = 10;
    /** 标准工作时长（小时） */
    private static final BigDecimal STANDARD_WORK_HOURS = new BigDecimal("8.0");

    private final AttendanceRecordMapper attendanceRecordMapper;
    private final EmployeeMapper employeeMapper;
    private final SchedulePlanMapper schedulePlanMapper;
    private final ScheduleEntryMapper scheduleEntryMapper;

    public AttendanceRecordServiceImpl(AttendanceRecordMapper attendanceRecordMapper,
                                       EmployeeMapper employeeMapper,
                                       @Lazy SchedulePlanMapper schedulePlanMapper,
                                       @Lazy ScheduleEntryMapper scheduleEntryMapper) {
        this.attendanceRecordMapper = attendanceRecordMapper;
        this.employeeMapper = employeeMapper;
        this.schedulePlanMapper = schedulePlanMapper;
        this.scheduleEntryMapper = scheduleEntryMapper;
    }

    @Override
    public Map<String, Object> getAttendancePage(AttendanceRecordQueryDTO queryDTO) {
        Page<AttendanceRecord> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());
        LambdaQueryWrapper<AttendanceRecord> wrapper = new LambdaQueryWrapper<>();

        // 员工ID精确查询
        if (queryDTO.getEmployeeId() != null && !queryDTO.getEmployeeId().isEmpty()) {
            wrapper.eq(AttendanceRecord::getEmployeeId, queryDTO.getEmployeeId());
        }

        // 日期范围查询
        if (queryDTO.getStartDate() != null) {
            wrapper.ge(AttendanceRecord::getAttendanceDate, queryDTO.getStartDate());
        }
        if (queryDTO.getEndDate() != null) {
            wrapper.le(AttendanceRecord::getAttendanceDate, queryDTO.getEndDate());
        }

        // 状态筛选
        if (queryDTO.getStatus() != null) {
            wrapper.eq(AttendanceRecord::getStatus, queryDTO.getStatus());
        }

        // 按日期倒序排列
        wrapper.orderByDesc(AttendanceRecord::getAttendanceDate);

        Page<AttendanceRecord> resultPage = this.page(page, wrapper);

        // 转换为VO列表
        List<AttendanceRecordVO> voList = resultPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("records", voList);
        resultMap.put("total", resultPage.getTotal());
        resultMap.put("current", resultPage.getCurrent());
        resultMap.put("size", resultPage.getSize());
        return resultMap;
    }

    @Override
    public AttendanceRecordVO getAttendanceDetail(Long recordId) {
        AttendanceRecord record = this.getById(recordId);
        if (record == null) {
            return null;
        }
        return convertToVO(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AttendanceRecord createAttendance(AttendanceRecordCreateDTO createDTO) {
        // 检查是否已有当日记录
        int existingCount = attendanceRecordMapper.countByEmployeeAndDate(
                createDTO.getEmployeeId(), createDTO.getAttendanceDate());
        if (existingCount > 0) {
            throw new RuntimeException("该员工当日已存在考勤记录");
        }

        AttendanceRecord record = new AttendanceRecord();
        record.setEmployeeId(createDTO.getEmployeeId());
        record.setAttendanceDate(createDTO.getAttendanceDate());
        record.setClockInTime(createDTO.getClockInTime());
        record.setClockOutTime(createDTO.getClockOutTime());
        record.setOvertimeHours(createDTO.getOvertimeHours() != null ? createDTO.getOvertimeHours() : BigDecimal.ZERO);
        record.setLeaveType(createDTO.getLeaveType() != null ? createDTO.getLeaveType() : AttendanceRecord.LEAVE_TYPE_NORMAL);
        record.setLeaveHours(createDTO.getLeaveHours() != null ? createDTO.getLeaveHours() : BigDecimal.ZERO);
        record.setRemark(createDTO.getRemark());

        // 自动计算考勤状态和统计信息
        calculateAndSetStatus(record);

        this.save(record);
        log.info("创建考勤记录成功，员工ID：{}，日期：{}", createDTO.getEmployeeId(), createDTO.getAttendanceDate());
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AttendanceRecord clockIn(String employeeId) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        // 查找今日是否已有记录
        LambdaQueryWrapper<AttendanceRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AttendanceRecord::getEmployeeId, employeeId)
              .eq(AttendanceRecord::getAttendanceDate, today);
        AttendanceRecord record = this.getOne(wrapper);

        if (record == null) {
            // 创建新记录
            record = new AttendanceRecord();
            record.setEmployeeId(employeeId);
            record.setAttendanceDate(today);
            record.setClockInTime(now);
            record.setStatus(AttendanceRecord.STATUS_NORMAL);
            record.setOvertimeHours(BigDecimal.ZERO);
            record.setLeaveType(AttendanceRecord.LEAVE_TYPE_NORMAL);
            record.setLeaveHours(BigDecimal.ZERO);

            // 判断是否迟到
            if (now.isAfter(STANDARD_CLOCK_IN.plusMinutes(LATE_TOLERANCE_MINUTES))) {
                long lateMinutes = ChronoUnit.MINUTES.between(STANDARD_CLOCK_IN, now);
                record.setLateMinutes((int) lateMinutes);
                record.setStatus(AttendanceRecord.STATUS_LATE);
            } else {
                record.setLateMinutes(0);
            }

            this.save(record);
        } else if (record.getClockInTime() == null) {
            // 更新上班打卡时间
            record.setClockInTime(now);
            if (now.isAfter(STANDARD_CLOCK_IN.plusMinutes(LATE_TOLERANCE_MINUTES))) {
                long lateMinutes = ChronoUnit.MINUTES.between(STANDARD_CLOCK_IN, now);
                record.setLateMinutes((int) lateMinutes);
                if (record.getStatus() == null || record.getStatus() == AttendanceRecord.STATUS_NORMAL) {
                    record.setStatus(AttendanceRecord.STATUS_LATE);
                }
            }
            this.updateById(record);
        } else {
            throw new RuntimeException("今日已完成上班打卡，请勿重复操作");
        }

        log.info("员工上班打卡成功，员工ID：{}，时间：{}", employeeId, now);
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AttendanceRecord clockOut(String employeeId) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        // 查找今日记录
        LambdaQueryWrapper<AttendanceRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AttendanceRecord::getEmployeeId, employeeId)
              .eq(AttendanceRecord::getAttendanceDate, today);
        AttendanceRecord record = this.getOne(wrapper);

        if (record == null || record.getClockInTime() == null) {
            throw new RuntimeException("请先进行上班打卡");
        }

        if (record.getClockOutTime() != null) {
            throw new RuntimeException("今日已完成下班打卡，请勿重复操作");
        }

        // 设置下班打卡时间
        record.setClockOutTime(now);

        // 计算工作时长
        long workedMinutes = ChronoUnit.MINUTES.between(record.getClockInTime(), now);
        BigDecimal workHours = BigDecimal.valueOf(workedMinutes)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        record.setWorkHours(workHours);

        // 判断早退或加班
        if (now.isBefore(STANDARD_CLOCK_OUT.minusMinutes(EARLY_LEAVE_TOLERANCE_MINUTES))) {
            long earlyMinutes = ChronoUnit.MINUTES.between(now, STANDARD_CLOCK_OUT);
            record.setEarlyLeaveMinutes((int) earlyMinutes);
            if (record.getStatus() == null || record.getStatus() == AttendanceRecord.STATUS_NORMAL) {
                record.setStatus(AttendanceRecord.STATUS_EARLY_LEAVE);
            }
        } else if (now.isAfter(STANDARD_CLOCK_OUT)) {
            // 判断是否加班（超过标准工作时间30分钟以上算加班）
            long overtimeMinutes = ChronoUnit.MINUTES.between(STANDARD_CLOCK_OUT, now);
            if (overtimeMinutes > 30) {
                BigDecimal overtimeHours = BigDecimal.valueOf(overtimeMinutes)
                        .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
                record.setOvertimeHours(overtimeHours);
                if (record.getStatus() == null || record.getStatus() == AttendanceRecord.STATUS_NORMAL
                        || record.getStatus() == AttendanceRecord.STATUS_LATE) {
                    record.setStatus(AttendanceRecord.STATUS_OVERTIME);
                }
            }
        }

        this.updateById(record);
        log.info("员工下班打卡成功，员工ID：{}，工作时长：{}小时", employeeId, workHours);
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AttendanceRecord updateAttendance(Long recordId, AttendanceRecordUpdateDTO updateDTO) {
        AttendanceRecord record = this.getById(recordId);
        if (record == null) {
            throw new RuntimeException("考勤记录不存在");
        }

        if (updateDTO.getClockInTime() != null) {
            record.setClockInTime(updateDTO.getClockInTime());
        }
        if (updateDTO.getClockOutTime() != null) {
            record.setClockOutTime(updateDTO.getClockOutTime());
        }
        if (updateDTO.getOvertimeHours() != null) {
            record.setOvertimeHours(updateDTO.getOvertimeHours());
        }
        if (updateDTO.getLeaveType() != null) {
            record.setLeaveType(updateDTO.getLeaveType());
        }
        if (updateDTO.getLeaveHours() != null) {
            record.setLeaveHours(updateDTO.getLeaveHours());
        }
        if (updateDTO.getStatus() != null) {
            record.setStatus(updateDTO.getStatus());
        }
        if (updateDTO.getRemark() != null) {
            record.setRemark(updateDTO.getRemark());
        }

        // 重新计算状态和工作时长
        calculateAndSetStatus(record);

        this.updateById(record);
        log.info("更新考勤记录成功，记录ID：{}", recordId);
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<AttendanceRecord> applyLeave(String employeeId, Integer leaveType,
                                             BigDecimal leaveHours, LocalDate startDate,
                                             LocalDate endDate, String remark) {
        List<AttendanceRecord> records = new ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            // 检查是否已有记录
            int existingCount = attendanceRecordMapper.countByEmployeeAndDate(employeeId, currentDate);
            AttendanceRecord record;

            if (existingCount > 0) {
                // 更新现有记录为请假状态
                LambdaQueryWrapper<AttendanceRecord> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(AttendanceRecord::getEmployeeId, employeeId)
                      .eq(AttendanceRecord::getAttendanceDate, currentDate);
                record = this.getOne(wrapper);
                record.setLeaveType(leaveType);
                record.setLeaveHours(leaveHours);
                record.setStatus(AttendanceRecord.STATUS_ON_LEAVE);
                record.setRemark(remark);
                this.updateById(record);
            } else {
                // 创建新请假记录
                record = new AttendanceRecord();
                record.setEmployeeId(employeeId);
                record.setAttendanceDate(currentDate);
                record.setLeaveType(leaveType);
                record.setLeaveHours(leaveHours);
                record.setStatus(AttendanceRecord.STATUS_ON_LEAVE);
                record.setRemark(remark);
                record.setOvertimeHours(BigDecimal.ZERO);
                this.save(record);
            }

            records.add(record);
            currentDate = currentDate.plusDays(1);
        }

        log.info("员工请假申请成功，员工ID：{}，类型：{}，日期范围：{} ~ {}",
                employeeId, leaveType, startDate, endDate);
        return records;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AttendanceRecord registerOvertime(String employeeId, BigDecimal overtimeHours,
                                              LocalDate date, String remark) {
        LambdaQueryWrapper<AttendanceRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AttendanceRecord::getEmployeeId, employeeId)
              .eq(AttendanceRecord::getAttendanceDate, date);
        AttendanceRecord record = this.getOne(wrapper);

        if (record == null) {
            // 创建新记录
            record = new AttendanceRecord();
            record.setEmployeeId(employeeId);
            record.setAttendanceDate(date);
            record.setOvertimeHours(overtimeHours);
            record.setStatus(AttendanceRecord.STATUS_OVERTIME);
            record.setRemark(remark);
            record.setLeaveType(AttendanceRecord.LEAVE_TYPE_NORMAL);
            record.setLeaveHours(BigDecimal.ZERO);
            this.save(record);
        } else {
            // 更新加班时长（累加）
            BigDecimal currentOvertime = record.getOvertimeHours() != null ? record.getOvertimeHours() : BigDecimal.ZERO;
            record.setOvertimeHours(currentOvertime.add(overtimeHours));
            record.setStatus(AttendanceRecord.STATUS_OVERTIME);
            if (remark != null && !remark.isEmpty()) {
                record.setRemark(remark);
            }
            this.updateById(record);
        }

        log.info("员工加班登记成功，员工ID：{}，加班时长：{}小时", employeeId, overtimeHours);
        return record;
    }

    @Override
    public Map<String, Object> getMonthlySummary(String employeeId, String yearMonth) {
        Map<String, Object> summary = attendanceRecordMapper.selectMonthlySummary(employeeId, yearMonth);

        // 如果SQL聚合没有数据，手动构建基础结构
        if (summary == null || summary.isEmpty()) {
            summary = new HashMap<>();
            summary.put("employeeId", employeeId);
            summary.put("yearMonth", yearMonth);
            summary.put("totalDays", 0);
            summary.put("normalDays", 0);
            summary.put("lateDays", 0);
            summary.put("earlyLeaveDays", 0);
            summary.put("absentDays", 0);
            summary.put("overtimeDays", 0);
            summary.put("onLeaveDays", 0);
            summary.put("totalWorkHours", BigDecimal.ZERO.setScale(2));
            summary.put("totalOvertimeHours", BigDecimal.ZERO.setScale(2));
            summary.put("totalLateMinutes", 0);
            summary.put("totalEarlyLeaveMinutes", 0);
        }

        return summary;
    }

    @Override
    public List<AttendanceRecordVO> getDepartmentDailyAttendance(String departmentId, LocalDate date) {
        // 查询部门下所有员工
        LambdaQueryWrapper<Employee> empWrapper = new LambdaQueryWrapper<>();
        empWrapper.eq(Employee::getDepartmentId, departmentId)
                  .eq(Employee::getStatus, 1);
        List<Employee> employees = employeeMapper.selectList(empWrapper);

        if (employees == null || employees.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> employeeIds = employees.stream()
                .map(emp -> String.valueOf(emp.getId()))
                .collect(Collectors.toList());

        // 批量查询这些员工的当日考勤
        List<AttendanceRecord> records = attendanceRecordMapper.selectByEmployeesAndDate(employeeIds, date);

        // 构建员工ID->考勤记录的映射
        Map<String, AttendanceRecord> recordMap = new HashMap<>();
        for (AttendanceRecord r : records) {
            recordMap.put(r.getEmployeeId(), r);
        }

        // 构建VO列表
        List<AttendanceRecordVO> voList = new ArrayList<>();
        for (Employee emp : employees) {
            AttendanceRecordVO vo = new AttendanceRecordVO();
            vo.setEmployeeId(String.valueOf(emp.getId()));
            vo.setEmployeeName(emp.getName());
            vo.setEmployeeNo(emp.getEmployeeCode());
            vo.setDepartmentName(emp.getDepartmentName());
            vo.setPositionName(emp.getPositionName());
            vo.setAttendanceDate(date);

            AttendanceRecord record = recordMap.get(emp.getId());
            if (record != null) {
                vo.setRecordId(record.getRecordId());
                vo.setClockInTime(record.getClockInTime());
                vo.setClockOutTime(record.getClockOutTime());
                vo.setWorkHours(record.getWorkHours());
                vo.setOvertimeHours(record.getOvertimeHours());
                vo.setLeaveTypeName(getLeaveTypeName(record.getLeaveType()));
                vo.setLeaveHours(record.getLeaveHours());
                vo.setLateMinutes(record.getLateMinutes());
                vo.setEarlyLeaveMinutes(record.getEarlyLeaveMinutes());
                vo.setStatus(record.getStatus());
                vo.setStatusName(getStatusName(record.getStatus()));
                vo.setRemark(record.getRemark());
            } else {
                // 未打卡状态
                vo.setStatus(AttendanceRecord.STATUS_ABSENT);
                vo.setStatusName("缺勤/未打卡");
            }

            voList.add(vo);
        }

        return voList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int syncFromSchedule(String planId) {
        // 查询排班方案下的所有排班条目
        List<ScheduleEntry> entries = scheduleEntryMapper.selectByPlanIds(List.of(planId));
        if (entries == null || entries.isEmpty()) {
            log.warn("排班方案无排班条目，跳过同步: planId={}", planId);
            return 0;
        }

        int syncedCount = 0;
        for (ScheduleEntry entry : entries) {
            try {
                if (entry.getEmployeeId() == null || entry.getWorkDate() == null) {
                    continue;
                }
                // 组合期望打卡时间（工作日期 + 班次时间）
                LocalDateTime expectedStart = entry.getStartTime() != null
                        ? LocalDateTime.of(entry.getWorkDate(), entry.getStartTime()) : null;
                LocalDateTime expectedEnd = entry.getEndTime() != null
                        ? LocalDateTime.of(entry.getWorkDate(), entry.getEndTime()) : null;

                String employeeId = String.valueOf(entry.getEmployeeId());

                // 查找该员工当日的考勤记录
                LambdaQueryWrapper<AttendanceRecord> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(AttendanceRecord::getEmployeeId, employeeId)
                       .eq(AttendanceRecord::getAttendanceDate, entry.getWorkDate());
                AttendanceRecord existing = this.getOne(wrapper);

                if (existing == null) {
                    // 创建考勤记录（仅写入排班关联信息，打卡数据待员工打卡时填充）
                    AttendanceRecord record = new AttendanceRecord();
                    record.setEmployeeId(employeeId);
                    record.setAttendanceDate(entry.getWorkDate());
                    record.setSchedulePlanId(planId);
                    record.setScheduleEntryId(entry.getEntryId());
                    record.setShiftType(entry.getShiftType());
                    record.setExpectedStartTime(expectedStart);
                    record.setExpectedEndTime(expectedEnd);
                    record.setOvertimeHours(BigDecimal.ZERO);
                    record.setLeaveType(AttendanceRecord.LEAVE_TYPE_NORMAL);
                    record.setLeaveHours(BigDecimal.ZERO);
                    record.setStatus(AttendanceRecord.STATUS_NORMAL);
                    this.save(record);
                } else {
                    // 更新已有记录的排班关联信息
                    existing.setSchedulePlanId(planId);
                    existing.setScheduleEntryId(entry.getEntryId());
                    existing.setShiftType(entry.getShiftType());
                    existing.setExpectedStartTime(expectedStart);
                    existing.setExpectedEndTime(expectedEnd);
                    this.updateById(existing);
                }
                syncedCount++;
            } catch (Exception e) {
                log.error("同步排班条目到考勤记录失败: entryId={}, employeeId={}",
                        entry.getEntryId(), entry.getEmployeeId(), e);
            }
        }
        log.info("排班方案同步考勤记录完成: planId={}, total={}, synced={}",
                planId, entries.size(), syncedCount);
        return syncedCount;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 计算并设置考勤状态和统计数据
     */
    private void calculateAndSetStatus(AttendanceRecord record) {
        // 如果是请假状态，直接返回
        if (record.getLeaveType() != null && record.getLeaveType() != AttendanceRecord.LEAVE_TYPE_NORMAL) {
            record.setStatus(AttendanceRecord.STATUS_ON_LEAVE);
            return;
        }

        boolean hasClockIn = record.getClockInTime() != null;
        boolean hasClockOut = record.getClockOutTime() != null;

        // 计算迟到
        if (hasClockIn && record.getClockInTime().isAfter(STANDARD_CLOCK_IN.plusMinutes(LATE_TOLERANCE_MINUTES))) {
            long lateMinutes = ChronoUnit.MINUTES.between(STANDARD_CLOCK_IN, record.getClockInTime());
            record.setLateMinutes((int) lateMinutes);
        } else {
            record.setLateMinutes(0);
        }

        // 计算早退和加班、工作时长
        if (hasClockIn && hasClockOut) {
            long workedMinutes = ChronoUnit.MINUTES.between(record.getClockInTime(), record.getClockOutTime());
            record.setWorkHours(BigDecimal.valueOf(workedMinutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP));

            if (record.getClockOutTime().isBefore(STANDARD_CLOCK_OUT.minusMinutes(EARLY_LEAVE_TOLERANCE_MINUTES))) {
                long earlyMinutes = ChronoUnit.MINUTES.between(record.getClockOutTime(), STANDARD_CLOCK_OUT);
                record.setEarlyLeaveMinutes((int) earlyMinutes);
            } else {
                record.setEarlyLeaveMinutes(0);
                // 判断加班
                if (record.getClockOutTime().isAfter(STANDARD_CLOCK_OUT)) {
                    long overtimeMinutes = ChronoUnit.MINUTES.between(STANDARD_CLOCK_OUT, record.getClockOutTime());
                    if (overtimeMinutes > 30) {
                        BigDecimal ot = record.getOvertimeHours() != null ? record.getOvertimeHours() : BigDecimal.ZERO;
                        record.setOvertimeHours(ot.add(BigDecimal.valueOf(overtimeMinutes)
                                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP)));
                    }
                }
            }
        }

        // 确定最终状态
        if (!hasClockIn && !hasClockOut) {
            record.setStatus(AttendanceRecord.STATUS_ABSENT);
        } else if (record.getLateMinutes() != null && record.getLateMinutes() > 0
                && record.getEarlyLeaveMinutes() != null && record.getEarlyLeaveMinutes() > 0) {
            record.setStatus(AttendanceRecord.STATUS_LATE); // 迟到优先显示
        } else if (record.getLateMinutes() != null && record.getLateMinutes() > 0) {
            record.setStatus(AttendanceRecord.STATUS_LATE);
        } else if (record.getEarlyLeaveMinutes() != null && record.getEarlyLeaveMinutes() > 0) {
            record.setStatus(AttendanceRecord.STATUS_EARLY_LEAVE);
        } else if (record.getOvertimeHours() != null && record.getOvertimeHours().compareTo(BigDecimal.ZERO) > 0) {
            record.setStatus(AttendanceRecord.STATUS_OVERTIME);
        } else {
            record.setStatus(AttendanceRecord.STATUS_NORMAL);
        }
    }

    /**
     * 将实体转换为VO
     */
    private AttendanceRecordVO convertToVO(AttendanceRecord record) {
        AttendanceRecordVO vo = new AttendanceRecordVO();
        vo.setRecordId(record.getRecordId());
        vo.setEmployeeId(record.getEmployeeId());
        vo.setAttendanceDate(record.getAttendanceDate());
        vo.setClockInTime(record.getClockInTime());
        vo.setClockOutTime(record.getClockOutTime());
        vo.setWorkHours(record.getWorkHours());
        vo.setOvertimeHours(record.getOvertimeHours());
        vo.setLeaveTypeName(getLeaveTypeName(record.getLeaveType()));
        vo.setLeaveHours(record.getLeaveHours());
        vo.setLateMinutes(record.getLateMinutes());
        vo.setEarlyLeaveMinutes(record.getEarlyLeaveMinutes());
        vo.setStatus(record.getStatus());
        vo.setStatusName(getStatusName(record.getStatus()));
        vo.setRemark(record.getRemark());

        // 关联员工信息
        if (record.getEmployeeId() != null) {
            Employee emp = employeeMapper.selectById(record.getEmployeeId());
            if (emp != null) {
                vo.setEmployeeName(emp.getName());
                vo.setEmployeeNo(emp.getEmployeeCode());
                vo.setDepartmentName(emp.getDepartmentName());
                vo.setPositionName(emp.getPositionName());
            }
        }

        return vo;
    }

    /**
     * 获取请假类型名称
     */
    private String getLeaveTypeName(Integer leaveType) {
        if (leaveType == null) return "正常";
        switch (leaveType) {
            case AttendanceRecord.LEAVE_TYPE_NORMAL: return "正常";
            case AttendanceRecord.LEAVE_TYPE_PERSONAL: return "事假";
            case AttendanceRecord.LEAVE_TYPE_SICK: return "病假";
            case AttendanceRecord.LEAVE_TYPE_ANNUAL: return "年假";
            case AttendanceRecord.LEAVE_TYPE_COMPENSATORY: return "调休";
            case AttendanceRecord.LEAVE_TYPE_OTHER: return "其他";
            default: return "未知";
        }
    }

    /**
     * 获取考勤状态名称
     */
    private String getStatusName(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case AttendanceRecord.STATUS_NORMAL: return "正常";
            case AttendanceRecord.STATUS_LATE: return "迟到";
            case AttendanceRecord.STATUS_EARLY_LEAVE: return "早退";
            case AttendanceRecord.STATUS_ABSENT: return "缺勤";
            case AttendanceRecord.STATUS_OVERTIME: return "加班";
            case AttendanceRecord.STATUS_ON_LEAVE: return "请假";
            default: return "未知";
        }
    }
}
