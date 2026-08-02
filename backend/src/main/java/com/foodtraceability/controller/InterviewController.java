package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.HrEvaluationDTO;
import com.foodtraceability.dto.recruitment.StoreEvaluationDTO;
import com.foodtraceability.entity.Interview;
import com.foodtraceability.service.InterviewService;
import com.foodtraceability.service.RecruitmentFeedbackService;
import com.foodtraceability.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/interviews")
@Tag(name = "面试管理", description = "面试相关接口")
public class InterviewController {


    public InterviewController(InterviewService interviewService,
                               RecruitmentFeedbackService recruitmentFeedbackService) {
        this.interviewService = interviewService;
        this.recruitmentFeedbackService = recruitmentFeedbackService;
    }

    private final InterviewService interviewService;
    private final RecruitmentFeedbackService recruitmentFeedbackService;
    
    @Operation(summary = "获取面试列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('admin', 'hr', 'store_manager')")
    public Result<Map<String, Object>> getInterviews(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String resumeId) {
        Map<String, Object> result = interviewService.getInterviews(current, size, status, resumeId);
        return Result.success(result);
    }
    
    @Operation(summary = "创建面试")
    @PostMapping
    @PreAuthorize("hasAnyRole('admin', 'hr', 'store_manager')")
    public Result<Interview> createInterview(@RequestBody Interview interview) {
        Interview created = interviewService.createInterview(interview);
        return Result.success(created);
    }
    
    @Operation(summary = "更新面试状态")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'store_manager')")
    public Result<Void> updateInterviewStatus(
            @PathVariable String id,
            @RequestParam String status) {
        interviewService.updateInterviewStatus(id, status);
        return Result.success();
    }
    
    @Operation(summary = "更新面试结果")
    @PutMapping("/{id}/result")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'store_manager')")
    public Result<Void> updateInterviewResult(
            @PathVariable String id,
            @RequestParam String result,
            @RequestParam(required = false) String feedback) {
        interviewService.updateInterviewResult(id, result, feedback);
        return Result.success();
    }
    
    @Operation(summary = "根据简历ID获取面试记录")
    @GetMapping("/resume/{resumeId}")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'store_manager')")
    public Result<Interview> getInterviewByResumeId(@PathVariable String resumeId) {
        Interview interview = interviewService.getInterviewByResumeId(resumeId);
        return Result.success(interview);
    }

    @Operation(summary = "根据ID获取面试记录")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'store_manager')")
    public Result<Interview> getInterviewById(@PathVariable String id) {
        Interview interview = interviewService.getInterviewById(id);
        return Result.success(interview);
    }

    /**
     * 门店面试官提交面评
     *
     * <p>发布 interview.store_evaluation.submitted 事件（接收人=HR 招聘员，渠道=SITE_MSG）。</p>
     *
     * @param id  面试 ID
     * @param dto 面评参数（评分 1-5 + 评价内容）
     * @return 是否提交成功
     */
    @Operation(summary = "门店提交面试面评", description = "门店面试官对候选人提交面试评价")
    @PutMapping("/{id}/store-evaluation")
    @PreAuthorize("hasAnyRole('admin', 'store_manager')")
    public Result<Boolean> submitStoreEvaluation(
            @Parameter(description = "面试 ID", required = true) @PathVariable String id,
            @Valid @RequestBody StoreEvaluationDTO dto) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        boolean success = recruitmentFeedbackService.submitStoreEvaluation(id, dto, operatorId);
        return Result.success(success);
    }

    /**
     * 创建人事面谈
     *
     * <p>门店初面通过后，HR 接手进行人事面谈，登记人事面试官信息并标记状态为 scheduled。</p>
     *
     * @param interviewId      面试 ID
     * @param hrInterviewerId  人事面试官用户 ID
     * @param hrInterviewerName 人事面试官姓名
     * @return 更新后的面试记录
     */
    @Operation(summary = "创建人事面谈")
    @PostMapping("/{interviewId}/hr-interview")
    @PreAuthorize("hasAnyRole('admin', 'hr')")
    public Result<Interview> createHrInterview(
            @PathVariable String interviewId,
            @RequestParam String hrInterviewerId,
            @RequestParam String hrInterviewerName) {
        return Result.success(interviewService.createHrInterview(interviewId, hrInterviewerId, hrInterviewerName));
    }

    /**
     * 提交人事面评
     *
     * <p>HR 提交人事面评内容、评分、学历核验、背调结果及最终人事面谈状态。</p>
     *
     * @param interviewId 面试 ID
     * @param dto         人事面评参数
     * @return 更新后的面试记录
     */
    @Operation(summary = "提交人事面评")
    @PutMapping("/{interviewId}/hr-evaluation")
    @PreAuthorize("hasAnyRole('admin', 'hr')")
    public Result<Interview> submitHrEvaluation(
            @PathVariable String interviewId,
            @Valid @RequestBody HrEvaluationDTO dto) {
        return Result.success(interviewService.submitHrEvaluation(
            interviewId, dto.getEvaluation(), dto.getScore(),
            dto.getEducationVerification(), dto.getEducationRemark(),
            dto.getBackgroundCheck(), dto.getBackgroundRemark(),
            dto.getStatus()));
    }
}
