package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.constant.RecruitmentConstants;
import com.foodtraceability.dto.recruitment.RecruitmentFeedbackCreateDTO;
import com.foodtraceability.dto.recruitment.RecruitmentFeedbackVO;
import com.foodtraceability.dto.recruitment.RequirementResubmitDTO;
import com.foodtraceability.dto.recruitment.StoreEvaluationDTO;
import com.foodtraceability.entity.Interview;
import com.foodtraceability.entity.RecruitmentFeedback;
import com.foodtraceability.entity.RecruitmentRequirement;
import com.foodtraceability.mapper.RecruitmentFeedbackMapper;
import com.foodtraceability.service.InterviewService;
import com.foodtraceability.service.RecruitmentFeedbackService;
import com.foodtraceability.service.RecruitmentRequirementService;
import com.foodtraceability.service.UserService;
import com.foodtraceability.service.event.RecruitmentNotificationEvent;
import com.foodtraceability.statemachine.RecruitmentRequirementStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 招聘反馈服务实现类
 *
 * <p>遵循 ADR-001（事件 AFTER_COMMIT + try-catch 隔离）、
 * ADR-008（不混淆 Feedback 与 Approval，不修改 RecruitmentApproval 模块）。</p>
 *
 * <p>所有写方法加 {@code @Transactional(rollbackFor = Exception.class)}，
 * 事件发布代码用 try-catch 包裹（防接收人查询失败导致业务回滚）。</p>
 */
