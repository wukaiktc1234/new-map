package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.recruitment.RequirementResubmitDTO;
import com.foodtraceability.entity.RecruitmentRequirement;
import com.foodtraceability.service.RecruitmentFeedbackService;
import com.foodtraceability.service.RecruitmentRequirementService;
import com.foodtraceability.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/recruitment-requirements")
@Tag(name = "招聘需求管理", description = "招聘需求相关接口")
public class RecruitmentRequirementController {


    public RecruitmentRequirementController(RecruitmentRequirementService recruitmentRequirementService,
                                           RecruitmentFeedbackService recruitmentFeedbackService) {
        this.recruitmentRequirementService = recruitmentRequirementService;
        this.recruitmentFeedbackService = recruitmentFeedbackService;
    }

    private final RecruitmentRequirementService recruitmentRequirementService;
    private final RecruitmentFeedbackService recruitmentFeedbackService;

    @Operation(summary = "获取招聘需求列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('admin', 'hr', 'store_manager')")
    public Result<Map<String, Object>> getRequirements(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Map<String, Object> result = recruitmentRequirementService.getRecruitmentRequirements(
                current, size, type, status, keyword, departmentId, startDate, endDate);
        return Result.success(result);
    }

    @Operation(summary = "获取招聘需求详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'store_manager')")
    public Result<RecruitmentRequirement> getRequirement(@PathVariable String id) {
        RecruitmentRequirement requirement = recruitmentRequirementService.getById(id);
        return Result.success(requirement);
    }

    @Operation(summary = "创建招聘需求")
    @PostMapping
    @PreAuthorize("hasAnyRole('admin', 'hr', 'store_manager')")
    public Result<RecruitmentRequirement> createRequirement(@RequestBody RecruitmentRequirement requirement) {
        RecruitmentRequirement created = recruitmentRequirementService.createRequirement(requirement);
        return Result.success(created);
    }

    @Operation(summary = "更新招聘需求")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'store_manager')")
    public Result<RecruitmentRequirement> updateRequirement(
            @PathVariable String id,
            @RequestBody RecruitmentRequirement requirement) {
        RecruitmentRequirement updated = recruitmentRequirementService.updateRequirement(id, requirement);
        return Result.success(updated);
    }

    @Operation(summary = "删除招聘需求")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'hr')")
    public Result<Void> deleteRequirement(@PathVariable String id) {
        recruitmentRequirementService.deleteRequirement(id);
        return Result.success();
    }

    @Operation(summary = "审批招聘需求")
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('admin', 'hr')")
    public Result<Void> approveRequirement(
            @PathVariable String id,
            @RequestParam String approvalStatus) {
        recruitmentRequirementService.approveRequirement(id, approvalStatus);
        return Result.success();
    }

    @Operation(summary = "获取门店招聘需求")
    @GetMapping("/store/{storeId}")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'store_manager')")
    public Result<List<RecruitmentRequirement>> getRequirementsByStore(@PathVariable String storeId) {
        List<RecruitmentRequirement> requirements = recruitmentRequirementService.getRequirementsByStore(storeId);
        return Result.success(requirements);
    }

    @Operation(summary = "获取部门招聘需求")
    @GetMapping("/department/{departmentId}")
    @PreAuthorize("hasAnyRole('admin', 'hr')")
    public Result<List<RecruitmentRequirement>> getRequirementsByDepartment(@PathVariable String departmentId) {
        List<RecruitmentRequirement> requirements = recruitmentRequirementService.getRequirementsByDepartment(departmentId);
        return Result.success(requirements);
    }

    /**
     * 门店修改后重新提交招聘需求（feedback_given → open）
     *
     * <p>发布 recruitment.requirement.resubmitted 事件（接收人=HR 招聘员，渠道=SITE_MSG）。</p>
     *
     * @param id  招聘需求 ID
     * @param dto 重新提交参数（description/requirements/salaryRange，均可选）
     * @return 是否重新提交成功
     */
    @Operation(summary = "门店重新提交招聘需求", description = "门店收到 HR 需修改反馈后，修改字段并重新提交")
    @PutMapping("/{id}/resubmit")
    @PreAuthorize("hasRole('STORE_MANAGER')")
    public Result<Boolean> resubmitRequirement(
            @Parameter(description = "招聘需求 ID", required = true) @PathVariable String id,
            @Valid @RequestBody RequirementResubmitDTO dto) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        boolean success = recruitmentFeedbackService.resubmitRequirement(id, dto, operatorId);
        return Result.success(success);
    }
}
