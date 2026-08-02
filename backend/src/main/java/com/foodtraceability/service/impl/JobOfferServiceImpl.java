package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.constant.RecruitmentConstants;
import com.foodtraceability.dto.recruitment.JobOfferCreateDTO;
import com.foodtraceability.dto.recruitment.JobOfferQueryDTO;
import com.foodtraceability.dto.recruitment.JobOfferVO;
import com.foodtraceability.dto.recruitment.OfferRejectDTO;
import com.foodtraceability.entity.JobOffer;
import com.foodtraceability.entity.OnboardingRecord;
import com.foodtraceability.mapper.JobOfferMapper;
import com.foodtraceability.service.JobOfferService;
import com.foodtraceability.service.OnboardingRecordService;
import com.foodtraceability.service.UserService;
import com.foodtraceability.service.event.RecruitmentNotificationEvent;
import com.foodtraceability.statemachine.JobOfferStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 录用 Offer 服务实现类
 *
 * <p>遵循 ADR-001（事件 AFTER_COMMIT + try-catch 隔离）、
 * plan.md 11.3 方案 A（候选人无账号时传 HR userId + variables 携带 candidateEmail）。</p>
 *
 * <p>所有写方法加 {@code @Transactional(rollbackFor = Exception.class)}，
 * 事件发布代码用 try-catch 包裹。convertToOnboarding 事务边界：Offer 状态更新 + OnboardingRecord 创建原子操作。</p>
 */
