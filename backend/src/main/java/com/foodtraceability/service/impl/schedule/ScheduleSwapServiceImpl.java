package com.foodtraceability.service.impl.schedule;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.schedule.SwapEntryInfoVO;
import com.foodtraceability.dto.schedule.SwapRequestCreateDTO;
import com.foodtraceability.dto.schedule.SwapRequestQueryDTO;
import com.foodtraceability.dto.schedule.SwapRequestVO;
import com.foodtraceability.entity.schedule.ScheduleEntry;
import com.foodtraceability.entity.schedule.SchedulePlan;
import com.foodtraceability.entity.schedule.SwapRequest;
import com.foodtraceability.mapper.schedule.ScheduleEntryMapper;
import com.foodtraceability.mapper.schedule.SchedulePlanMapper;
import com.foodtraceability.mapper.schedule.SwapRequestMapper;
import com.foodtraceability.service.schedule.ScheduleSwapService;
import com.foodtraceability.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 排班换班管理服务实现类
 * 实现换班申请的创建、审批、查询等业务逻辑
 *
 * <p>状态机：
 * <ul>
 *   <li>pending → approved（审批通过）</li>
 *   <li>pending → rejected（驳回）</li>
 *   <li>pending → cancelled（取消）</li>
 * </ul>
 */
