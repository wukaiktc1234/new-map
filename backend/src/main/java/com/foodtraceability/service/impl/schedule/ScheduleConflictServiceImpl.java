package com.foodtraceability.service.impl.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.dto.schedule.ConflictCheckResultVO;
import com.foodtraceability.dto.schedule.ConflictDetailVO;
import com.foodtraceability.dto.schedule.ConflictQueryDTO;
import com.foodtraceability.entity.schedule.ScheduleConflict;
import com.foodtraceability.entity.schedule.ScheduleEntry;
import com.foodtraceability.mapper.schedule.ScheduleConflictMapper;
import com.foodtraceability.mapper.schedule.ScheduleEntryMapper;
import com.foodtraceability.service.schedule.ScheduleConflictService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 排班冲突检测服务实现类
 * 实现冲突检查、查询、自动修复、忽略等业务逻辑
 *
 * <p>冲突检测规则（基础版）：
 * <ul>
 *   <li>重复班次：同一员工同一天有多个非休息班次</li>
 *   <li>连续工作超限：连续工作超过6天</li>
 *   <li>TODO: 完善的冲突检测规则需后续迭代（休息违规、班次间隔不足等）</li>
 * </ul>
 */
@Service
public class ScheduleConflictServiceImpl implements ScheduleConflictService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleConflictServiceImpl.class);

    /** 日期格式化器 */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /** 日期时间格式化器 */
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 最大连续工作天数（超过此值视为冲突） */
    private static final int MAX_CONSECUTIVE_DAYS = 6;

    private final ScheduleConflictMapper scheduleConflictMapper;
    private final ScheduleEntryMapper scheduleEntryMapper;

    public ScheduleConflictServiceImpl(ScheduleConflictMapper scheduleConflictMapper,
                                       ScheduleEntryMapper scheduleEntryMapper) {
        this.scheduleConflictMapper = scheduleConflictMapper;
        this.scheduleEntryMapper = scheduleEntryMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConflictCheckResultVO checkConflicts(String planId) {
        // 清理该方案旧的冲突记录（物理删除，便于重新检查）
        scheduleConflictMapper.deleteByPlanId(planId);

        // 查询方案下所有排班条目
        LambdaQueryWrapper<ScheduleEntry> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScheduleEntry::getPlanId, planId)
               .orderByAsc(ScheduleEntry::getEmployeeId)
               .orderByAsc(ScheduleEntry::getWorkDate);
        List<ScheduleEntry> entries = scheduleEntryMapper.selectList(wrapper);

        // 执行冲突检测
        List<ScheduleConflict> conflicts = new ArrayList<>();
        conflicts.addAll(detectDuplicateShifts(planId, entries));
        conflicts.addAll(detectConsecutiveDays(planId, entries));

        // 批量插入冲突记录
        for (ScheduleConflict conflict : conflicts) {
            scheduleConflictMapper.insert(conflict);
        }

        log.info("冲突检查完成: planId={}, 发现冲突数={}", planId, conflicts.size());

        // 构建返回结果
        return buildCheckResult(planId, conflicts);
    }

    @Override
    public List<ConflictDetailVO> getConflictList(ConflictQueryDTO queryDTO) {
        List<ScheduleConflict> list = scheduleConflictMapper.selectByPlanId(
                queryDTO.getPlanId(), queryDTO.getLevel());
        return list.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public ConflictDetailVO getConflictById(String id) {
        ScheduleConflict entity = scheduleConflictMapper.selectById(id);
        if (entity == null) {
            return null;
        }
        return convertToVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConflictDetailVO autoFixConflict(String id) {
        ScheduleConflict entity = scheduleConflictMapper.selectById(id);
        if (entity == null) {
            throw new RuntimeException("冲突记录不存在：" + id);
        }

        // 校验：仅 autoFixAvailable=true 且 fix_status=pending 可修复
        if (!Boolean.TRUE.equals(entity.getAutoFixAvailable())) {
            throw new RuntimeException("该冲突不支持自动修复");
        }
        if (!ScheduleConflict.FIX_PENDING.equals(entity.getFixStatus())) {
            throw new RuntimeException("当前修复状态不允许操作：" + entity.getFixStatus());
        }

        // TODO: 实现具体的自动修复逻辑（如调整班次、重新分配员工等）
        entity.setFixStatus(ScheduleConflict.FIX_FIXED);
        entity.setFixedTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());

        scheduleConflictMapper.updateById(entity);
        log.info("自动修复冲突: conflictId={}", id);

        return convertToVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConflictDetailVO ignoreConflict(String id) {
        ScheduleConflict entity = scheduleConflictMapper.selectById(id);
        if (entity == null) {
            throw new RuntimeException("冲突记录不存在：" + id);
        }

        if (!ScheduleConflict.FIX_PENDING.equals(entity.getFixStatus())) {
            throw new RuntimeException("当前修复状态不允许忽略：" + entity.getFixStatus());
        }

        entity.setFixStatus(ScheduleConflict.FIX_IGNORED);
        entity.setFixedTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());

        scheduleConflictMapper.updateById(entity);
        log.info("忽略冲突: conflictId={}", id);

        return convertToVO(entity);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 检测重复班次冲突
     * 规则：同一员工同一天有多个非休息班次视为冲突
     * @param planId 方案ID
     * @param entries 排班条目列表
     * @return 冲突记录列表
     */
    private List<ScheduleConflict> detectDuplicateShifts(String planId, List<ScheduleEntry> entries) {
        List<ScheduleConflict> conflicts = new ArrayList<>();

        // 按员工+日期分组
        Map<String, List<ScheduleEntry>> groupMap = entries.stream()
                .filter(e -> e.getEmployeeId() != null && e.getWorkDate() != null)
                .collect(Collectors.groupingBy(
                        e -> e.getEmployeeId() + "_" + e.getWorkDate().format(DATE_FORMATTER)));

        for (Map.Entry<String, List<ScheduleEntry>> entry : groupMap.entrySet()) {
            List<ScheduleEntry> sameDayEntries = entry.getValue();
            // 同一天超过1条非休息班次视为冲突
            if (sameDayEntries.size() > 1) {
                ScheduleEntry first = sameDayEntries.get(0);
                ScheduleConflict conflict = buildConflict(
                        planId, ScheduleConflict.LEVEL_ERROR, "duplicate_shift",
                        first.getEntryId(), String.valueOf(first.getEmployeeId()),
                        first.getEmployeeName(), first.getWorkDate(),
                        first.getShiftType(),
                        "员工同一天存在多个排班班次",
                        "请保留一个班次，删除其他重复班次");
                conflicts.add(conflict);
            }
        }

        return conflicts;
    }

    /**
     * 检测连续工作天数超限冲突
     * 规则：连续工作超过 MAX_CONSECUTIVE_DAYS 天视为冲突
     * @param planId 方案ID
     * @param entries 排班条目列表
     * @return 冲突记录列表
     */
    private List<ScheduleConflict> detectConsecutiveDays(String planId, List<ScheduleEntry> entries) {
        List<ScheduleConflict> conflicts = new ArrayList<>();

        // 按员工分组
        Map<Long, List<ScheduleEntry>> byEmployee = entries.stream()
                .filter(e -> e.getEmployeeId() != null && e.getWorkDate() != null)
                .collect(Collectors.groupingBy(ScheduleEntry::getEmployeeId));

        for (Map.Entry<Long, List<ScheduleEntry>> empEntry : byEmployee.entrySet()) {
            List<ScheduleEntry> empList = empEntry.getValue().stream()
                    .sorted((a, b) -> a.getWorkDate().compareTo(b.getWorkDate()))
                    .collect(Collectors.toList());

            int consecutive = 1;
            for (int i = 1; i < empList.size(); i++) {
                ScheduleEntry prev = empList.get(i - 1);
                ScheduleEntry curr = empList.get(i);

                // 判断是否为连续日期且非休息班次
                if (prev.getWorkDate().plusDays(1).equals(curr.getWorkDate())
                        && !isRestShift(prev) && !isRestShift(curr)) {
                    consecutive++;
                } else {
                    consecutive = 1;
                }

                // 超过限制天数视为冲突
                if (consecutive > MAX_CONSECUTIVE_DAYS) {
                    ScheduleConflict conflict = buildConflict(
                            planId, ScheduleConflict.LEVEL_WARNING, "consecutive_days",
                            curr.getEntryId(), String.valueOf(curr.getEmployeeId()),
                            curr.getEmployeeName(), curr.getWorkDate(),
                            curr.getShiftType(),
                            "员工连续工作超过" + MAX_CONSECUTIVE_DAYS + "天",
                            "请安排休息日，避免员工过度疲劳");
                    conflicts.add(conflict);
                }
            }
        }

        return conflicts;
    }

    /**
     * 判断是否为休息班次
     * @param entry 排班条目
     * @return true表示休息班次
     */
    private boolean isRestShift(ScheduleEntry entry) {
        return "night_off".equals(entry.getShiftType());
    }

    /**
     * 构建冲突实体
     */
    private ScheduleConflict buildConflict(String planId, String level, String type,
                                            String entryId, String employeeId, String employeeName,
                                            java.time.LocalDate conflictDate, String shiftType,
                                            String message, String suggestion) {
        ScheduleConflict conflict = new ScheduleConflict();
        conflict.setPlanId(planId);
        conflict.setLevel(level);
        conflict.setType(type);
        conflict.setEntryId(entryId);
        conflict.setEmployeeId(employeeId);
        conflict.setEmployeeName(employeeName);
        conflict.setConflictDate(conflictDate);
        conflict.setShiftType(shiftType);
        conflict.setMessage(message);
        conflict.setSuggestion(suggestion);
        conflict.setAutoFixAvailable(ScheduleConflict.LEVEL_WARNING.equals(level));
        conflict.setFixStatus(ScheduleConflict.FIX_PENDING);
        conflict.setCreateTime(LocalDateTime.now());
        conflict.setUpdateTime(LocalDateTime.now());
        conflict.setDeleted(0);
        return conflict;
    }

    /**
     * 构建冲突检查结果
     * @param planId 方案ID
     * @param conflicts 冲突列表
     * @return 检查结果VO
     */
    private ConflictCheckResultVO buildCheckResult(String planId, List<ScheduleConflict> conflicts) {
        ConflictCheckResultVO result = new ConflictCheckResultVO();
        result.setPlanId(planId);
        result.setCheckTime(LocalDateTime.now().format(DATETIME_FORMATTER));

        // 按级别统计
        int errorCount = 0;
        int warningCount = 0;
        int infoCount = 0;
        for (ScheduleConflict conflict : conflicts) {
            switch (conflict.getLevel()) {
                case ScheduleConflict.LEVEL_ERROR:
                    errorCount++;
                    break;
                case ScheduleConflict.LEVEL_WARNING:
                    warningCount++;
                    break;
                case ScheduleConflict.LEVEL_INFO:
                    infoCount++;
                    break;
                default:
                    break;
            }
        }

        ConflictCheckResultVO.Summary summary = new ConflictCheckResultVO.Summary(
                errorCount, warningCount, infoCount);
        result.setSummary(summary);
        result.setCanPublish(errorCount == 0);

        // 转换为VO列表
        List<ConflictDetailVO> voList = conflicts.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        result.setConflicts(voList);

        return result;
    }

    /**
     * Entity → VO 转换
     * @param entity 冲突实体
     * @return 视图对象
     */
    private ConflictDetailVO convertToVO(ScheduleConflict entity) {
        if (entity == null) {
            return null;
        }

        ConflictDetailVO vo = new ConflictDetailVO();
        vo.setConflictId(entity.getConflictId());
        vo.setLevel(entity.getLevel());
        vo.setType(entity.getType());
        vo.setEmployeeId(entity.getEmployeeId());
        vo.setEmployeeName(entity.getEmployeeName());
        if (entity.getConflictDate() != null) {
            vo.setDate(entity.getConflictDate().format(DATE_FORMATTER));
        }
        vo.setShiftType(entity.getShiftType());
        vo.setRuleId(entity.getRuleId());
        vo.setMessage(entity.getMessage());
        vo.setSuggestion(entity.getSuggestion());
        vo.setAutoFixAvailable(entity.getAutoFixAvailable());
        vo.setFixStatus(entity.getFixStatus());

        return vo;
    }
}