@Service
public class RecruitmentFeedbackServiceImpl extends ServiceImpl<RecruitmentFeedbackMapper, RecruitmentFeedback>
        implements RecruitmentFeedbackService {

    private static final Logger logger = LoggerFactory.getLogger(RecruitmentFeedbackServiceImpl.class);

    private final ApplicationEventPublisher eventPublisher;
    private final UserService userService;
    private final RecruitmentRequirementService requirementService;
    private final InterviewService interviewService;

    public RecruitmentFeedbackServiceImpl(ApplicationEventPublisher eventPublisher,
                                          UserService userService,
                                          RecruitmentRequirementService requirementService,
                                          InterviewService interviewService) {
        this.eventPublisher = eventPublisher;
        this.userService = userService;
        this.requirementService = requirementService;
        this.interviewService = interviewService;
    }

    // ============ 写方法（带事务 + 事件发布） ============

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RecruitmentFeedback submitFeedback(RecruitmentFeedbackCreateDTO dto, Long operatorId) {
        // 1. 校验需求存在 + 状态可反馈（open/submitted）
        RecruitmentRequirement requirement = requirementService.getById(dto.getRequirementId());
        if (requirement == null) {
            throw new BusinessException(ErrorCode.REQUIREMENT_NOT_FOUND,
                    "招聘需求不存在: " + dto.getRequirementId());
        }
        // 仅 open（submitted）状态可反馈
        if (!RecruitmentConstants.REQ_OPEN.equals(requirement.getStatus())
                && !RecruitmentConstants.REQ_SUBMITTED.equals(requirement.getStatus())) {
            throw new BusinessException(ErrorCode.REQUIREMENT_STATUS_NOT_FEEDBACKABLE,
                    "当前状态不可反馈: " + requirement.getStatus());
        }

        // 2. 校验 feedbackType（DTO 已用 @Pattern 校验，此处再防御性判断）
        String feedbackType = dto.getFeedbackType();
        if (!RecruitmentConstants.FEEDBACK_APPROVE.equals(feedbackType)
                && !RecruitmentConstants.FEEDBACK_FEEDBACK.equals(feedbackType)
                && !RecruitmentConstants.FEEDBACK_REJECT.equals(feedbackType)) {
            throw new BusinessException(ErrorCode.FEEDBACK_TYPE_INVALID,
                    "反馈类型非法: " + feedbackType);
        }

        // 3. 确定目标状态（approve→approved / feedback→feedback_given / reject→rejected）
        String targetStatus;
        if (RecruitmentConstants.FEEDBACK_APPROVE.equals(feedbackType)) {
            targetStatus = RecruitmentConstants.REQ_APPROVED;
        } else if (RecruitmentConstants.FEEDBACK_FEEDBACK.equals(feedbackType)) {
            targetStatus = RecruitmentConstants.REQ_FEEDBACK_GIVEN;
        } else {
            targetStatus = RecruitmentConstants.REQ_REJECTED;
        }

        // 4. 状态机校验
        RecruitmentRequirementStateMachine.validateTransition(requirement.getStatus(), targetStatus);

        // 5. 保存反馈记录
        RecruitmentFeedback feedback = new RecruitmentFeedback();
        feedback.setRequirementId(dto.getRequirementId());
        feedback.setFeedbackType(feedbackType);
        feedback.setContent(dto.getContent());
        feedback.setReviewerId(operatorId);
        feedback.setReviewTime(LocalDateTime.now());
        this.save(feedback);
        logger.info("招聘反馈保存成功, feedbackId={}, requirementId={}, type={}",
                feedback.getFeedbackId(), dto.getRequirementId(), feedbackType);

        // 6. 更新需求状态
        requirement.setStatus(targetStatus);
        requirement.setUpdatedAt(LocalDateTime.now());
        requirementService.updateById(requirement);

        // 7. 发布对应事件（接收人=门店店长；try-catch 隔离）
        try {
            // 门店店长接收人（requirement.storeId 为 String，需转 Long）
            List<Long> recipientUserIds = resolveStoreManagers(requirement.getStoreId());
            Map<String, Object> variables = new HashMap<>();
            variables.put("positionName", requirement.getPositionName());
            variables.put("storeName", requirement.getStoreName());
            variables.put("content", dto.getContent());

            if (RecruitmentConstants.FEEDBACK_APPROVE.equals(feedbackType)) {
                eventPublisher.publishEvent(RecruitmentNotificationEvent.requirementApproved(
                        this, operatorId, variables, recipientUserIds));
            } else if (RecruitmentConstants.FEEDBACK_FEEDBACK.equals(feedbackType)) {
                eventPublisher.publishEvent(RecruitmentNotificationEvent.feedbackGiven(
                        this, operatorId, variables, recipientUserIds));
            } else {
                eventPublisher.publishEvent(RecruitmentNotificationEvent.requirementRejected(
                        this, operatorId, variables, recipientUserIds));
            }
        } catch (Exception e) {
            logger.error("发布招聘反馈事件失败, requirementId={}, type={}",
                    dto.getRequirementId(), feedbackType, e);
        }

        return feedback;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submitStoreEvaluation(String interviewId, StoreEvaluationDTO dto, Long operatorId) {
        // 1. 查询面试记录
        Interview interview = interviewService.getInterviewById(interviewId);
        if (interview == null) {
            throw new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND,
                    "面试记录不存在: " + interviewId);
        }

        // 2. 校验评分范围（DTO 已用 @Min/@Max 校验，此处防御性判断）
        if (dto.getScore() == null || dto.getScore() < 1 || dto.getScore() > 5) {
            throw new BusinessException(ErrorCode.STORE_EVALUATION_SCORE_OUT_OF_RANGE,
                    "面评评分超出范围: " + dto.getScore());
        }

        // 3. 更新 interview 5 字段
        interview.setStoreInterviewerId(dto.getStoreInterviewerId());
        interview.setStoreInterviewerName(dto.getStoreInterviewerName());
        interview.setStoreEvaluation(dto.getEvaluation());
        interview.setStoreEvaluationScore(dto.getScore());
        interview.setStoreEvaluationTime(LocalDateTime.now());
        interview.setUpdatedAt(LocalDateTime.now());
        interviewService.updateById(interview);
        logger.info("门店面评提交成功, interviewId={}, score={}, operatorId={}",
                interviewId, dto.getScore(), operatorId);

        // 4. 发布 interview.store_evaluation.submitted 事件（接收人=HR 招聘员）
        try {
            List<Long> recipientUserIds = userService.getHrRecruiters();
            Map<String, Object> variables = new HashMap<>();
            variables.put("interviewId", interviewId);
            variables.put("candidateName", interview.getInterviewerName());
            variables.put("storeInterviewerName", dto.getStoreInterviewerName());
            variables.put("score", dto.getScore());
            variables.put("evaluation", dto.getEvaluation());
            eventPublisher.publishEvent(RecruitmentNotificationEvent.storeEvaluationSubmitted(
                    this, operatorId, variables, recipientUserIds));
        } catch (Exception e) {
            logger.error("发布门店面评提交事件失败, interviewId={}", interviewId, e);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resubmitRequirement(String requirementId, RequirementResubmitDTO dto, Long operatorId) {
        // 1. 查询需求
        RecruitmentRequirement requirement = requirementService.getById(requirementId);
        if (requirement == null) {
            throw new BusinessException(ErrorCode.REQUIREMENT_NOT_FOUND,
                    "招聘需求不存在: " + requirementId);
        }

        // 2. 状态机校验 feedback_given → open
        RecruitmentRequirementStateMachine.validateTransition(
                requirement.getStatus(), RecruitmentConstants.REQ_OPEN);

        // 3. 更新需求字段（仅更新提交的字段）
        if (StringUtils.hasText(dto.getDescription())) {
            requirement.setDescription(dto.getDescription());
        }
        if (StringUtils.hasText(dto.getRequirements())) {
            requirement.setRequirements(dto.getRequirements());
        }
        if (StringUtils.hasText(dto.getSalaryRange())) {
            requirement.setSalaryRange(dto.getSalaryRange());
        }
        requirement.setStatus(RecruitmentConstants.REQ_OPEN);
        requirement.setUpdatedAt(LocalDateTime.now());
        requirementService.updateById(requirement);
        logger.info("招聘需求重新提交成功, requirementId={}, operatorId={}",
                requirementId, operatorId);

        // 4. 发布 recruitment.requirement.resubmitted 事件（接收人=HR 招聘员）
        try {
            List<Long> recipientUserIds = userService.getHrRecruiters();
            Map<String, Object> variables = new HashMap<>();
            variables.put("requirementId", requirementId);
            variables.put("positionName", requirement.getPositionName());
            variables.put("storeName", requirement.getStoreName());
            eventPublisher.publishEvent(RecruitmentNotificationEvent.requirementResubmitted(
                    this, operatorId, variables, recipientUserIds));
        } catch (Exception e) {
            logger.error("发布招聘需求重新提交事件失败, requirementId={}", requirementId, e);
        }
        return true;
    }

    // ============ 读方法 ============

    @Override
    public List<RecruitmentFeedbackVO> getFeedbackHistory(String requirementId) {
        LambdaQueryWrapper<RecruitmentFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RecruitmentFeedback::getRequirementId, requirementId)
                .orderByDesc(RecruitmentFeedback::getReviewTime);
        List<RecruitmentFeedback> feedbacks = this.list(wrapper);
        return feedbacks.stream().map(this::toVO).collect(Collectors.toList());
    }

    // ============ 私有辅助方法 ============

    /**
     * 转换实体为 VO。
     */
    private RecruitmentFeedbackVO toVO(RecruitmentFeedback feedback) {
        RecruitmentFeedbackVO vo = new RecruitmentFeedbackVO();
        vo.setFeedbackId(feedback.getFeedbackId());
        vo.setRequirementId(feedback.getRequirementId());
        vo.setFeedbackType(feedback.getFeedbackType());
        vo.setContent(feedback.getContent());
        vo.setReviewerId(feedback.getReviewerId());
        vo.setReviewerName(feedback.getReviewerName());
        vo.setReviewTime(feedback.getReviewTime());
        vo.setCreateTime(feedback.getCreateTime());
        vo.setUpdateTime(feedback.getUpdateTime());
        return vo;
    }

    /**
     * 解析门店店长接收人（requirement.storeId 为 String）。
     * 若 storeId 无法解析为 Long，返回空列表。
     */
    private List<Long> resolveStoreManagers(String storeIdStr) {
        if (!StringUtils.hasText(storeIdStr)) {
            return List.of();
        }
        try {
            Long storeId = Long.valueOf(storeIdStr);
            return userService.getStoreManagersByStoreId(storeId);
        } catch (NumberFormatException e) {
            logger.warn("门店 ID 无法解析为 Long, storeId={}", storeIdStr);
            return List.of();
        }
    }
}
