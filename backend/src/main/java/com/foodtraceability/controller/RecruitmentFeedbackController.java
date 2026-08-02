package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.recruitment.RecruitmentFeedbackCreateDTO;
import com.foodtraceability.dto.recruitment.RecruitmentFeedbackVO;
import com.foodtraceability.entity.RecruitmentFeedback;
import com.foodtraceability.service.RecruitmentFeedbackService;
import com.foodtraceability.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 招聘反馈控制器
 *
 * <p>对应 Ch3.2 招聘协同 2 个 API 端点（门店面评、需求重新提交在对应实体 Controller 中扩展）：</p>
 * <ul>
 *   <li>submitFeedback(approve) → recruitment.approved</li>
 *   <li>submitFeedback(feedback) → recruitment.feedback.given</li>
 *   <li>submitFeedback(reject) → recruitment.rejected</li>
 *   <li>getFeedbackHistory → 无事件</li>
 * </ul>
 *
 * <p>门店面评端点见 {@link InterviewController#submitStoreEvaluation}，
 * 需求重新提交端点见 {@link RecruitmentRequirementController#resubmitRequirement}。</p>
 */
@RestController
@RequestMapping("/v1/recruitment-feedback")
@Tag(name = "招聘反馈", description = "HR 对门店提报的招聘需求反馈接口")
public class RecruitmentFeedbackController {

    private final RecruitmentFeedbackService feedbackService;

    /**
     * 构造函数注入
     *
     * @param feedbackService 招聘反馈服务
     */
    public RecruitmentFeedbackController(RecruitmentFeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    /**
     * HR 提交招聘需求反馈（approve/feedback/reject）
     *
     * <p>根据 feedbackType 发布不同事件：</p>
     * <ul>
     *   <li>approve → recruitment.approved（接收人=门店店长）</li>
     *   <li>feedback → recruitment.feedback.given（接收人=门店店长）</li>
     *   <li>reject → recruitment.rejected（接收人=门店店长）</li>
     * </ul>
     *
     * @param dto 反馈参数
     * @return 已保存的反馈实体
     */
    @Operation(summary = "HR 提交招聘需求反馈", description = "支持 approve/feedback/reject 三种反馈类型")
    @PostMapping
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'HR_RECRUITER')")
    public Result<RecruitmentFeedback> submitFeedback(@Valid @RequestBody RecruitmentFeedbackCreateDTO dto) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        RecruitmentFeedback feedback = feedbackService.submitFeedback(dto, operatorId);
        return Result.success(feedback);
    }

    /**
     * 按招聘需求查询反馈历史
     *
     * @param requirementId 招聘需求 ID（VARCHAR(32) 雪花）
     * @return 反馈 VO 列表（按时间倒序）
     */
    @Operation(summary = "查询招聘需求反馈历史")
    @GetMapping("/requirement/{requirementId}")
    @PreAuthorize("hasAnyRole('HR_MANAGER', 'HR_RECRUITER', 'STORE_MANAGER')")
    public Result<List<RecruitmentFeedbackVO>> getFeedbackHistory(
            @Parameter(description = "招聘需求 ID", required = true) @PathVariable String requirementId) {
        List<RecruitmentFeedbackVO> history = feedbackService.getFeedbackHistory(requirementId);
        return Result.success(history);
    }
}
