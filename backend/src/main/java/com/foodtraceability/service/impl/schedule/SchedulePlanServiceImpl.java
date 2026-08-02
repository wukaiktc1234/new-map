package com.foodtraceability.service.impl.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.StoreBasicInfo;
import com.foodtraceability.dto.schedule.*;
import com.foodtraceability.entity.schedule.ScheduleEntry;
import com.foodtraceability.entity.schedule.SchedulePlan;
import com.foodtraceability.entity.schedule.ScheduleShiftType;
import com.foodtraceability.mapper.schedule.ScheduleEntryMapper;
import com.foodtraceability.mapper.schedule.SchedulePlanMapper;
import com.foodtraceability.mapper.schedule.ScheduleShiftTypeMapper;
import com.foodtraceability.service.StoreDataService;
import com.foodtraceability.service.schedule.ScheduleAttendanceSyncService;
import com.foodtraceability.service.schedule.SchedulePlanService;
import com.foodtraceability.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 排班方案服务实现类
 * 实现排班方案的增删改查及统计功能（F-004）
 */
@Service
public class SchedulePlanServiceImpl implements SchedulePlanService {

    private static final Logger log = LoggerFactory.getLogger(SchedulePlanServiceImpl.class);

    /** 日期格式化器 */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /** 时间格式化器 */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final SchedulePlanMapper schedulePlanMapper;
    private final ScheduleEntryMapper scheduleEntryMapper;
    private final ScheduleShiftTypeMapper scheduleShiftTypeMapper;
    private final StoreDataService storeDataService;
    private final ScheduleAttendanceSyncService scheduleAttendanceSyncService;

    public SchedulePlanServiceImpl(SchedulePlanMapper schedulePlanMapper,
                                   ScheduleEntryMapper scheduleEntryMapper,
                                   ScheduleShiftTypeMapper scheduleShiftTypeMapper,
                                   StoreDataService storeDataService,
                                   @Lazy ScheduleAttendanceSyncService scheduleAttendanceSyncService) {
        this.schedulePlanMapper = schedulePlanMapper;
        this.scheduleEntryMapper = scheduleEntryMapper;
        this.scheduleShiftTypeMapper = scheduleShiftTypeMapper;
        this.storeDataService = storeDataService;
        this.scheduleAttendanceSyncService = scheduleAttendanceSyncService;
    }

    @Override
    public PageResult<SchedulePlanVO> getPlanList(SchedulePlanQueryDTO queryDTO) {
        // 获取当前用户门店ID（数据隔离）
        // 容错：管理员或未分配门店的用户调用时，直接返回空列表，避免抛出异常阻塞前端
        String storeIdStr = SecurityUtils.getCurrentUserStoreId();
        if (storeIdStr == null || storeIdStr.isEmpty()) {
            log.info("当前用户未分配门店，返回空排班方案列表");
            Long current = queryDTO.getPage() != null ? queryDTO.getPage().longValue() : 1L;
            Long size = queryDTO.getSize() != null ? queryDTO.getSize().longValue() : 20L;
            return new PageResult<>(0L, java.util.Collections.emptyList(), current, size);
        }
        Long storeId = Long.parseLong(storeIdStr);

        // 构建分页对象
        Page<SchedulePlan> page = new Page<>(
                queryDTO.getPage() != null ? queryDTO.getPage() : 1,
                queryDTO.getSize() != null ? queryDTO.getSize() : 20
        );

        // 构建查询条件
        LambdaQueryWrapper<SchedulePlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SchedulePlan::getStoreId, storeId);

        // 按状态筛选
        if (queryDTO.getStatus() != null && !queryDTO.getStatus().isEmpty()) {
            wrapper.eq(SchedulePlan::getStatus, queryDTO.getStatus());
        }

        // 按周期起始日期筛选（>= startDate）
        if (queryDTO.getStartDate() != null) {
            wrapper.ge(SchedulePlan::getStartDate, queryDTO.getStartDate());
        }

        // 按周期结束日期筛选（<= endDate）
        if (queryDTO.getEndDate() != null) {
            wrapper.le(SchedulePlan::getEndDate, queryDTO.getEndDate());
        }

        // 按模板ID筛选
        if (queryDTO.getTemplateId() != null) {
            wrapper.eq(SchedulePlan::getTemplateId, queryDTO.getTemplateId());
        }

        // 按更新时间倒序排序
        wrapper.orderByDesc(SchedulePlan::getUpdateTime);

        // 执行分页查询
        Page<SchedulePlan> resultPage = schedulePlanMapper.selectPage(page, wrapper);

