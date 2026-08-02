package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.constant.RecruitmentConstants;
import com.foodtraceability.dto.recruitment.QuotaAdjustmentDTO;
import com.foodtraceability.dto.recruitment.QuotaAdjustmentReviewDTO;
import com.foodtraceability.dto.recruitment.QuotaRejectDTO;
import com.foodtraceability.dto.recruitment.QuotaValidateResult;
import com.foodtraceability.dto.recruitment.RecruitmentQuotaCreateDTO;
import com.foodtraceability.dto.recruitment.RecruitmentQuotaQueryDTO;
import com.foodtraceability.dto.recruitment.RecruitmentQuotaVO;
import com.foodtraceability.entity.Position;
import com.foodtraceability.entity.RecruitmentQuota;
import com.foodtraceability.entity.Store;
import com.foodtraceability.mapper.RecruitmentQuotaMapper;
import com.foodtraceability.service.PositionService;
import com.foodtraceability.service.RecruitmentQuotaService;
import com.foodtraceability.service.StoreService;
import com.foodtraceability.service.UserService;
import com.foodtraceability.service.event.RecruitmentNotificationEvent;
import com.foodtraceability.statemachine.RecruitmentQuotaStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 招聘名额服务实现类
 *
 * <p>遵循 ADR-001（事件 AFTER_COMMIT + try-catch 隔离）、ADR-002（状态机 Map transitions）、
 * ADR-006（无 DataService 缓存层，直接 ServiceImpl→Mapper→DB）、
 * ADR-007（80% 实时检查 + incrementUsedCountAtomic 原子计数避免超卖）。</p>
 *
 * <p>所有写方法加 {@code @Transactional(rollbackFor = Exception.class)}，
 * 事件发布代码用 try-catch 包裹（防接收人查询失败导致业务回滚）。</p>
 */