@Service
public class JobOfferServiceImpl extends ServiceImpl<JobOfferMapper, JobOffer>
        implements JobOfferService {

    private static final Logger logger = LoggerFactory.getLogger(JobOfferServiceImpl.class);

    private final ApplicationEventPublisher eventPublisher;
    private final UserService userService;
    private final OnboardingRecordService onboardingRecordService;

    public JobOfferServiceImpl(ApplicationEventPublisher eventPublisher,
                               UserService userService,
                               OnboardingRecordService onboardingRecordService) {
        this.eventPublisher = eventPublisher;
        this.userService = userService;
        this.onboardingRecordService = onboardingRecordService;
    }

    // ============ 写方法（带事务 + 事件发布） ============

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JobOffer createOffer(JobOfferCreateDTO dto, Long operatorId) {
        JobOffer offer = new JobOffer();
        offer.setRequirementId(dto.getRequirementId());
        offer.setResumeId(dto.getResumeId());
        offer.setInterviewId(dto.getInterviewId());
        offer.setCandidateName(dto.getCandidateName());
        offer.setCandidateEmail(dto.getCandidateEmail());
        offer.setPositionId(dto.getPositionId());
        offer.setPositionName(dto.getPositionName());
        offer.setStoreId(dto.getStoreId());
        offer.setStoreName(dto.getStoreName());
        offer.setProposedSalary(dto.getProposedSalary());
        offer.setProbationMonths(dto.getProbationMonths());
        offer.setProbationSalary(dto.getProbationSalary());
        offer.setEntryDate(dto.getEntryDate());
        offer.setRemark(dto.getRemark());
        // 初始状态 pending（不发布事件，发送时才触发）
        offer.setStatus(RecruitmentConstants.OFFER_PENDING);
        this.save(offer);
        logger.info("Offer 创建成功, offerId={}, candidateName={}, operatorId={}",
                offer.getOfferId(), dto.getCandidateName(), operatorId);
        return offer;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean sendOffer(Long offerId, Long operatorId) {
        JobOffer offer = getOfferOrThrow(offerId);
        JobOfferStateMachine.validateTransition(offer.getStatus(), RecruitmentConstants.OFFER_SENT);

        offer.setStatus(RecruitmentConstants.OFFER_SENT);
        offer.setSendTime(LocalDateTime.now());
        boolean updated = this.updateById(offer);

        if (updated) {
            logger.info("Offer 发送成功, offerId={}, operatorId={}", offerId, operatorId);
            // 发布 offer.sent 事件
            // plan.md 11.3 方案 A：候选人无系统账号时，传 HR userId + variables 携带 candidateEmail
            try {
                List<Long> recipientUserIds = resolveCandidateRecipients(offer, operatorId);
                Map<String, Object> variables = new HashMap<>();
                variables.put("candidateName", offer.getCandidateName());
                variables.put("candidateEmail", offer.getCandidateEmail());
                variables.put("positionName", offer.getPositionName());
                variables.put("storeName", offer.getStoreName());
                variables.put("proposedSalary", offer.getProposedSalary());
                variables.put("entryDate", offer.getEntryDate() != null
                        ? offer.getEntryDate().toString() : "未设置");
                eventPublisher.publishEvent(RecruitmentNotificationEvent.offerSent(
                        this, operatorId, variables, recipientUserIds));
            } catch (Exception e) {
                logger.error("发布 Offer 发送事件失败, offerId={}", offerId, e);
            }
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean acceptOffer(Long offerId, Long operatorId) {
        JobOffer offer = getOfferOrThrow(offerId);
        JobOfferStateMachine.validateTransition(offer.getStatus(), RecruitmentConstants.OFFER_ACCEPTED);

        offer.setStatus(RecruitmentConstants.OFFER_ACCEPTED);
        offer.setAcceptTime(LocalDateTime.now());
        boolean updated = this.updateById(offer);

        if (updated) {
            logger.info("Offer 接受成功, offerId={}, operatorId={}", offerId, operatorId);
            // 发布 offer.accepted 事件（接收人=HR 经理 + HR 招聘员）
            try {
                List<Long> recipientUserIds = resolveHrRecipients();
                Map<String, Object> variables = new HashMap<>();
                variables.put("candidateName", offer.getCandidateName());
                variables.put("positionName", offer.getPositionName());
                variables.put("storeName", offer.getStoreName());
                variables.put("entryDate", offer.getEntryDate() != null
                        ? offer.getEntryDate().toString() : "未设置");
                eventPublisher.publishEvent(RecruitmentNotificationEvent.offerAccepted(
                        this, operatorId, variables, recipientUserIds));
            } catch (Exception e) {
                logger.error("发布 Offer 接受事件失败, offerId={}", offerId, e);
            }
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rejectOffer(Long offerId, OfferRejectDTO dto, Long operatorId) {
        JobOffer offer = getOfferOrThrow(offerId);
        JobOfferStateMachine.validateTransition(offer.getStatus(), RecruitmentConstants.OFFER_REJECTED);

        offer.setStatus(RecruitmentConstants.OFFER_REJECTED);
        offer.setRejectTime(LocalDateTime.now());
        if (dto != null && StringUtils.hasText(dto.getRejectReason())) {
            offer.setRejectReason(dto.getRejectReason());
        }
        boolean updated = this.updateById(offer);

        if (updated) {
            logger.info("Offer 拒绝成功, offerId={}, operatorId={}", offerId, operatorId);
            // 发布 offer.rejected 事件（接收人=HR 经理 + HR 招聘员）
            try {
                List<Long> recipientUserIds = resolveHrRecipients();
                Map<String, Object> variables = new HashMap<>();
                variables.put("candidateName", offer.getCandidateName());
                variables.put("positionName", offer.getPositionName());
                variables.put("rejectReason", offer.getRejectReason());
                eventPublisher.publishEvent(RecruitmentNotificationEvent.offerRejected(
                        this, operatorId, variables, recipientUserIds));
            } catch (Exception e) {
                logger.error("发布 Offer 拒绝事件失败, offerId={}", offerId, e);
            }
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean withdrawOffer(Long offerId, Long operatorId) {
        JobOffer offer = getOfferOrThrow(offerId);
        // sent 或 pending 均可撤回
        JobOfferStateMachine.validateTransition(offer.getStatus(), RecruitmentConstants.OFFER_WITHDRAWN);

        offer.setStatus(RecruitmentConstants.OFFER_WITHDRAWN);
        boolean updated = this.updateById(offer);

        if (updated) {
            logger.info("Offer 撤回成功, offerId={}, operatorId={}", offerId, operatorId);
            // 发布 offer.withdrawn 事件（接收人=候选人邮箱，方案 A）
            try {
                List<Long> recipientUserIds = resolveCandidateRecipients(offer, operatorId);
                Map<String, Object> variables = new HashMap<>();
                variables.put("candidateName", offer.getCandidateName());
                variables.put("candidateEmail", offer.getCandidateEmail());
                variables.put("positionName", offer.getPositionName());
                eventPublisher.publishEvent(RecruitmentNotificationEvent.offerWithdrawn(
                        this, operatorId, variables, recipientUserIds));
            } catch (Exception e) {
                logger.error("发布 Offer 撤回事件失败, offerId={}", offerId, e);
            }
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OnboardingRecord convertToOnboarding(Long offerId, Long operatorId) {
        JobOffer offer = getOfferOrThrow(offerId);
        JobOfferStateMachine.validateTransition(offer.getStatus(), RecruitmentConstants.OFFER_ONBOARDED);

        // 1. 同事务创建 OnboardingRecord（含岗位/薪资/试用期/门店信息）
        OnboardingRecord record = buildOnboardingRecordFromOffer(offer, operatorId);
        OnboardingRecord savedRecord = onboardingRecordService.createOnboardingRecord(record);
        logger.info("入职记录创建成功, onboardingId={}, offerId={}, operatorId={}",
                savedRecord.getId(), offerId, operatorId);

        // 2. 更新 Offer 状态为 onboarded
        offer.setStatus(RecruitmentConstants.OFFER_ONBOARDED);
        this.updateById(offer);
        logger.info("Offer 转入职成功, offerId={}, onboardingId={}", offerId, savedRecord.getId());

        // 不发布 onboarding.completed 事件（留 Sprint 3）
        return savedRecord;
    }

    // ============ 读方法 ============

    @Override
    public JobOffer getOfferById(Long offerId) {
        return this.getById(offerId);
    }

    @Override
    public IPage<JobOfferVO> listOffers(JobOfferQueryDTO query) {
        Page<JobOffer> page = new Page<>(
                query.getPage() == null ? 1 : query.getPage(),
                query.getSize() == null ? 10 : query.getSize());
        LambdaQueryWrapper<JobOffer> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getRequirementId())) {
            wrapper.eq(JobOffer::getRequirementId, query.getRequirementId());
        }
        if (StringUtils.hasText(query.getCandidateName())) {
            wrapper.like(JobOffer::getCandidateName, query.getCandidateName());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(JobOffer::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(JobOffer::getCreateTime);

        IPage<JobOffer> resultPage = this.page(page, wrapper);
        return resultPage.convert(this::toVO);
    }

    // ============ 私有辅助方法 ============

    /**
     * 按 offerId 查询 Offer，不存在则抛 BusinessException。
     */
    private JobOffer getOfferOrThrow(Long offerId) {
        JobOffer offer = this.getById(offerId);
        if (offer == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Offer 不存在: " + offerId);
        }
        return offer;
    }

    /**
     * 转换实体为 VO。
     */
    private JobOfferVO toVO(JobOffer offer) {
        JobOfferVO vo = new JobOfferVO();
        vo.setOfferId(offer.getOfferId());
        vo.setRequirementId(offer.getRequirementId());
        vo.setResumeId(offer.getResumeId());
        vo.setInterviewId(offer.getInterviewId());
        vo.setCandidateName(offer.getCandidateName());
        vo.setCandidateEmail(offer.getCandidateEmail());
        vo.setPositionId(offer.getPositionId());
        vo.setPositionName(offer.getPositionName());
        vo.setStoreId(offer.getStoreId());
        vo.setStoreName(offer.getStoreName());
        vo.setProposedSalary(offer.getProposedSalary());
        vo.setProbationMonths(offer.getProbationMonths());
        vo.setProbationSalary(offer.getProbationSalary());
        vo.setStatus(offer.getStatus());
        vo.setSendTime(offer.getSendTime());
        vo.setAcceptTime(offer.getAcceptTime());
        vo.setRejectTime(offer.getRejectTime());
        vo.setRejectReason(offer.getRejectReason());
        vo.setEntryDate(offer.getEntryDate());
        vo.setRemark(offer.getRemark());
        vo.setCreateTime(offer.getCreateTime());
        vo.setUpdateTime(offer.getUpdateTime());
        return vo;
    }

    /**
     * 解析 Offer 事件的候选人接收人。
     *
     * <p>plan.md 11.3 方案 A：候选人无系统账号时，传 HR 操作人 userId 作为占位接收人，
     * 在 variables 中携带 candidateEmail 供模板渲染。当前简化处理：直接用 HR 操作人 ID。</p>
     */
    private List<Long> resolveCandidateRecipients(JobOffer offer, Long operatorId) {
        // 简化方案：候选人无系统账号，使用 HR 操作人 ID 作为占位接收人
        // variables 中已携带 candidateEmail 供模板渲染
        if (operatorId != null) {
            return List.of(operatorId);
        }
        // operatorId 为空时降级为 HR 经理列表
        try {
            List<Long> hrManagers = userService.getHrManagers();
            return hrManagers != null ? hrManagers : List.of();
        } catch (Exception e) {
            logger.warn("查询 HR 经理接收人失败（operatorId 为空降级）", e);
            return List.of();
        }
    }

    /**
     * 解析 HR 接收人（HR 经理 + HR 招聘员）。
     */
    private List<Long> resolveHrRecipients() {
        java.util.List<Long> recipients = new java.util.ArrayList<>();
        try {
            List<Long> hrManagers = userService.getHrManagers();
            if (hrManagers != null) {
                recipients.addAll(hrManagers);
            }
        } catch (Exception e) {
            logger.warn("查询 HR 经理接收人失败", e);
        }
        try {
            List<Long> hrRecruiters = userService.getHrRecruiters();
            if (hrRecruiters != null) {
                recipients.addAll(hrRecruiters);
            }
        } catch (Exception e) {
            logger.warn("查询 HR 招聘员接收人失败", e);
        }
        return recipients;
    }

    /**
     * 根据 Offer 构建入职记录。
     *
     * <p>字段映射：</p>
     * <ul>
     *   <li>resumeId / requirementId / candidateName / positionName / storeName：直接复制</li>
     *   <li>positionId / storeId：Long → String 转换（OnboardingRecord 使用 String）</li>
     *   <li>probationMonths：直接复制</li>
     *   <li>salary：分 → 元（BigDecimal，除以 100，保留 2 位小数）</li>
     *   <li>hireDate：offer.entryDate，为空则用今天</li>
     *   <li>probationStart/End：根据 hireDate + probationMonths 计算</li>
     *   <li>interviewId：直接复制</li>
     * </ul>
     */
    private OnboardingRecord buildOnboardingRecordFromOffer(JobOffer offer, Long operatorId) {
        OnboardingRecord record = new OnboardingRecord();
        record.setResumeId(offer.getResumeId());
        record.setRequirementId(offer.getRequirementId());
        record.setCandidateName(offer.getCandidateName());
        record.setPositionName(offer.getPositionName());
        record.setStoreName(offer.getStoreName());

        // Long → String 转换
        if (offer.getPositionId() != null) {
            record.setPositionId(String.valueOf(offer.getPositionId()));
        }
        if (offer.getStoreId() != null) {
            record.setStoreId(String.valueOf(offer.getStoreId()));
        }

        // 试用期月数
        record.setProbationMonths(offer.getProbationMonths());

        // 薪资：分 → 元（BigDecimal，保留 2 位小数）
        if (offer.getProposedSalary() != null) {
            BigDecimal salaryYuan = BigDecimal.valueOf(offer.getProposedSalary())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            record.setSalary(salaryYuan);
        }

        // 入职日期：offer.entryDate 为空则用今天
        LocalDate hireDate = offer.getEntryDate() != null ? offer.getEntryDate() : LocalDate.now();
        record.setHireDate(hireDate);
        record.setProbationStart(hireDate);
        if (offer.getProbationMonths() != null && offer.getProbationMonths() > 0) {
            record.setProbationEnd(hireDate.plusMonths(offer.getProbationMonths()));
        } else {
            // 试用期 0 月，结束日期=开始日期
            record.setProbationEnd(hireDate);
        }

        // 面试 ID
        record.setInterviewId(offer.getInterviewId());

        // 创建人信息
        if (operatorId != null) {
            record.setCreatedBy(String.valueOf(operatorId));
        }
        record.setNotes("由 Offer ID=" + offer.getOfferId() + " 转入职");

        return record;
    }
}