        // 转换为VO列表
        List<SchedulePlanVO> voList = resultPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageResult<>(
                resultPage.getTotal(),
                voList,
                resultPage.getCurrent(),
                resultPage.getSize()
        );
    }

    @Override
    public SchedulePlanVO getPlanById(String planId) {
        SchedulePlan entity = schedulePlanMapper.selectById(planId);
        if (entity == null) {
            return null;
        }

        // 转换为VO（含冗余字段）
        SchedulePlanVO vo = convertToVO(entity);

        // 查询关联的排班条目列表
        List<ScheduleEntry> entries = scheduleEntryMapper.selectByPlanIds(List.of(planId));
        List<ScheduleEntryVO> entryVOList = entries.stream()
                .map(this::convertEntryToVO)
                .collect(Collectors.toList());
        vo.setEntries(entryVOList);

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SchedulePlanVO createPlan(SchedulePlanCreateDTO createDTO) {
        Long storeId = getCurrentUserStoreId();

        // 校验：结束日期不能早于起始日期
        if (createDTO.getEndDate().isBefore(createDTO.getStartDate())) {
            throw new RuntimeException("结束日期不能早于起始日期");
        }

        // 构建实体
        SchedulePlan plan = new SchedulePlan();
        plan.setPlanName(createDTO.getPlanName());
        plan.setStoreId(storeId);
        plan.setStartDate(createDTO.getStartDate());
        plan.setEndDate(createDTO.getEndDate());

        // 默认值设置
        plan.setStatus(SchedulePlan.STATUS_DRAFT);
        plan.setVersion(1);
        plan.setEmployeeCount(0);
        plan.setTotalWorkHours(0);
        plan.setSyncStatus(SchedulePlan.SYNC_STATUS_NOT_SYNCED);

        // 可选字段
        if (createDTO.getTemplateId() != null) {
            plan.setTemplateId(createDTO.getTemplateId());
        }

        // 插入数据库
        schedulePlanMapper.insert(plan);

        log.info("创建排班方案成功: planId={}, planName={}, storeId={}",
                plan.getPlanId(), plan.getPlanName(), storeId);

        return convertToVO(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SchedulePlanVO updatePlan(String planId, SchedulePlanUpdateDTO updateDTO) {
        // 查询现有方案
        SchedulePlan existing = schedulePlanMapper.selectById(planId);
        if (existing == null) {
            throw new RuntimeException("排班方案不存在");
        }

        // 业务规则校验：仅draft状态可编辑
        if (!SchedulePlan.STATUS_DRAFT.equals(existing.getStatus())) {
            throw new RuntimeException("方案已发布无法编辑");
        }

        // 更新非空字段
        boolean needUpdate = false;

        if (updateDTO.getPlanName() != null && !updateDTO.getPlanName().isEmpty()) {
            existing.setPlanName(updateDTO.getPlanName());
            needUpdate = true;
        }
        if (updateDTO.getStartDate() != null) {
            existing.setStartDate(updateDTO.getStartDate());
            needUpdate = true;
        }
        if (updateDTO.getEndDate() != null) {
            existing.setEndDate(updateDTO.getEndDate());
            needUpdate = true;
        }
        if (updateDTO.getTemplateId() != null) {
            existing.setTemplateId(updateDTO.getTemplateId());
            needUpdate = true;
        }

        // 校验日期范围
        if (existing.getStartDate() != null && existing.getEndDate() != null
                && existing.getEndDate().isBefore(existing.getStartDate())) {
            throw new RuntimeException("结束日期不能早于起始日期");
        }

        if (needUpdate) {
            try {
                schedulePlanMapper.updateById(existing);
            } catch (OptimisticLockingFailureException e) {
                log.warn("乐观锁冲突: planId={}", planId);
                throw new RuntimeException("数据已被其他人修改，请刷新后重试");
            }

            log.info("更新排班方案成功: planId={}", planId);
        }

        return convertToVO(schedulePlanMapper.selectById(planId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePlan(String planId) {
        // 查询现有方案
        SchedulePlan existing = schedulePlanMapper.selectById(planId);
        if (existing == null) {
            throw new RuntimeException("排班方案不存在");
        }

        // 业务规则校验：仅draft状态可删除
        if (!SchedulePlan.STATUS_DRAFT.equals(existing.getStatus())) {
            throw new RuntimeException("已发布的方案无法删除");
        }

        // 逻辑删除（MyBatis Plus自动处理@TableLogic）
        int rows = schedulePlanMapper.deleteById(planId);
        if (rows <= 0) {
            throw new RuntimeException("排班方案不存在或已被删除");
        }

        log.info("删除排班方案成功: planId={}", planId);
    }

    @Override
    public SchedulePlanStatsVO getPlanStats() {
        Long storeId = getCurrentUserStoreId();

        // 统计各状态的方案数量
        long totalCount = countByStatus(storeId, null);
        long draftCount = countByStatus(storeId, SchedulePlan.STATUS_DRAFT);
        long publishedCount = countByStatus(storeId, SchedulePlan.STATUS_PUBLISHED);
        long executingCount = countByStatus(storeId, SchedulePlan.STATUS_EXECUTING);

        return new SchedulePlanStatsVO(totalCount, draftCount, publishedCount, executingCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SchedulePlanVO publishPlan(String planId) {
        SchedulePlan existing = schedulePlanMapper.selectById(planId);
        if (existing == null) {
            throw new RuntimeException("排班方案不存在");
        }

        // 业务规则校验：仅 draft 状态可发布
        if (!SchedulePlan.STATUS_DRAFT.equals(existing.getStatus())) {
            throw new RuntimeException("仅草稿状态的方案可发布，当前状态：" + existing.getStatus());
        }

        // 更新发布信息
        existing.setStatus(SchedulePlan.STATUS_PUBLISHED);
        existing.setPublisherId(SecurityUtils.getCurrentUserId());
        existing.setPublisherName(SecurityUtils.getCurrentUsername());
        existing.setPublishTime(LocalDateTime.now());
        existing.setUpdateTime(LocalDateTime.now());

        try {
            schedulePlanMapper.updateById(existing);
        } catch (OptimisticLockingFailureException e) {
            log.warn("发布方案时乐观锁冲突: planId={}", planId);
            throw new RuntimeException("数据已被其他人修改，请刷新后重试");
        }

        // 发布成功后触发排班→考勤同步（同步失败不影响发布事务）
        try {
            scheduleAttendanceSyncService.syncToAttendance(planId);
        } catch (Exception e) {
            log.warn("发布方案后触发考勤同步失败，不影响发布结果: planId={}", planId, e);
        }

        log.info("发布排班方案成功: planId={}, publisher={}", planId, existing.getPublisherName());
        return convertToVO(schedulePlanMapper.selectById(planId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SchedulePlanVO withdrawPlan(String planId, String withdrawReason) {
        SchedulePlan existing = schedulePlanMapper.selectById(planId);
        if (existing == null) {
            throw new RuntimeException("排班方案不存在");
        }

        // 业务规则校验：仅 published 状态可撤回
        if (!SchedulePlan.STATUS_PUBLISHED.equals(existing.getStatus())) {
            throw new RuntimeException("仅已发布的方案可撤回，当前状态：" + existing.getStatus());
        }

        // 校验撤回原因
        if (withdrawReason == null || withdrawReason.trim().isEmpty()) {
            throw new RuntimeException("撤回原因不能为空");
        }

        // 更新撤回信息，状态回退为 draft
        existing.setStatus(SchedulePlan.STATUS_DRAFT);
        existing.setWithdrawerId(SecurityUtils.getCurrentUserId());
        existing.setWithdrawerName(SecurityUtils.getCurrentUsername());
        existing.setWithdrawTime(LocalDateTime.now());
        existing.setWithdrawReason(withdrawReason);
        existing.setUpdateTime(LocalDateTime.now());

        try {
            schedulePlanMapper.updateById(existing);
        } catch (OptimisticLockingFailureException e) {
            log.warn("撤回方案时乐观锁冲突: planId={}", planId);
            throw new RuntimeException("数据已被其他人修改，请刷新后重试");
        }

        log.info("撤回排班方案成功: planId={}, withdrawer={}, reason={}",
                planId, existing.getWithdrawerName(), withdrawReason);
        return convertToVO(schedulePlanMapper.selectById(planId));
    }

    // ==================== 时间线日历(F-001)核心方法实现 ====================

    @Override
    public TimelineResponseVO getTimelineData(String planId, TimelineRequestDTO requestDTO) {
        // 1. 校验方案存在性
        SchedulePlan plan = schedulePlanMapper.selectById(planId);
        if (plan == null) {
            throw new RuntimeException("排班方案不存在");
        }

        // 2. 解析周起始日期，计算7天范围
        LocalDate weekStart = LocalDate.parse(requestDTO.getWeekStart(), DATE_FORMATTER);
        LocalDate weekEnd = weekStart.plusDays(6);

        // 生成7天日期列表
        List<String> dates = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            dates.add(weekStart.plusDays(i).format(DATE_FORMATTER));
        }

        // 3. 批量查询该方案在7天内的所有排班条目（性能优化：一次查询）
        List<ScheduleEntry> allEntries = scheduleEntryMapper.selectByPlanAndDateRange(
                planId, weekStart, weekEnd);

        // 4. 提取涉及的员工ID列表（去重）
        List<Long> employeeIds = allEntries.stream()
                .map(ScheduleEntry::getEmployeeId)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        // 如果没有员工，返回空的时间线
        if (employeeIds.isEmpty()) {
            TimelineResponseVO emptyResponse = new TimelineResponseVO();
            emptyResponse.setWeekStart(weekStart.format(DATE_FORMATTER));
            emptyResponse.setWeekEnd(weekEnd.format(DATE_FORMATTER));
            emptyResponse.setDates(dates);
            emptyResponse.setEmployees(new ArrayList<>());
            emptyResponse.setDailySummary(buildEmptyDailySummary(dates));
            return emptyResponse;
        }

        // 5. 按员工ID分组构建Map<employeeId, List<Entry>>
        Map<Long, List<ScheduleEntry>> entriesByEmployee = allEntries.stream()
                .collect(Collectors.groupingBy(ScheduleEntry::getEmployeeId));

        // 6. 获取班次配置信息（用于填充颜色、时间段等）
        Long storeId = plan.getStoreId();
        List<ScheduleShiftType> shiftTypes = scheduleShiftTypeMapper.selectActiveByStoreId(storeId);
        Map<String, ScheduleShiftType> shiftTypeMap = shiftTypes.stream()
                .collect(Collectors.toMap(
                        ScheduleShiftType::getShiftCode,
                        st -> st,
                        (existing, replacement) -> existing));

        // 7. 构建员工时间线数据
        List<TimelineEmployeeVO> employees = new ArrayList<>();
        for (Long empId : employeeIds) {
            TimelineEmployeeVO employeeVO = buildTimelineEmployee(
                    empId,
                    entriesByEmployee.getOrDefault(empId, Collections.emptyList()),
                    dates,
                    shiftTypeMap);
            employees.add(employeeVO);
        }

        // 8. 构建每日汇总
        List<DailySummaryVO> dailySummary = buildDailySummary(allEntries, dates, shiftTypeMap);

        // 9. 组装响应
        TimelineResponseVO response = new TimelineResponseVO();
        response.setWeekStart(weekStart.format(DATE_FORMATTER));
        response.setWeekEnd(weekEnd.format(DATE_FORMATTER));
        response.setDates(dates);
        response.setEmployees(employees);
        response.setDailySummary(dailySummary);

        log.info("获取时间线数据成功: planId={}, weekStart={}, employeeCount={}",
                planId, requestDTO.getWeekStart(), employees.size());

        return response;
    }

    @Override
    public PageResult<ScheduleEntryVO> getEntryList(String planId, ScheduleEntryQueryDTO queryDTO) {
        // 校验方案存在
        SchedulePlan plan = schedulePlanMapper.selectById(planId);
        if (plan == null) {
            throw new RuntimeException("排班方案不存在");
        }

        // 构建分页对象
        Page<ScheduleEntry> page = new Page<>(
                queryDTO.getPage() != null ? queryDTO.getPage() : 1,
                queryDTO.getSize() != null ? queryDTO.getSize() : 20
        );

        // 构建查询条件
        LambdaQueryWrapper<ScheduleEntry> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScheduleEntry::getPlanId, planId);

        // 按员工筛选
        if (queryDTO.getEmployeeId() != null) {
            wrapper.eq(ScheduleEntry::getEmployeeId, queryDTO.getEmployeeId());
        }

        // 按日期范围筛选
        if (queryDTO.getStartDate() != null) {
            wrapper.ge(ScheduleEntry::getWorkDate, queryDTO.getStartDate());
        }
        if (queryDTO.getEndDate() != null) {
            wrapper.le(ScheduleEntry::getWorkDate, queryDTO.getEndDate());
        }

        // 按班次类型筛选
        if (queryDTO.getShiftType() != null && !queryDTO.getShiftType().isEmpty()) {
            wrapper.eq(ScheduleEntry::getShiftType, queryDTO.getShiftType());
        }

        // 按工作日期和排序顺序排序
        wrapper.orderByAsc(ScheduleEntry::getWorkDate)
               .orderByAsc(ScheduleEntry::getSortOrder);

        // 执行分页查询
        Page<ScheduleEntry> resultPage = scheduleEntryMapper.selectPage(page, wrapper);

        // 转换为VO列表
        List<ScheduleEntryVO> voList = resultPage.getRecords().stream()
                .map(this::convertEntryToVO)
                .collect(Collectors.toList());

        return new PageResult<>(
                resultPage.getTotal(),
                voList,
                resultPage.getCurrent(),
                resultPage.getSize()
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchUpdateResult batchUpdateEntries(String planId, ScheduleEntryBatchUpdateDTO batchUpdateDTO) {
        // 校验方案存在
        SchedulePlan plan = schedulePlanMapper.selectById(planId);
        if (plan == null) {
            throw new RuntimeException("排班方案不存在");
        }

        // 校验方案状态（仅draft/published可编辑）
        if (!SchedulePlan.STATUS_DRAFT.equals(plan.getStatus())
                && !SchedulePlan.STATUS_PUBLISHED.equals(plan.getStatus())) {
            throw new RuntimeException("当前方案状态不允许编辑排班");
        }

        Long storeId = plan.getStoreId();
        List<ScheduleEntryBatchUpdateDTO.EntryUpdateItem> items = batchUpdateDTO.getEntries();

        int successCount = 0;
        int failedCount = 0;
        List<String> errors = new ArrayList<>();

        // 获取班次配置（用于填充shiftName/color/durationMinutes等）
        List<ScheduleShiftType> shiftTypes = scheduleShiftTypeMapper.selectActiveByStoreId(storeId);
        Map<String, ScheduleShiftType> shiftTypeMap = shiftTypes.stream()
                .collect(Collectors.toMap(
                        ScheduleShiftType::getShiftCode,
                        st -> st,
                        (existing, replacement) -> existing));

        for (ScheduleEntryBatchUpdateDTO.EntryUpdateItem item : items) {
            try {
                processSingleEntryUpdate(planId, storeId, item, shiftTypeMap);
                successCount++;
            } catch (RuntimeException e) {
                failedCount++;
                errors.add("员工[" + item.getEmployeeId() + "] " + item.getWorkDate() + ": " + e.getMessage());
                log.warn("批量更新单条失败: employeeId={}, workDate={}, error={}",
                        item.getEmployeeId(), item.getWorkDate(), e.getMessage());
            }
        }

        // 更新方案的统计字段
        updatePlanStatistics(planId);

        log.info("批量更新排班条目完成: planId={}, success={}, failed={}",
                planId, successCount, failedCount);

        return new BatchUpdateResult(successCount, failedCount, errors);
    }

    // ==================== 私有方法 ====================

    /**
     * 获取当前用户门店ID
     * @return 门店ID（Long类型）
     */
    private Long getCurrentUserStoreId() {
        String storeIdStr = SecurityUtils.getCurrentUserStoreId();
        if (storeIdStr == null || storeIdStr.isEmpty()) {
            throw new RuntimeException("当前用户未分配门店，无法操作");
        }
        return Long.parseLong(storeIdStr);
    }

    /**
     * 按状态统计门店下的方案数量
     * @param storeId 门店ID
     * @param status 状态（null表示不限制）
     * @return 数量
     */
    private long countByStatus(Long storeId, String status) {
        LambdaQueryWrapper<SchedulePlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SchedulePlan::getStoreId, storeId);
        if (status != null) {
            wrapper.eq(SchedulePlan::getStatus, status);
        }
        return schedulePlanMapper.selectCount(wrapper);
    }

    /**
     * 将排班方案实体转换为视图对象
     * 填充冗余字段（门店名、模板名、状态名、工时显示等）
     * @param entity 排班方案实体
     * @return 视图对象
     */
    private SchedulePlanVO convertToVO(SchedulePlan entity) {
        if (entity == null) {
            return null;
        }

        SchedulePlanVO vo = new SchedulePlanVO();

        // 基础字段
        vo.setPlanId(entity.getPlanId());
        vo.setPlanName(entity.getPlanName());
        vo.setStoreId(entity.getStoreId());
        vo.setStatus(entity.getStatus());
        vo.setStatusName(getStatusDisplayName(entity.getStatus()));
        vo.setVersion(entity.getVersion());
        vo.setTemplateId(entity.getTemplateId());
        vo.setEmployeeCount(entity.getEmployeeCount());
        vo.setTotalWorkHoursDisplay(formatWorkHours(entity.getTotalWorkHours()));

        // 日期格式化
        if (entity.getStartDate() != null) {
            vo.setStartDate(entity.getStartDate().format(DATE_FORMATTER));
        }
        if (entity.getEndDate() != null) {
            vo.setEndDate(entity.getEndDate().format(DATE_FORMATTER));
        }

        // 发布信息
        vo.setPublisherId(entity.getPublisherId());
        vo.setPublisherName(entity.getPublisherName());
        vo.setPublishTime(entity.getPublishTime());

        // 撤回信息
        vo.setWithdrawerId(entity.getWithdrawerId());
        vo.setWithdrawerName(entity.getWithdrawerName());
        vo.setWithdrawTime(entity.getWithdrawTime());
        vo.setWithdrawReason(entity.getWithdrawReason());

        // 同步信息
        vo.setSyncStatus(entity.getSyncStatus());
        vo.setSyncToAttendanceTime(entity.getSyncToAttendanceTime());
        vo.setSyncMessage(entity.getSyncMessage());

        // 时间戳
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());

        // 填充冗余关联名称
        fillRelatedNames(vo);

        return vo;
    }

    /**
     * 将排班条目实体转换为视图对象
     * @param entity 排班条目实体
     * @return 视图对象
     */
    private ScheduleEntryVO convertEntryToVO(ScheduleEntry entity) {
        if (entity == null) {
            return null;
        }

        ScheduleEntryVO vo = new ScheduleEntryVO();
        vo.setEntryId(entity.getEntryId());
        vo.setEmployeeId(entity.getEmployeeId());
        vo.setEmployeeName(entity.getEmployeeName());
        vo.setPositionName(entity.getPositionName());

        if (entity.getWorkDate() != null) {
            vo.setWorkDate(entity.getWorkDate().format(DATE_FORMATTER));
        }

        vo.setShiftType(entity.getShiftType());
        vo.setShiftName(entity.getShiftName());

        if (entity.getStartTime() != null) {
            vo.setStartTime(entity.getStartTime().format(TIME_FORMATTER));
        }
        if (entity.getEndTime() != null) {
            vo.setEndTime(entity.getEndTime().format(TIME_FORMATTER));
        }

        vo.setSource(entity.getSource());
        vo.setConflictLevel(entity.getConflictLevel());
        vo.setConflictMessage(entity.getConflictMessage());
        vo.setCreateTime(entity.getCreateTime());

        return vo;
    }

    /**
     * 填充冗余关联名称（门店名、模板名）
     * @param vo 视图对象
     */
    private void fillRelatedNames(SchedulePlanVO vo) {
        // 填充门店名称
        if (vo.getStoreId() != null) {
            StoreBasicInfo storeInfo = storeDataService.getStoreBasicInfo(
                    String.valueOf(vo.getStoreId()));
            if (storeInfo != null) {
                vo.setStoreName(storeInfo.getStoreName());
            }
        }

        // 填充模板名称（后续可通过ScheduleTemplateDataService获取）
        // 当前先留空，待模板模块完善后补充
    }

    /**
     * 获取状态的中文名称
     * @param status 状态编码
     * @return 中文名称
     */
    private String getStatusDisplayName(String status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case SchedulePlan.STATUS_DRAFT:
                return "草稿";
            case SchedulePlan.STATUS_PUBLISHED:
                return "已发布";
            case SchedulePlan.STATUS_EXECUTING:
                return "执行中";
            case SchedulePlan.STATUS_ARCHIVED:
                return "已归档";
            default:
                return status;
        }
    }

    /**
     * 格式化工时显示（分钟转小时）
     * @param totalMinutes 总分钟数
     * @return 格式化的工时字符串（如"8.5小时"）
     */
    private String formatWorkHours(Integer totalMinutes) {
        if (totalMinutes == null || totalMinutes == 0) {
            return "0小时";
        }
        double hours = totalMinutes / 60.0;
        // 如果是整数小时则不显示小数
        if (hours == Math.floor(hours)) {
            return (int) hours + "小时";
        }
        return String.format("%.1f小时", hours);
    }

    // ==================== 时间线日历(F-001)辅助方法 ====================

    /**
     * 构建单个员工的时间线数据
     * 包含7天排班条目和统计信息（本周工时、连续工作天数、冲突检测）
     *
     * @param employeeId 员工ID
     * @param entries 该员工在7天内的排班条目列表
     * @param dates 7天日期数组
     * @param shiftTypeMap 班次配置映射(shiftCode -> ScheduleShiftType)
     * @return 员工时间线VO
     */
    private TimelineEmployeeVO buildTimelineEmployee(
            Long employeeId,
            List<ScheduleEntry> entries,
            List<String> dates,
            Map<String, ScheduleShiftType> shiftTypeMap) {

        TimelineEmployeeVO employeeVO = new TimelineEmployeeVO();
        employeeVO.setEmployeeId(employeeId);

        // 从第一条记录获取员工姓名和岗位（冗余字段）
        if (!entries.isEmpty()) {
            ScheduleEntry firstEntry = entries.get(0);
            employeeVO.setEmployeeName(firstEntry.getEmployeeName());
            employeeVO.setPositionName(firstEntry.getPositionName());
        }

        // 按日期建立索引 Map<dateStr, Entry>
        Map<String, ScheduleEntry> entryByDate = entries.stream()
                .collect(Collectors.toMap(
                        e -> e.getWorkDate().format(DATE_FORMATTER),
                        e -> e,
                        (existing, replacement) -> existing));

        // 构建7天排班条目列表
        List<TimelineEntryVO> entryVOList = new ArrayList<>();
        int weeklyHours = 0;
        boolean hasConflict = false;

        for (String date : dates) {
            ScheduleEntry entry = entryByDate.get(date);
            TimelineEntryVO entryVO = convertToTimelineEntry(entry, date, shiftTypeMap);
            entryVOList.add(entryVO);

            // 累加工时（非休息班次）
            if (entry != null && entry.getShiftType() != null) {
                ScheduleShiftType shiftType = shiftTypeMap.get(entry.getShiftType());
                if (shiftType != null && Boolean.FALSE.equals(shiftType.getIsRest())) {
                    Integer duration = shiftType.getDurationMinutes();
                    if (duration != null) {
                        weeklyHours += duration;
                    }
                }
            }

            // 检测冲突
            if (entryVO != null && entryVO.getConflictLevel() != null
                    && !"info".equals(entryVO.getConflictLevel())) {
                hasConflict = true;
            }
        }

        employeeVO.setEntries(entryVOList);
        employeeVO.setWeeklyHours(weeklyHours);
        employeeVO.setHasConflict(hasConflict);

        // 计算连续工作天数（从当前日期向前追溯）
        int consecutiveDays = calculateConsecutiveDays(employeeId, LocalDate.now(), shiftTypeMap);
        employeeVO.setConsecutiveDays(consecutiveDays);

        return employeeVO;
    }

    /**
     * 将排班条目实体转换为时间线条目VO
     * 关联班次配置获取颜色、时间段等信息
     *
     * @param entity 排班条目实体（可能为null，表示无排班）
     * @param date 日期字符串(YYYY-MM-DD)
     * @param shiftTypeMap 班次配置映射
     * @return 时间线条目VO
     */
    private TimelineEntryVO convertToTimelineEntry(
            ScheduleEntry entity,
            String date,
            Map<String, ScheduleShiftType> shiftTypeMap) {

        TimelineEntryVO vo = new TimelineEntryVO();
        vo.setDate(date);

        if (entity == null) {
            // 无排班的日期返回空条目（前端显示为空白/休息格）
            return vo;
        }

        vo.setEntryId(entity.getEntryId());
        vo.setShiftType(entity.getShiftType());
        vo.setShiftName(entity.getShiftName());
        vo.setSource(entity.getSource());
        vo.setConflictLevel(entity.getConflictLevel());
        vo.setConflictMessage(entity.getConflictMessage());

        // 时间格式化
        if (entity.getStartTime() != null) {
            vo.setStartTime(entity.getStartTime().format(TIME_FORMATTER));
        }
        if (entity.getEndTime() != null) {
            vo.setEndTime(entity.getEndTime().format(TIME_FORMATTER));
        }

        // 从班次配置获取颜色信息
        if (entity.getShiftType() != null && shiftTypeMap.containsKey(entity.getShiftType())) {
            ScheduleShiftType shiftType = shiftTypeMap.get(entity.getShiftType());
            vo.setShiftColor(shiftType.getColor());

            // 如果实体中缺少名称/时间，从班次配置补充
            if (vo.getShiftName() == null || vo.getShiftName().isEmpty()) {
                vo.setShiftName(shiftType.getShiftName());
            }
            if (vo.getStartTime() == null && shiftType.getStartTime() != null) {
                vo.setStartTime(shiftType.getStartTime().format(TIME_FORMATTER));
            }
            if (vo.getEndTime() == null && shiftType.getEndTime() != null) {
                vo.setEndTime(shiftType.getEndTime().format(TIME_FORMATTER));
            }
        }

        return vo;
    }

    /**
     * 构建每日汇总数据
     * 计算每天各时段的需求、实际、缺员情况
     *
     * @param allEntries 7天内所有排班条目
     * @param dates 7天日期数组
     * @param shiftTypeMap 班次配置映射
     * @return 每日汇总列表
     */
    private List<DailySummaryVO> buildDailySummary(
            List<ScheduleEntry> allEntries,
            List<String> dates,
            Map<String, ScheduleShiftType> shiftTypeMap) {

        List<DailySummaryVO> summaryList = new ArrayList<>();

        // 按日期分组统计实际排班人数
        Map<String, Map<String, Long>> actualByDateAndShift = allEntries.stream()
                .filter(e -> e.getShiftType() != null
                        && !ScheduleShiftType.SHIFT_CODE_NIGHT_OFF.equals(e.getShiftType()))
                .collect(Collectors.groupingBy(
                        e -> e.getWorkDate().format(DATE_FORMATTER),
                        Collectors.groupingBy(
                                ScheduleEntry::getShiftType,
                                Collectors.counting())));

        // 定义需要统计的时段
        String[] shiftTypes = {
                ScheduleShiftType.SHIFT_CODE_MORNING,
                ScheduleShiftType.SHIFT_CODE_NOON,
                ScheduleShiftType.SHIFT_CODE_EVENING
        };

        for (String date : dates) {
            DailySummaryVO summary = new DailySummaryVO();
            summary.setDate(date);

            // 计算星期几和是否周末
            LocalDate localDate = LocalDate.parse(date, DATE_FORMATTER);
            DayOfWeek dayOfWeek = localDate.getDayOfWeek();
            summary.setDayOfWeek(getDayOfWeekChinese(dayOfWeek));
            summary.setIsWeekend(dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY);
            summary.setIsHoliday(false); // 后续可通过节假日服务获取

            // 初始化各时段的demand/actual/shortage
            Map<String, Integer> demand = new HashMap<>();
            Map<String, Integer> actual = new HashMap<>();
            Map<String, Integer> shortage = new HashMap<>();

            for (String shift : shiftTypes) {
                // 实际排班人数
                long actualCount = actualByDateAndShift
                        .getOrDefault(date, Collections.emptyMap())
                        .getOrDefault(shift, 0L);
                actual.put(shift, (int) actualCount);

                // 需求人数（当前默认为0，后续可从规则模板获取）
                // TODO: 需求人数应从排班规则或门店配置获取
                demand.put(shift, 0);

                // 缺员计算: demand - actual
                shortage.put(shift, demand.get(shift) - (int) actualCount);
            }

            summary.setDemand(demand);
            summary.setActual(actual);
            summary.setShortage(shortage);

            summaryList.add(summary);
        }

        return summaryList;
    }

    /**
     * 构建空的每日汇总（无员工时使用）
     * @param dates 7天日期数组
     * @return 空的每日汇总列表
     */
    private List<DailySummaryVO> buildEmptyDailySummary(List<String> dates) {
        List<DailySummaryVO> summaryList = new ArrayList<>();
        String[] shiftTypes = {
                ScheduleShiftType.SHIFT_CODE_MORNING,
                ScheduleShiftType.SHIFT_CODE_NOON,
                ScheduleShiftType.SHIFT_CODE_EVENING
        };

        for (String date : dates) {
            DailySummaryVO summary = new DailySummaryVO();
            summary.setDate(date);

            LocalDate localDate = LocalDate.parse(date, DATE_FORMATTER);
            DayOfWeek dayOfWeek = localDate.getDayOfWeek();
            summary.setDayOfWeek(getDayOfWeekChinese(dayOfWeek));
            summary.setIsWeekend(dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY);
            summary.setIsHoliday(false);

            Map<String, Integer> emptyMap = new HashMap<>();
            for (String shift : shiftTypes) {
                emptyMap.put(shift, 0);
            }

            summary.setDemand(emptyMap);
            summary.setActual(emptyMap);
            summary.setShortage(emptyMap);

            summaryList.add(summary);
        }

        return summaryList;
    }

    /**
     * 获取星期几的中文名称
     * @param dayOfWeek 星期枚举
     * @return 中文名称（如"周一"）
     */
    private String getDayOfWeekChinese(DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case MONDAY: return "周一";
            case TUESDAY: return "周二";
            case WEDNESDAY: return "周三";
            case THURSDAY: return "周四";
            case FRIDAY: return "周五";
            case SATURDAY: return "周六";
            case SUNDAY: return "周日";
            default: return "";
        }
    }

    /**
     * 计算员工的连续工作天数
     * 从指定日期向前追溯，统计连续非休息天数
     *
     * @param employeeId 员工ID
     * @param referenceDate 参考日期（通常为今天或周最后一天）
     * @param shiftTypeMap 班次配置映射（用于判断是否休息班次）
     * @return 连续工作天数
     */
    private int calculateConsecutiveDays(
            Long employeeId,
            LocalDate referenceDate,
            Map<String, ScheduleShiftType> shiftTypeMap) {

        int consecutiveDays = 0;

        // 向前追溯最多30天（避免无限循环）
        for (int i = 0; i < 30; i++) {
            LocalDate checkDate = referenceDate.minusDays(i);
            ScheduleEntry entry = scheduleEntryMapper.selectByEmployeeAndDate(employeeId, checkDate);

            if (entry == null) {
                // 无排班记录视为休息
                break;
            }

            // 判断是否休息班次
            boolean isRest = false;
            if (entry.getShiftType() != null) {
                ScheduleShiftType shiftType = shiftTypeMap.get(entry.getShiftType());
                if (shiftType != null && Boolean.TRUE.equals(shiftType.getIsRest())) {
                    isRest = true;
                } else if (ScheduleShiftType.SHIFT_CODE_NIGHT_OFF.equals(entry.getShiftType())) {
                    isRest = true;
                }
            }

            if (isRest) {
                break;
            }

            consecutiveDays++;
        }

        return consecutiveDays;
    }

    /**
     * 处理单条排班条目的更新或新增
     *
     * @param planId 方案ID
     * @param storeId 门店ID
     * @param item 更新项
     * @param shiftTypeMap 班次配置映射
     */
    private void processSingleEntryUpdate(
            String planId,
            Long storeId,
            ScheduleEntryBatchUpdateDTO.EntryUpdateItem item,
            Map<String, ScheduleShiftType> shiftTypeMap) {

        LocalDate workDate = LocalDate.parse(item.getWorkDate(), DATE_FORMATTER);

        if (item.getEntryId() != null && !item.getEntryId().isEmpty()) {
            // 更新现有条目
            updateExistingEntry(planId, item, workDate, shiftTypeMap);
        } else {
            // 创建新条目
            createNewEntry(planId, storeId, item, workDate, shiftTypeMap);
        }
    }

    /**
     * 更新现有的排班条目
     */
    private void updateExistingEntry(
            String planId,
            ScheduleEntryBatchUpdateDTO.EntryUpdateItem item,
            LocalDate workDate,
            Map<String, ScheduleShiftType> shiftTypeMap) {

        ScheduleEntry existing = scheduleEntryMapper.selectById(item.getEntryId());
        if (existing == null) {
            throw new RuntimeException("排班条目不存在");
        }

        // 校验归属方案
        if (!planId.equals(existing.getPlanId())) {
            throw new RuntimeException("条目不属于当前方案");
        }

        // 更新班次类型
        if (item.getShiftType() != null) {
            existing.setShiftType(item.getShiftType());

            // 从班次配置填充详细信息
            if (shiftTypeMap.containsKey(item.getShiftType())) {
                ScheduleShiftType shiftType = shiftTypeMap.get(item.getShiftType());
                existing.setShiftName(shiftType.getShiftName());
                existing.setStartTime(shiftType.getStartTime());
                existing.setEndTime(shiftType.getEndTime());
            }
        }

        // 更新来源
        if (item.getSource() != null) {
            existing.setSource(item.getSource());
        }

        scheduleEntryMapper.updateById(existing);
    }

    /**
     * 创建新的排班条目
     * 校验唯一约束：同一方案同一员工同一天只能有一条记录
     */
    private void createNewEntry(
            String planId,
            Long storeId,
            ScheduleEntryBatchUpdateDTO.EntryUpdateItem item,
            LocalDate workDate,
            Map<String, ScheduleShiftType> shiftTypeMap) {

        // 校验唯一约束：同一方案同一员工同一天只能有一条记录
        ScheduleEntry existing = scheduleEntryMapper.selectByEmployeeAndDate(item.getEmployeeId(), workDate);
        if (existing != null && planId.equals(existing.getPlanId())) {
            throw new RuntimeException("该员工当天已有排班记录");
        }

        ScheduleEntry entry = new ScheduleEntry();
        entry.setPlanId(planId);
        entry.setEmployeeId(item.getEmployeeId());
        entry.setWorkDate(workDate);
        entry.setShiftType(item.getShiftType());
        entry.setSource(item.getSource() != null ? item.getSource() : ScheduleEntry.SOURCE_MANUAL);

        // 从班次配置填充详细信息
        if (item.getShiftType() != null && shiftTypeMap.containsKey(item.getShiftType())) {
            ScheduleShiftType shiftType = shiftTypeMap.get(item.getShiftType());
            entry.setShiftName(shiftType.getShiftName());
            entry.setStartTime(shiftType.getStartTime());
            entry.setEndTime(shiftType.getEndTime());
        }

        // 设置默认排序顺序
        entry.setSortOrder(0);

        scheduleEntryMapper.insert(entry);
    }

    /**
     * 更新方案的统计字段
     * 包括employeeCount（涉及员工数）和totalWorkHours（总工时）
     *
     * @param planId 方案ID
     */
    private void updatePlanStatistics(String planId) {
        SchedulePlan plan = schedulePlanMapper.selectById(planId);
        if (plan == null) {
            return;
        }

        // 统计涉及员工数
        Long employeeCount = scheduleEntryMapper.countDistinctEmployeeByPlan(planId);
        plan.setEmployeeCount(employeeCount != null ? employeeCount.intValue() : 0);

        // 统计总工时（分钟）
        Integer totalWorkHours = scheduleEntryMapper.sumWorkHoursByPlan(planId);
        plan.setTotalWorkHours(totalWorkHours != null ? totalWorkHours : 0);

        schedulePlanMapper.updateById(plan);
    }
}