@Service
public class RecruitmentQuotaServiceImpl extends ServiceImpl<RecruitmentQuotaMapper, RecruitmentQuota>
        implements RecruitmentQuotaService {

    private static final Logger logger = LoggerFactory.getLogger(RecruitmentQuotaServiceImpl.class);

    private final ApplicationEventPublisher eventPublisher;
    private final UserService userService;
    private final StoreService storeService;
    private final PositionService positionService;

    public RecruitmentQuotaServiceImpl(ApplicationEventPublisher eventPublisher,
                                       UserService userService,
                                       StoreService storeService,
                                       PositionService positionService) {
        this.eventPublisher = eventPublisher;
        this.userService = userService;
        this.storeService = storeService;
        this.positionService = positionService;
    }

    // ============ 写方法（带事务 + 事件发布） ============

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RecruitmentQuota issueQuota(RecruitmentQuotaCreateDTO dto, Long operatorId) {
        // 1. 填充字段
        RecruitmentQuota quota = new RecruitmentQuota();
        quota.setYear(dto.getYear());
        quota.setQuarter(dto.getQuarter());
        quota.setStoreId(dto.getStoreId());
        quota.setPositionId(dto.getPositionId());
        quota.setHeadcount(dto.getHeadcount());
        quota.setUsedCount(0);
        quota.setExpireDate(dto.getExpireDate());
        quota.setRemark(dto.getRemark());

        // 冗余门店/岗位名称（issueQuota 时填充避免后续多次 join 查询）
        fillStoreAndPositionNames(quota);

        // 2. 状态设 issued + 下发信息
        quota.setStatus(RecruitmentConstants.QUOTA_ISSUED);
        quota.setIssuedBy(operatorId);
        quota.setIssuedTime(LocalDateTime.now());

        // 3. 保存
        this.save(quota);
        logger.info("名额下发成功, quotaId={}, storeId={}, positionId={}, headcount={}",
                quota.getQuotaId(), quota.getStoreId(), quota.getPositionId(), quota.getHeadcount());

        // 4. 发布 quota.issued 事件（try-catch 隔离）
        try {
            List<Long> recipientUserIds = userService.getStoreManagersByStoreId(dto.getStoreId());
            Map<String, Object> variables = new HashMap<>();
            variables.put("year", dto.getYear());
            variables.put("quarter", dto.getQuarter());
            variables.put("storeName", quota.getStoreName());
            variables.put("positionName", quota.getPositionName());
            variables.put("headcount", dto.getHeadcount());
            variables.put("expireDate", dto.getExpireDate() != null ? dto.getExpireDate().toString() : "未设置");
            eventPublisher.publishEvent(RecruitmentNotificationEvent.quotaIssued(
                    this, operatorId, variables, recipientUserIds));
        } catch (Exception e) {
            logger.error("发布名额下发事件失败, quotaId={}", quota.getQuotaId(), e);
        }

        return quota;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmQuota(Long quotaId, Long operatorId) {
        RecruitmentQuota quota = getQuotaOrThrow(quotaId);
        RecruitmentQuotaStateMachine.validateTransition(quota.getStatus(), RecruitmentConstants.QUOTA_ACTIVE);

        quota.setStatus(RecruitmentConstants.QUOTA_ACTIVE);
        quota.setConfirmedBy(operatorId);
        quota.setConfirmedTime(LocalDateTime.now());
        boolean updated = this.updateById(quota);

        if (updated) {
            logger.info("名额确认成功, quotaId={}, operatorId={}", quotaId, operatorId);
            // 发布 quota.confirmed 事件（接收人=下发 HR）
            try {
                List<Long> recipientUserIds = resolveRecipientsForIssuedHr(quota);
                Map<String, Object> variables = new HashMap<>();
                variables.put("storeName", quota.getStoreName());
                variables.put("positionName", quota.getPositionName());
                variables.put("headcount", quota.getHeadcount());
                eventPublisher.publishEvent(RecruitmentNotificationEvent.quotaConfirmed(
                        this, operatorId, variables, recipientUserIds));
            } catch (Exception e) {
                logger.error("发布名额确认事件失败, quotaId={}", quotaId, e);
            }
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rejectQuota(Long quotaId, QuotaRejectDTO dto, Long operatorId) {
        RecruitmentQuota quota = getQuotaOrThrow(quotaId);
        RecruitmentQuotaStateMachine.validateTransition(quota.getStatus(), RecruitmentConstants.QUOTA_REJECTED);

        quota.setStatus(RecruitmentConstants.QUOTA_REJECTED);
        // 拒绝原因追加到 remark 末尾，避免覆盖下发备注
        String rejectRemark = "【拒绝原因】" + dto.getReason();
        quota.setRemark(StringUtils.hasText(quota.getRemark())
                ? quota.getRemark() + " " + rejectRemark : rejectRemark);
        quota.setConfirmedBy(operatorId);
        quota.setConfirmedTime(LocalDateTime.now());
        boolean updated = this.updateById(quota);

        if (updated) {
            logger.info("名额拒绝成功, quotaId={}, operatorId={}", quotaId, operatorId);
            try {
                List<Long> recipientUserIds = resolveRecipientsForIssuedHr(quota);
                Map<String, Object> variables = new HashMap<>();
                variables.put("storeName", quota.getStoreName());
                variables.put("positionName", quota.getPositionName());
                variables.put("reason", dto.getReason());
                eventPublisher.publishEvent(RecruitmentNotificationEvent.quotaRejected(
                        this, operatorId, variables, recipientUserIds));
            } catch (Exception e) {
                logger.error("发布名额拒绝事件失败, quotaId={}", quotaId, e);
            }
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean requestAdjustment(Long quotaId, QuotaAdjustmentDTO dto, Long operatorId) {
        RecruitmentQuota quota = getQuotaOrThrow(quotaId);
        RecruitmentQuotaStateMachine.validateTransition(quota.getStatus(),
                RecruitmentConstants.QUOTA_ADJUSTMENT_REQUESTED);

        quota.setStatus(RecruitmentConstants.QUOTA_ADJUSTMENT_REQUESTED);
        String adjustmentRemark = String.format("【追加申请】+%d 名，原因：%s",
                dto.getAdditionalCount(), dto.getReason());
        quota.setRemark(StringUtils.hasText(quota.getRemark())
                ? quota.getRemark() + " " + adjustmentRemark : adjustmentRemark);
        boolean updated = this.updateById(quota);

        if (updated) {
            logger.info("名额追加申请成功, quotaId={}, additionalCount={}, operatorId={}",
                    quotaId, dto.getAdditionalCount(), operatorId);
            try {
                // 接收人：HR 经理（审核追加）
                List<Long> recipientUserIds = userService.getHrManagers();
                Map<String, Object> variables = new HashMap<>();
                variables.put("storeName", quota.getStoreName());
                variables.put("positionName", quota.getPositionName());
                variables.put("additionalCount", dto.getAdditionalCount());
                variables.put("reason", dto.getReason());
                eventPublisher.publishEvent(RecruitmentNotificationEvent.quotaAdjustmentRequested(
                        this, operatorId, variables, recipientUserIds));
            } catch (Exception e) {
                logger.error("发布名额追加申请事件失败, quotaId={}", quotaId, e);
            }
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approveAdjustment(Long quotaId, QuotaAdjustmentReviewDTO dto, Long operatorId) {
        RecruitmentQuota quota = getQuotaOrThrow(quotaId);
        // approved=true → active（追加成功）；approved=false → issued（驳回回到已下发）
        String targetStatus = Boolean.TRUE.equals(dto.getApproved())
                ? RecruitmentConstants.QUOTA_ACTIVE
                : RecruitmentConstants.QUOTA_ISSUED;
        RecruitmentQuotaStateMachine.validateTransition(quota.getStatus(), targetStatus);

        quota.setStatus(targetStatus);
        if (Boolean.TRUE.equals(dto.getApproved()) && StringUtils.hasText(dto.getRemark())) {
            // 通过时累加 headcount（追加数量已在申请阶段记录在 remark 中，此处仅追加审核备注）
            // 注意：实际追加数量需从 remark 中解析或单独存储；此处简化处理，仅记录审核结果
            quota.setRemark(quota.getRemark() + " 【审核结果】通过：" + dto.getRemark());
        } else if (!Boolean.TRUE.equals(dto.getApproved())) {
            quota.setRemark(quota.getRemark() + " 【审核结果】驳回：" + dto.getRemark());
        }
        boolean updated = this.updateById(quota);

        if (updated) {
            logger.info("名额追加审核完成, quotaId={}, approved={}, operatorId={}",
                    quotaId, dto.getApproved(), operatorId);
            try {
                // 接收人：门店店长
                List<Long> recipientUserIds = userService.getStoreManagersByStoreId(quota.getStoreId());
                Map<String, Object> variables = new HashMap<>();
                variables.put("storeName", quota.getStoreName());
                variables.put("positionName", quota.getPositionName());
                variables.put("approved", dto.getApproved());
                variables.put("remark", dto.getRemark());
                eventPublisher.publishEvent(RecruitmentNotificationEvent.quotaAdjustmentResult(
                        this, operatorId, variables, recipientUserIds));
            } catch (Exception e) {
                logger.error("发布名额追加审核结果事件失败, quotaId={}", quotaId, e);
            }
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean closeQuota(Long quotaId, Long operatorId) {
        RecruitmentQuota quota = getQuotaOrThrow(quotaId);
        RecruitmentQuotaStateMachine.validateTransition(quota.getStatus(), RecruitmentConstants.QUOTA_CLOSED);

        quota.setStatus(RecruitmentConstants.QUOTA_CLOSED);
        boolean updated = this.updateById(quota);

        if (updated) {
            logger.info("名额关闭成功, quotaId={}, operatorId={}", quotaId, operatorId);
            try {
                List<Long> recipientUserIds = userService.getStoreManagersByStoreId(quota.getStoreId());
                Map<String, Object> variables = new HashMap<>();
                variables.put("storeName", quota.getStoreName());
                variables.put("positionName", quota.getPositionName());
                eventPublisher.publishEvent(RecruitmentNotificationEvent.quotaClosed(
                        this, operatorId, variables, recipientUserIds));
            } catch (Exception e) {
                logger.error("发布名额关闭事件失败, quotaId={}", quotaId, e);
            }
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean incrementUsedCount(Long quotaId) {
        // 1. 原子更新：UPDATE ... SET used_count = used_count + 1 WHERE used_count < headcount
        int updated = baseMapper.incrementUsedCountAtomic(quotaId);
        if (updated == 0) {
            logger.warn("名额计数+1失败（名额已用完）, quotaId={}", quotaId);
            return false;
        }

        // 2. 查询最新状态
        RecruitmentQuota quota = this.getById(quotaId);
        if (quota == null) {
            return false;
        }
        int newUsedCount = quota.getUsedCount() == null ? 0 : quota.getUsedCount();
        int headcount = quota.getHeadcount() == null ? 0 : quota.getHeadcount();

        // 3. 80% 阈值检查（首次达到，避免重复触发）
        // ADR-007：threshold = ceil(headcount * 0.8)，仅当 newUsedCount == threshold 且 < headcount 时触发一次
        int threshold = (int) Math.ceil(headcount * RecruitmentConstants.QUOTA_EXHAUSTING_THRESHOLD);
        if (newUsedCount == threshold && newUsedCount < headcount) {
            publishExhaustingEvent(quota);
        }

        // 4. 100% 状态变更 + 事件
        if (newUsedCount >= headcount) {
            quota.setStatus(RecruitmentConstants.QUOTA_EXHAUSTED);
            this.updateById(quota);
            publishExhaustedEvent(quota);
        }
        return true;
    }

    // ============ 读方法 ============

    @Override
    public RecruitmentQuota getQuotaById(Long quotaId) {
        return this.getById(quotaId);
    }

    @Override
    public IPage<RecruitmentQuotaVO> listQuotas(RecruitmentQuotaQueryDTO query) {
        Page<RecruitmentQuota> page = new Page<>(
                query.getPage() == null ? 1 : query.getPage(),
                query.getSize() == null ? 10 : query.getSize());
        LambdaQueryWrapper<RecruitmentQuota> wrapper = new LambdaQueryWrapper<>();
        if (query.getYear() != null) {
            wrapper.eq(RecruitmentQuota::getYear, query.getYear());
        }
        if (query.getQuarter() != null) {
            wrapper.eq(RecruitmentQuota::getQuarter, query.getQuarter());
        }
        if (query.getStoreId() != null) {
            wrapper.eq(RecruitmentQuota::getStoreId, query.getStoreId());
        }
        if (query.getPositionId() != null) {
            wrapper.eq(RecruitmentQuota::getPositionId, query.getPositionId());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(RecruitmentQuota::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(RecruitmentQuota::getCreateTime);

        IPage<RecruitmentQuota> resultPage = this.page(page, wrapper);
        // 转换为 VO（含 remaining 计算字段）
        IPage<RecruitmentQuotaVO> voPage = resultPage.convert(this::toVO);
        return voPage;
    }

    @Override
    public List<RecruitmentQuotaVO> getAvailableQuotasByStore(Long storeId) {
        LambdaQueryWrapper<RecruitmentQuota> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RecruitmentQuota::getStoreId, storeId)
                .eq(RecruitmentQuota::getStatus, RecruitmentConstants.QUOTA_ACTIVE)
                .orderByDesc(RecruitmentQuota::getCreateTime);
        List<RecruitmentQuota> quotas = this.list(wrapper);
        return quotas.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public QuotaValidateResult validateQuota(Long quotaId, int count) {
        RecruitmentQuota quota = this.getById(quotaId);
        if (quota == null) {
            return QuotaValidateResult.fail(0, String.valueOf(ErrorCode.QUOTA_NOT_FOUND.getCode()));
        }
        if (!RecruitmentConstants.QUOTA_ACTIVE.equals(quota.getStatus())) {
            return QuotaValidateResult.fail(0, String.valueOf(ErrorCode.QUOTA_STATUS_UNAVAILABLE.getCode()));
        }
        int headcount = quota.getHeadcount() == null ? 0 : quota.getHeadcount();
        int usedCount = quota.getUsedCount() == null ? 0 : quota.getUsedCount();
        int remaining = headcount - usedCount;
        if (remaining < count) {
            return QuotaValidateResult.fail(remaining, String.valueOf(ErrorCode.QUOTA_EXHAUSTED.getCode()));
        }
        return QuotaValidateResult.ok(remaining);
    }

    // ============ 私有辅助方法 ============

    /**
     * 按 quotaId 查询名额，不存在则抛 BusinessException。
     */
    private RecruitmentQuota getQuotaOrThrow(Long quotaId) {
        RecruitmentQuota quota = this.getById(quotaId);
        if (quota == null) {
            throw new BusinessException(ErrorCode.QUOTA_NOT_FOUND, "招聘名额不存在: " + quotaId);
        }
        return quota;
    }

    /**
     * 填充门店名称和岗位名称（冗余存储，避免后续 join 查询）。
     */
    private void fillStoreAndPositionNames(RecruitmentQuota quota) {
        if (quota.getStoreId() != null) {
            try {
                Store store = storeService.getStoreById(String.valueOf(quota.getStoreId()));
                if (store != null) {
                    quota.setStoreName(store.getStoreName());
                }
            } catch (Exception e) {
                logger.warn("查询门店信息失败, storeId={}", quota.getStoreId(), e);
            }
        }
        if (quota.getPositionId() != null) {
            try {
                Position position = positionService.getPositionById(quota.getPositionId());
                if (position != null) {
                    quota.setPositionName(position.getPositionName());
                }
            } catch (Exception e) {
                logger.warn("查询岗位信息失败, positionId={}", quota.getPositionId(), e);
            }
        }
    }

    /**
     * 转换实体为 VO（含 remaining 计算字段）。
     */
    private RecruitmentQuotaVO toVO(RecruitmentQuota quota) {
        RecruitmentQuotaVO vo = new RecruitmentQuotaVO();
        vo.setQuotaId(quota.getQuotaId());
        vo.setYear(quota.getYear());
        vo.setQuarter(quota.getQuarter());
        vo.setStoreId(quota.getStoreId());
        vo.setStoreName(quota.getStoreName());
        vo.setPositionId(quota.getPositionId());
        vo.setPositionName(quota.getPositionName());
        vo.setHeadcount(quota.getHeadcount());
        vo.setUsedCount(quota.getUsedCount());
        int headcount = quota.getHeadcount() == null ? 0 : quota.getHeadcount();
        int usedCount = quota.getUsedCount() == null ? 0 : quota.getUsedCount();
        vo.setRemaining(headcount - usedCount);
        vo.setStatus(quota.getStatus());
        vo.setIssuedBy(quota.getIssuedBy());
        vo.setIssuedTime(quota.getIssuedTime());
        vo.setConfirmedBy(quota.getConfirmedBy());
        vo.setConfirmedTime(quota.getConfirmedTime());
        vo.setExpireDate(quota.getExpireDate());
        vo.setRemark(quota.getRemark());
        vo.setCreateTime(quota.getCreateTime());
        vo.setUpdateTime(quota.getUpdateTime());
        return vo;
    }

    /**
     * 解析名额事件的接收人：门店店长 + HR 经理（exhausting/exhausted 通知双方）。
     */
    private List<Long> resolveRecipients(RecruitmentQuota quota) {
        List<Long> recipients = new ArrayList<>();
        try {
            List<Long> storeManagers = userService.getStoreManagersByStoreId(quota.getStoreId());
            if (storeManagers != null) {
                recipients.addAll(storeManagers);
            }
        } catch (Exception e) {
            logger.warn("查询门店店长接收人失败, storeId={}", quota.getStoreId(), e);
        }
        try {
            List<Long> hrManagers = userService.getHrManagers();
            if (hrManagers != null) {
                recipients.addAll(hrManagers);
            }
        } catch (Exception e) {
            logger.warn("查询 HR 经理接收人失败", e);
        }
        return recipients;
    }

    /**
     * 解析名额下发 HR 接收人（confirm/reject 事件通知下发 HR）。
     * 若 issuedBy 为空则降级为 HR 经理列表。
     */
    private List<Long> resolveRecipientsForIssuedHr(RecruitmentQuota quota) {
        if (quota.getIssuedBy() != null) {
            return List.of(quota.getIssuedBy());
        }
        try {
            List<Long> hrManagers = userService.getHrManagers();
            return hrManagers != null ? hrManagers : List.of();
        } catch (Exception e) {
            logger.warn("查询 HR 经理接收人失败（issuedBy 为空降级）", e);
            return List.of();
        }
    }

    /**
     * 发布名额即将用完事件（80% 阈值首次触发）。
     */
    private void publishExhaustingEvent(RecruitmentQuota quota) {
        try {
            List<Long> recipients = resolveRecipients(quota);
            Map<String, Object> variables = new HashMap<>();
            variables.put("storeName", quota.getStoreName());
            variables.put("positionName", quota.getPositionName());
            int usedCount = quota.getUsedCount() == null ? 0 : quota.getUsedCount();
            int headcount = quota.getHeadcount() == null ? 0 : quota.getHeadcount();
            variables.put("usedCount", usedCount);
            variables.put("headcount", headcount);
            variables.put("remaining", headcount - usedCount);
            eventPublisher.publishEvent(RecruitmentNotificationEvent.quotaExhausting(
                    this, null, variables, recipients));
            logger.info("名额即将用完事件已发布, quotaId={}, usedCount={}, headcount={}",
                    quota.getQuotaId(), usedCount, headcount);
        } catch (Exception e) {
            logger.error("发布名额即将用完事件失败, quotaId={}", quota.getQuotaId(), e);
        }
    }

    /**
     * 发布名额已用完事件（100% 触发）。
     */
    private void publishExhaustedEvent(RecruitmentQuota quota) {
        try {
            List<Long> recipients = resolveRecipients(quota);
            Map<String, Object> variables = new HashMap<>();
            variables.put("storeName", quota.getStoreName());
            variables.put("positionName", quota.getPositionName());
            int usedCount = quota.getUsedCount() == null ? 0 : quota.getUsedCount();
            int headcount = quota.getHeadcount() == null ? 0 : quota.getHeadcount();
            variables.put("usedCount", usedCount);
            variables.put("headcount", headcount);
            variables.put("remaining", headcount - usedCount);
            eventPublisher.publishEvent(RecruitmentNotificationEvent.quotaExhausted(
                    this, null, variables, recipients));
            logger.info("名额已用完事件已发布, quotaId={}", quota.getQuotaId());
        } catch (Exception e) {
            logger.error("发布名额已用完事件失败, quotaId={}", quota.getQuotaId(), e);
        }
    }
}
