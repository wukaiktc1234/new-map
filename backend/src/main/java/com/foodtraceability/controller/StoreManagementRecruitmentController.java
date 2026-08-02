package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.store.operation.RecruitmentApprovalCreateDTO;
import com.foodtraceability.dto.store.operation.RecruitmentApprovalUpdateDTO;
import com.foodtraceability.dto.store.operation.vo.RecruitmentApprovalVO;
import com.foodtraceability.service.RecruitmentApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 门店招聘管理控制器
 * 提供自主招聘与三级审批流程的RESTful API
 * 支持提交申请、审批通过/驳回、查看审批历史等功能
 *
 * <h2>审批流程</h2>
 * <ul>
 *   <li><b>Level 1 初审</b>：区域经理审批</li>
 *   <li><b>Level 2 复审</b>：总部HR审批</li>
 *   <li><b>Level 3 终审</b>：总部HR总监审批</li>
 * </ul>
 */
@RestController
@RequestMapping("/v1/store-management/recruitment")
@Tag(name = "门店招聘管理", description = "自主招聘与三级审批流程")
public class StoreManagementRecruitmentController {

    private final RecruitmentApprovalService recruitmentApprovalService;

    /**
     * 构造函数注入
     *
     * @param recruitmentApprovalService 招聘审批服务
     */
    public StoreManagementRecruitmentController(RecruitmentApprovalService recruitmentApprovalService) {
        this.recruitmentApprovalService = recruitmentApprovalService;
    }

    /**
     * 提交招聘申请
     * 店长提交后进入审批流程，支持自动升级检测
     */
    @PostMapping("/approvals")
    @Operation(summary = "提交招聘申请", description = "店长提交新的招聘申请，进入三级审批流程")
    @PreAuthorize("hasAnyRole('STORE_MANAGER', 'ADMIN', 'admin')")
    public Result<RecruitmentApprovalVO> submitApplication(
            @Valid @RequestBody RecruitmentApprovalCreateDTO dto,
            Principal principal) {
        RecruitmentApprovalVO approval = recruitmentApprovalService.submitApplication(dto, principal.getName());
        return Result.success(approval);
    }

    /**
     * 分页查询招聘审批列表
     * 用于查看所有招聘申请记录（占位实现，返回空分页数据）
     * 对应前端测试：GET /v1/store-management/recruitment/approvals?page=1&pageSize=10
     */
    @GetMapping("/approvals")
    @Operation(summary = "招聘审批列表", description = "分页查询招聘审批记录列表")
    @PreAuthorize("hasAnyRole('STORE_MANAGER', 'REGIONAL_MANAGER', 'HR_MANAGER', 'HR_DIRECTOR', 'ADMIN', 'admin')")
    public Result<Map<String, Object>> getApprovalList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Map<String, Object> result = new HashMap<>();
        result.put("records", List.of());
        result.put("total", 0);
        result.put("current", page);
        result.put("size", pageSize);
        result.put("pages", 0);
        return Result.success(result);
    }

    /**
     * 查询我提交的申请列表
     * 用于店长查看自己提交的所有招聘申请及其审批进度
     */
    @GetMapping("/approvals/my-list")
    @Operation(summary = "我的申请列表", description = "查询当前用户提交的所有招聘申请")
    @PreAuthorize("hasAnyRole('STORE_MANAGER', 'ADMIN', 'admin')")
    public Result<IPage<RecruitmentApprovalVO>> getMyApplications(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            Principal principal) {
        IPage<RecruitmentApprovalVO> approvalPage = recruitmentApprovalService.getMyApplications(
                principal.getName(), page, size);
        return Result.success(approvalPage);
    }

    /**
     * 查询当前用户待审批的记录
     * 用于审批人查看需要自己处理的审批任务
     */
    @GetMapping("/approvals/pending-me")
    @Operation(summary = "待我审批", description = "查询当前用户待处理的审批记录")
    @PreAuthorize("hasAnyRole('REGIONAL_MANAGER', 'HR_MANAGER', 'HR_DIRECTOR', 'ADMIN', 'admin')")
    public Result<List<RecruitmentApprovalVO>> getPendingApprovals(Principal principal) {
        // 根据用户角色确定当前审批层级
        Integer currentLevel = determineCurrentLevel(principal);
        List<RecruitmentApprovalVO> approvals = recruitmentApprovalService.getPendingApprovals(
                principal.getName(), currentLevel);
        return Result.success(approvals);
    }

    /**
     * 审批通过
     * 当前层级通过后，检查是否升级到下一级或终审通过
     */
    @PutMapping("/approvals/{id}/approve")
    @Operation(summary = "审批通过", description = "通过当前层级的审批")
    @PreAuthorize("hasAnyRole('REGIONAL_MANAGER', 'HR_MANAGER', 'HR_DIRECTOR', 'ADMIN', 'admin')")
    public Result<Void> approve(@PathVariable String id, Principal principal) {
        recruitmentApprovalService.approve(id, principal.getName());
        return Result.success();
    }

    /**
     * 审批驳回
     * 驳回原因不能少于10个字符
     */
    @PutMapping("/approvals/{id}/reject")
    @Operation(summary = "审批驳回", description = "驳回当前的招聘申请")
    @PreAuthorize("hasAnyRole('REGIONAL_MANAGER', 'HR_MANAGER', 'HR_DIRECTOR', 'ADMIN', 'admin')")
    public Result<Void> reject(@PathVariable String id,
                               @Valid @RequestBody RecruitmentApprovalUpdateDTO dto,
                               Principal principal) {
        recruitmentApprovalService.reject(id, principal.getName(), dto);
        return Result.success();
    }

    /**
     * 获取审批详情
     * 包含完整的审批信息和审批历史
     */
    @GetMapping("/approvals/{id}")
    @Operation(summary = "审批详情", description = "获取招聘审批记录的详细信息")
    @PreAuthorize("isAuthenticated()")
    public Result<RecruitmentApprovalVO> getApprovalDetail(@PathVariable String id) {
        RecruitmentApprovalVO approval = recruitmentApprovalService.getApprovalDetail(id);
        return Result.success(approval);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 根据用户角色确定当前审批层级
     *
     * @param principal 用户身份
     * @return 审批层级（1=初审 2=复审 3=终审）
     */
    private Integer determineCurrentLevel(Principal principal) {
        // TODO: 根据实际的角色配置确定审批层级
        // 当前简化处理：区域经理=1，HR经理=2，HR总监=3
        if (principal == null) {
            return 1;
        }

        // 使用Spring Security上下文判断角色
        org.springframework.security.core.Authentication authentication =
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            boolean isHrDirector = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_HR_DIRECTOR"));
            boolean isHrManager = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_HR_MANAGER"));

            if (isHrDirector) {
                return 3;
            } else if (isHrManager) {
                return 2;
            }
        }

        return 1; // 区域经理默认为初审
    }
}
