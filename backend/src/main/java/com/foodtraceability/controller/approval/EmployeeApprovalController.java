package com.foodtraceability.controller.approval;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.approval.EmployeeApprovalCreateDTO;
import com.foodtraceability.dto.approval.EmployeeApprovalQueryDTO;
import com.foodtraceability.dto.approval.vo.ApprovalDetailVO;
import com.foodtraceability.dto.approval.vo.ApprovalStatsVO;
import com.foodtraceability.dto.approval.vo.EmployeeApprovalVO;
import com.foodtraceability.service.approval.EmployeeApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 员工日常审批管理控制器
 * 提供请假、加班、换班、出差、报销、领用等日常审批的RESTful API
 */
@Tag(name = "员工审批管理", description = "员工日常审批相关接口")
@RestController
@RequestMapping("/v1/approvals")
public class EmployeeApprovalController {

    private static final Logger log = LoggerFactory.getLogger(EmployeeApprovalController.class);

    private final EmployeeApprovalService employeeApprovalService;

    /**
     * 构造函数注入
     */
    public EmployeeApprovalController(EmployeeApprovalService employeeApprovalService) {
        this.employeeApprovalService = employeeApprovalService;
    }

    /**
     * 获取我的审批列表（我发起的）
     */
    @Operation(summary = "获取我的审批列表", description = "分页查询当前员工发起的审批申请")
    @GetMapping("/my/page")
    @PreAuthorize("hasRole('employee')")
    public Result<Map<String, Object>> getMyApprovals(
            @ModelAttribute EmployeeApprovalQueryDTO query,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            if (userId == null || userId.isEmpty()) {
                return Result.error("用户ID不能为空");
            }
            
            // 设置默认分页参数
            if (query.getCurrent() == null || query.getCurrent() <= 0) {
                query.setCurrent(1);
            }
            if (query.getSize() == null || query.getSize() <= 0) {
                query.setSize(10);
            }

            IPage<EmployeeApprovalVO> pageResult = employeeApprovalService.getMyApprovalPage(query, userId);

            Map<String, Object> result = new HashMap<>();
            result.put("records", pageResult.getRecords());
            result.put("total", pageResult.getTotal());
            result.put("current", pageResult.getCurrent());
            result.put("size", pageResult.getSize());

            return Result.success(result);
        } catch (Exception e) {
            log.error("获取我的审批列表失败，用户ID：{}", userId, e);
            return Result.error("获取我的审批列表失败");
        }
    }

    /**
     * 获取待我审批的列表
     */
    @Operation(summary = "获取待我审批的列表", description = "分页查询需要当前员工审批的申请")
    @GetMapping("/pending-review/page")
    @PreAuthorize("hasRole('employee')")
    public Result<Map<String, Object>> getPendingReviews(
            @ModelAttribute EmployeeApprovalQueryDTO query,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            if (userId == null || userId.isEmpty()) {
                return Result.error("用户ID不能为空");
            }

            if (query.getCurrent() == null || query.getCurrent() <= 0) {
                query.setCurrent(1);
            }
            if (query.getSize() == null || query.getSize() <= 0) {
                query.setSize(10);
            }

            IPage<EmployeeApprovalVO> pageResult = employeeApprovalService.getPendingReviewPage(query, userId);

            Map<String, Object> result = new HashMap<>();
            result.put("records", pageResult.getRecords());
            result.put("total", pageResult.getTotal());
            result.put("current", pageResult.getCurrent());
            result.put("size", pageResult.getSize());

            return Result.success(result);
        } catch (Exception e) {
            log.error("获取待我审批列表失败，用户ID：{}", userId, e);
            return Result.error("获取待我审批列表失败");
        }
    }

    /**
     * 获取审批详情
     */
    @Operation(summary = "获取审批详情", description = "根据审批ID获取完整的审批信息，包含上下文数据和风险预警")
    @GetMapping("/{approvalId}")
    @PreAuthorize("hasAuthority('hr:approval:view') or hasAuthority('*')")
    public Result<ApprovalDetailVO> getDetail(
            @Parameter(description = "审批ID") @PathVariable String approvalId) {
        try {
            if (approvalId == null || approvalId.isEmpty()) {
                return Result.error("审批ID不能为空");
            }

            ApprovalDetailVO detail = employeeApprovalService.getApprovalDetail(approvalId);
            return Result.success(detail);
        } catch (RuntimeException e) {
            log.warn("获取审批详情业务异常，审批ID：{}，错误：{}", approvalId, e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("获取审批详情失败，审批ID：{}", approvalId, e);
            return Result.error("获取审批详情失败");
        }
    }

    /**
     * 提交审批申请
     */
    @Operation(summary = "提交审批申请", description = "创建新的日常审批申请")
    @PostMapping
    @PreAuthorize("hasRole('employee')")
    public Result<EmployeeApprovalVO> submit(
            @Valid @RequestBody EmployeeApprovalCreateDTO dto,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            if (userId == null || userId.isEmpty()) {
                return Result.error("用户ID不能为空");
            }

            EmployeeApprovalVO vo = employeeApprovalService.submitApproval(dto, userId);
            return Result.success(vo);
        } catch (RuntimeException e) {
            log.warn("提交审批申请业务异常，用户ID：{}，错误：{}", userId, e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("提交审批申请异常，用户ID：{}", userId, e);
            return Result.error("提交审批申请失败");
        }
    }

    /**
     * 通过审批
     */
    @Operation(summary = "通过审批", description = "审批人通过该审批申请")
    @PutMapping("/{approvalId}/approve")
    @PreAuthorize("hasAnyRole('manager', 'hr', 'admin')")
    public Result<Void> approve(
            @Parameter(description = "审批ID") @PathVariable String approvalId,
            @RequestBody Map<String, String> body) {
        try {
            if (approvalId == null || approvalId.isEmpty()) {
                return Result.error("审批ID不能为空");
            }

            String comment = body != null ? body.get("comment") : "";

            // reviewerId 从安全上下文获取，不从请求体读取（防止冒充）
            // TODO: 对接 Spring Security 后从 SecurityContext 获取当前用户ID
            String reviewerId = "";  // 临时占位，后续对接认证模块后替换

            employeeApprovalService.approveApproval(approvalId, reviewerId, comment);
            return Result.success();
        } catch (RuntimeException e) {
            log.warn("通过审批业务异常，审批ID：{}，错误：{}", approvalId, e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("通过审批异常，审批ID：{}", approvalId, e);
            return Result.error("通过审批失败");
        }
    }

    /**
     * 驳回审批
     */
    @Operation(summary = "驳回审批", description = "审批人驳回该审批申请")
    @PutMapping("/{approvalId}/reject")
    @PreAuthorize("hasAnyRole('manager', 'hr', 'admin')")
    public Result<Void> reject(
            @Parameter(description = "审批ID") @PathVariable String approvalId,
            @RequestBody Map<String, String> body) {
        try {
            if (approvalId == null || approvalId.isEmpty()) {
                return Result.error("审批ID不能为空");
            }

            String comment = body != null ? body.get("comment") : "";

            // reviewerId 从安全上下文获取，不从请求体读取（防止冒充）
            // TODO: 对接 Spring Security 后从 SecurityContext 获取当前用户ID
            String reviewerId = "";  // 临时占位，后续对接认证模块后替换

            if (comment == null || comment.trim().isEmpty()) {
                return Result.error("驳回原因不能为空");
            }

            employeeApprovalService.rejectApproval(approvalId, reviewerId, comment);
            return Result.success();
        } catch (RuntimeException e) {
            log.warn("驳回审批业务异常，审批ID：{}，错误：{}", approvalId, e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("驳回审批异常，审批ID：{}", approvalId, e);
            return Result.error("驳回审批失败");
        }
    }

    /**
     * 撤回审批
     */
    @Operation(summary = "撤回审批", description = "申请人撤回待审批状态的申请")
    @PutMapping("/{approvalId}/withdraw")
    @PreAuthorize("hasRole('employee')")
    public Result<Void> withdraw(
            @Parameter(description = "审批ID") @PathVariable String approvalId,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            if (approvalId == null || approvalId.isEmpty()) {
                return Result.error("审批ID不能为空");
            }
            if (userId == null || userId.isEmpty()) {
                return Result.error("用户ID不能为空");
            }

            employeeApprovalService.withdrawApproval(approvalId, userId);
            return Result.success();
        } catch (RuntimeException e) {
            log.warn("撤回审批业务异常，审批ID：{}，错误：{}", approvalId, e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("撤回审批异常，审批ID：{}", approvalId, e);
            return Result.error("撤回审批失败");
        }
    }

    /**
     * 催办审批
     */
    @Operation(summary = "催办审批", description = "申请人向审批人发送催办通知")
    @PutMapping("/{approvalId}/urge")
    @PreAuthorize("hasRole('employee')")
    public Result<Void> urge(
            @Parameter(description = "审批ID") @PathVariable String approvalId,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            if (approvalId == null || approvalId.isEmpty()) {
                return Result.error("审批ID不能为空");
            }
            if (userId == null || userId.isEmpty()) {
                return Result.error("用户ID不能为空");
            }

            employeeApprovalService.urgeApproval(approvalId, userId);
            return Result.success();
        } catch (RuntimeException e) {
            log.warn("催办审批业务异常，审批ID：{}，错误：{}", approvalId, e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("催办审批异常，审批ID：{}", approvalId, e);
            return Result.error("催办审批失败");
        }
    }

    /**
     * 获取我的审批统计
     */
    @Operation(summary = "获取我的审批统计", description = "获取当前员工的审批统计数据")
    @GetMapping("/stats/my")
    @PreAuthorize("hasRole('employee')")
    public Result<ApprovalStatsVO> getMyStats(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            if (userId == null || userId.isEmpty()) {
                return Result.error("用户ID不能为空");
            }

            ApprovalStatsVO stats = employeeApprovalService.getMyApprovalStats(userId);
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取审批统计失败，用户ID：{}", userId, e);
            return Result.error("获取审批统计失败");
        }
    }

    /**
     * 批量获取审批基本信息
     */
    @Operation(summary = "批量获取审批基本信息", description = "根据审批ID列表批量获取审批基本信息")
    @PostMapping("/batch/basic-info")
    @PreAuthorize("hasAuthority('hr:approval:manage') or hasAuthority('*')")
    public Result<Map<String, EmployeeApprovalVO>> batchBasicInfo(
            @RequestBody List<String> approvalIds) {
        try {
            if (approvalIds == null || approvalIds.isEmpty()) {
                return Result.error("审批ID列表不能为空");
            }

            Map<String, EmployeeApprovalVO> result = employeeApprovalService.batchGetBasicInfo(approvalIds);
            return Result.success(result);
        } catch (Exception e) {
            log.error("批量获取审批基本信息失败", e);
            return Result.error("批量获取审批基本信息失败");
        }
    }
}