@Service
public class ScheduleSwapServiceImpl implements ScheduleSwapService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleSwapServiceImpl.class);

    /** 日期格式化器 */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /** 时间格式化器 */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    /** 日期时间格式化器 */
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SwapRequestMapper swapRequestMapper;
    private final ScheduleEntryMapper scheduleEntryMapper;
    private final SchedulePlanMapper schedulePlanMapper;

    public ScheduleSwapServiceImpl(SwapRequestMapper swapRequestMapper,
                                   ScheduleEntryMapper scheduleEntryMapper,
                                   SchedulePlanMapper schedulePlanMapper) {
        this.swapRequestMapper = swapRequestMapper;
        this.scheduleEntryMapper = scheduleEntryMapper;
        this.schedulePlanMapper = schedulePlanMapper;
    }

    @Override
    public PageResult<SwapRequestVO> getSwapList(SwapRequestQueryDTO queryDTO) {
        Page<SwapRequest> page = new Page<>(
                queryDTO.getPage() != null ? queryDTO.getPage() : 1,
                queryDTO.getSize() != null ? queryDTO.getSize() : 10
        );

        IPage<SwapRequest> resultPage = swapRequestMapper.selectSwapPage(
                page, queryDTO.getStatus(), queryDTO.getPlanId());

        List<SwapRequestVO> voList = resultPage.getRecords().stream()
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
    public SwapRequestVO getSwapById(String id) {
        SwapRequest entity = swapRequestMapper.selectById(id);
        if (entity == null) {
            return null;
        }
        return convertToVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SwapRequestVO createSwap(SwapRequestCreateDTO createDTO) {
        // 校验方案存在
        SchedulePlan plan = schedulePlanMapper.selectById(createDTO.getPlanId());
        if (plan == null) {
            throw new RuntimeException("排班方案不存在：" + createDTO.getPlanId());
        }

        // 查询发起人的排班条目
        ScheduleEntry initiatorEntry = scheduleEntryMapper.selectById(createDTO.getInitiatorEntryId());
        if (initiatorEntry == null) {
            throw new RuntimeException("发起人的排班条目不存在");
        }

        // 查询目标员工的排班条目
        ScheduleEntry targetEntry = scheduleEntryMapper.selectById(createDTO.getTargetEntryId());
        if (targetEntry == null) {
            throw new RuntimeException("目标员工的排班条目不存在");
        }

        // 获取当前用户信息
        Long currentUserId = SecurityUtils.getCurrentUserId();
        String currentUsername = SecurityUtils.getCurrentUsername();

        // 构建实体
        SwapRequest entity = new SwapRequest();
        entity.setPlanId(createDTO.getPlanId());
        entity.setInitiatorId(currentUserId);
        entity.setInitiatorName(currentUsername);
        entity.setInitiatorEntryId(createDTO.getInitiatorEntryId());
        entity.setInitiatorWorkDate(initiatorEntry.getWorkDate());
        entity.setInitiatorShiftType(initiatorEntry.getShiftType());
        entity.setInitiatorShiftName(initiatorEntry.getShiftName());
        entity.setTargetEmployeeId(Long.parseLong(createDTO.getTargetEmployeeId()));
        entity.setTargetEmployeeName(targetEntry.getEmployeeName());
        entity.setTargetEntryId(createDTO.getTargetEntryId());
        entity.setTargetWorkDate(targetEntry.getWorkDate());
        entity.setTargetShiftType(targetEntry.getShiftType());
        entity.setTargetShiftName(targetEntry.getShiftName());
        entity.setStatus(SwapRequest.STATUS_PENDING);
        entity.setReason(createDTO.getReason());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        entity.setDeleted(0);

        swapRequestMapper.insert(entity);
        log.info("创建换班申请成功: requestId={}, planId={}", entity.getRequestId(), entity.getPlanId());

        return convertToVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SwapRequestVO approveSwap(String id, String comment) {
        SwapRequest entity = swapRequestMapper.selectById(id);
        if (entity == null) {
            throw new RuntimeException("换班请求不存在：" + id);
        }

        // 状态机校验：仅 pending 可审批通过
        if (!SwapRequest.STATUS_PENDING.equals(entity.getStatus())) {
            throw new RuntimeException("当前状态不允许审批通过：" + entity.getStatus());
        }

        // 更新审批信息
        entity.setStatus(SwapRequest.STATUS_APPROVED);
        entity.setApproverId(SecurityUtils.getCurrentUserId());
        entity.setApproverName(SecurityUtils.getCurrentUsername());
        entity.setApproveTime(LocalDateTime.now());
        entity.setApproveComment(comment);
        entity.setUpdateTime(LocalDateTime.now());

        swapRequestMapper.updateById(entity);
        log.info("审批通过换班申请: requestId={}, approver={}", id, entity.getApproverName());

        return convertToVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SwapRequestVO rejectSwap(String id, String reason) {
        SwapRequest entity = swapRequestMapper.selectById(id);
        if (entity == null) {
            throw new RuntimeException("换班请求不存在：" + id);
        }

        // 状态机校验：仅 pending 可驳回
        if (!SwapRequest.STATUS_PENDING.equals(entity.getStatus())) {
            throw new RuntimeException("当前状态不允许驳回：" + entity.getStatus());
        }

        entity.setStatus(SwapRequest.STATUS_REJECTED);
        entity.setApproverId(SecurityUtils.getCurrentUserId());
        entity.setApproverName(SecurityUtils.getCurrentUsername());
        entity.setApproveTime(LocalDateTime.now());
        entity.setRejectReason(reason);
        entity.setUpdateTime(LocalDateTime.now());

        swapRequestMapper.updateById(entity);
        log.info("驳回换班申请: requestId={}, reason={}", id, reason);

        return convertToVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SwapRequestVO cancelSwap(String id) {
        SwapRequest entity = swapRequestMapper.selectById(id);
        if (entity == null) {
            throw new RuntimeException("换班请求不存在：" + id);
        }

        // 状态机校验：仅 pending 可取消
        if (!SwapRequest.STATUS_PENDING.equals(entity.getStatus())) {
            throw new RuntimeException("当前状态不允许取消：" + entity.getStatus());
        }

        entity.setStatus(SwapRequest.STATUS_CANCELLED);
        entity.setUpdateTime(LocalDateTime.now());

        swapRequestMapper.updateById(entity);
        log.info("取消换班申请: requestId={}", id);

        return convertToVO(entity);
    }

    @Override
    public Integer getPendingCount() {
        return swapRequestMapper.countPending();
    }

    // ==================== 私有辅助方法 ====================

    /**
     * Entity → VO 转换（含发起人和目标人班次详情）
     * @param entity 换班请求实体
     * @return 视图对象
     */
    private SwapRequestVO convertToVO(SwapRequest entity) {
        if (entity == null) {
            return null;
        }

        SwapRequestVO vo = new SwapRequestVO();
        vo.setRequestId(entity.getRequestId());
        vo.setPlanId(entity.getPlanId());
        vo.setStatus(entity.getStatus());
        vo.setInitiatorId(entity.getInitiatorId() != null ? String.valueOf(entity.getInitiatorId()) : null);
        vo.setInitiatorName(entity.getInitiatorName());
        vo.setTargetEmployeeId(entity.getTargetEmployeeId() != null ? String.valueOf(entity.getTargetEmployeeId()) : null);
        vo.setTargetEmployeeName(entity.getTargetEmployeeName());
        vo.setReason(entity.getReason());
        vo.setApproverId(entity.getApproverId() != null ? String.valueOf(entity.getApproverId()) : null);
        vo.setApproverName(entity.getApproverName());
        vo.setApproveComment(entity.getApproveComment());

        // 时间格式化
        if (entity.getCreateTime() != null) {
            vo.setCreateTime(entity.getCreateTime().format(DATETIME_FORMATTER));
        }
        if (entity.getApproveTime() != null) {
            vo.setApproveTime(entity.getApproveTime().format(DATETIME_FORMATTER));
        }

        // 构建发起人班次详情
        vo.setInitiatorEntry(buildSwapEntryInfo(
                entity.getInitiatorEntryId(),
                entity.getInitiatorWorkDate(),
                entity.getInitiatorShiftType(),
                entity.getInitiatorShiftName()));

        // 构建目标员工班次详情
        vo.setTargetEntry(buildSwapEntryInfo(
                entity.getTargetEntryId(),
                entity.getTargetWorkDate(),
                entity.getTargetShiftType(),
                entity.getTargetShiftName()));

        // 填充方案名称（冗余字段）
        if (entity.getPlanId() != null) {
            SchedulePlan plan = schedulePlanMapper.selectById(entity.getPlanId());
            if (plan != null) {
                vo.setPlanName(plan.getPlanName());
            }
        }

        return vo;
    }

    /**
     * 构建换班条目信息VO
     * 通过排班条目ID查询完整班次信息（开始/结束时间）
     * @param entryId 排班条目ID
     * @param workDate 工作日期
     * @param shiftType 班次类型
     * @param shiftName 班次名称
     * @return 换班条目信息VO
     */
    private SwapEntryInfoVO buildSwapEntryInfo(String entryId, LocalDate workDate,
                                                String shiftType, String shiftName) {
        SwapEntryInfoVO vo = new SwapEntryInfoVO();
        vo.setEntryId(entryId);
        vo.setShiftType(shiftType);
        vo.setShiftName(shiftName);

        if (workDate != null) {
            vo.setDate(workDate.format(DATE_FORMATTER));
        }

        // 通过排班条目ID查询完整的班次时间信息
        if (entryId != null) {
            ScheduleEntry entry = scheduleEntryMapper.selectById(entryId);
            if (entry != null) {
                if (entry.getStartTime() != null) {
                    vo.setStartTime(entry.getStartTime().format(TIME_FORMATTER));
                }
                if (entry.getEndTime() != null) {
                    vo.setEndTime(entry.getEndTime().format(TIME_FORMATTER));
                }
            }
        }

        return vo;
    }
}
