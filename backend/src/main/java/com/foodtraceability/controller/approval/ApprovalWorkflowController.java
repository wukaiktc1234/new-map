package com.foodtraceability.controller.approval;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.PageResult;
import com.foodtraceability.dto.approval.ApprovalActionDTO;
import com.foodtraceability.dto.approval.ApprovalAuditLogVO;
import com.foodtraceability.dto.approval.ApprovalCurrentNodeVO;
import com.foodtraceability.dto.approval.ApprovalWorkflowCreateDTO;
import com.foodtraceability.dto.approval.ApprovalWorkflowQueryDTO;
import com.foodtraceability.dto.approval.ApprovalWorkflowVO;
import com.foodtraceability.service.approval.ApprovalWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 审批工作流管理控制器
 * 提供审批流程的 CRUD、启用/禁用、审批操作（提交/通过/驳回/撤回）、审批记录查询等接口
 *
 * <p>路径前缀: /v1/approval/workflows
 *
 * <p>注意：字面量路径（/page、/default、/current-node、/submit、/approve、/reject、/withdraw）
 * 必须在 /{id} 之前定义，避免被路径变量拦截。
 */
@Tag(name = "审批管理-审批流程", description = "审批流程定义及审批操作相关接口")
@RestController
@RequestMapping("/v1/approval/workflows")
public class ApprovalWorkflowController {

    private static final Logger log = LoggerFactory.getLogger(ApprovalWorkflowController.class);

    private final ApprovalWorkflowService approvalWorkflowService;

    /**
     * 构造函数注入
     */
    public ApprovalWorkflowController(ApprovalWorkflowService approvalWorkflowService) {
        this.approvalWorkflowService = approvalWorkflowService;
    }

    // ==================== 字面量路径（必须在 /{id} 之前定义） ====================

    /**
     * 分页查询审批流程列表
     * 支持按业务类型、权限模板、启用状态、关键词筛选
     */
    @Operation(summary = "分页查询审批流程列表",
            description = "支持按业务类型、权限模板、启用状态、关键词筛选，按更新时间倒序返回")
    @GetMapping("/page")
    public Result<PageResult<ApprovalWorkflowVO>> getList(ApprovalWorkflowQueryDTO queryDTO) {
        try {
            PageResult<ApprovalWorkflowVO> result = approvalWorkflowService.getWorkflowList(queryDTO);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询审批流程列表失败", e);
            return Result.error("查询审批流程列表失败");
        }
    }

    /**
     * 获取默认审批流程（按业务类型 + 权限模板）
     */
    @Operation(summary = "获取默认审批流程",
            description = "根据业务类型和权限模板编码获取启用的默认审批流程")
    @GetMapping("/default")
    public Result<ApprovalWorkflowVO> getDefaultWorkflow(
            @Parameter(description = "业务类型") @RequestParam String businessType,
            @Parameter(description = "权限模板编码") @RequestParam String templateCode) {
        try {
            ApprovalWorkflowVO vo = approvalWorkflowService.getDefaultWorkflow(businessType, templateCode);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("获取默认审批流程失败: businessType={}, templateCode={}", businessType, templateCode, e);
            return Result.error("获取默认审批流程失败");
        }
    }

    /**
     * 获取当前审批节点信息
     */
    @Operation(summary = "获取当前审批节点",
            description = "根据业务ID和业务类型查询当前审批节点信息")
    @GetMapping("/current-node")
    public Result<ApprovalCurrentNodeVO> getCurrentNode(
            @Parameter(description = "业务ID") @RequestParam String businessId,
            @Parameter(description = "业务类型") @RequestParam String businessType) {
        try {
            ApprovalCurrentNodeVO vo = approvalWorkflowService.getCurrentNode(businessId, businessType);
            return Result.success(vo);
        } catch (Exception e) {
            log.error("获取当前审批节点失败: businessId={}, businessType={}", businessId, businessType, e);
            return Result.error("获取当前审批节点失败");
        }
    }

    /**
     * 提交审批
     */
    @Operation(summary = "提交审批",
            description = "提交业务单据进入审批流程，写入提交审批记录")
    @PostMapping("/submit")
    public Result<Void> submitApproval(@RequestBody Map<String, String> body) {
        try {
            String businessType = body.get("businessType");
            String businessId = body.get("businessId");
            if (businessType == null || businessType.isEmpty() || businessId == null || businessId.isEmpty()) {
                return Result.error("业务类型和业务ID不能为空");
            }
            approvalWorkflowService.submitApproval(businessType, businessId);
            return Result.success(null, "提交审批成功");
        } catch (Exception e) {
            log.error("提交审批失败: body={}", body, e);
            return Result.error("提交审批失败");
        }
    }

    /**
     * 审批通过
     */
    @Operation(summary = "审批通过",
            description = "对指定业务单据进行审批通过操作")
    @PostMapping("/approve")
    public Result<Void> approve(@Valid @RequestBody ApprovalActionDTO actionDTO) {
        try {
            if (actionDTO.getBusinessId() == null || actionDTO.getBusinessId().isEmpty()) {
                return Result.error("业务ID不能为空");
            }
            approvalWorkflowService.approve(actionDTO);
            return Result.success(null, "审批通过成功");
        } catch (Exception e) {
            log.error("审批通过失败: businessId={}", actionDTO.getBusinessId(), e);
            return Result.error("审批通过失败");
        }
    }

    /**
     * 审批驳回
     */
    @Operation(summary = "审批驳回",
            description = "对指定业务单据进行审批驳回操作")
    @PostMapping("/reject")
    public Result<Void> reject(@Valid @RequestBody ApprovalActionDTO actionDTO) {
        try {
            if (actionDTO.getBusinessId() == null || actionDTO.getBusinessId().isEmpty()) {
                return Result.error("业务ID不能为空");
            }
            approvalWorkflowService.reject(actionDTO);
            return Result.success(null, "审批驳回成功");
        } catch (Exception e) {
            log.error("审批驳回失败: businessId={}", actionDTO.getBusinessId(), e);
            return Result.error("审批驳回失败");
        }
    }

    /**
     * 撤回审批
     */
    @Operation(summary = "撤回审批",
            description = "撤回指定业务单据的审批申请")
    @PostMapping("/withdraw")
    public Result<Void> withdraw(@RequestBody Map<String, String> body) {
        try {
            String businessId = body.get("businessId");
            if (businessId == null || businessId.isEmpty()) {
                return Result.error("业务ID不能为空");
            }
            approvalWorkflowService.withdraw(businessId);
            return Result.success(null, "撤回审批成功");
        } catch (Exception e) {
            log.error("撤回审批失败: body={}", body, e);
            return Result.error("撤回审批失败");
        }
    }

    // ==================== 变量路径（/v1/approval/workflows/{...}） ====================

    /**
     * 新建审批流程
     */
    @Operation(summary = "新建审批流程",
            description = "创建新的审批流程定义，需指定流程名称、业务类型、权限模板、节点列表等")
    @PostMapping
    public Result<ApprovalWorkflowVO> create(@Valid @RequestBody ApprovalWorkflowCreateDTO createDTO) {
        try {
            ApprovalWorkflowVO vo = approvalWorkflowService.createWorkflow(createDTO);
            return Result.success(vo, "审批流程创建成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("创建审批流程失败", e);
            return Result.error("创建审批流程失败");
        }
    }

    /**
     * 获取审批流程详情
     */
    @Operation(summary = "获取审批流程详情",
            description = "根据流程ID获取审批流程详细信息")
    @GetMapping("/{id}")
    public Result<ApprovalWorkflowVO> getById(
            @Parameter(description = "流程ID") @PathVariable String id) {
        try {
            ApprovalWorkflowVO vo = approvalWorkflowService.getWorkflowById(id);
            if (vo == null) {
                return Result.error("审批流程不存在");
            }
            return Result.success(vo);
        } catch (Exception e) {
            log.error("获取审批流程详情失败: id={}", id, e);
            return Result.error("获取审批流程详情失败");
        }
    }

    /**
     * 更新审批流程
     */
    @Operation(summary = "更新审批流程",
            description = "根据流程ID更新审批流程信息")
    @PutMapping("/{id}")
    public Result<ApprovalWorkflowVO> update(
            @Parameter(description = "流程ID") @PathVariable String id,
            @Valid @RequestBody ApprovalWorkflowCreateDTO updateDTO) {
        try {
            ApprovalWorkflowVO vo = approvalWorkflowService.updateWorkflow(id, updateDTO);
            return Result.success(vo, "审批流程更新成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("更新审批流程失败: id={}", id, e);
            return Result.error("更新审批流程失败");
        }
    }

    /**
     * 删除审批流程（逻辑删除）
     */
    @Operation(summary = "删除审批流程",
            description = "逻辑删除指定审批流程")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @Parameter(description = "流程ID") @PathVariable String id) {
        try {
            approvalWorkflowService.deleteWorkflow(id);
            return Result.success(null, "审批流程删除成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("删除审批流程失败: id={}", id, e);
            return Result.error("删除审批流程失败");
        }
    }

    /**
     * 启用/禁用审批流程
     */
    @Operation(summary = "启用/禁用审批流程",
            description = "切换审批流程的启用状态")
    @PutMapping("/{id}/toggle-enabled")
    public Result<Void> toggleEnabled(
            @Parameter(description = "流程ID") @PathVariable String id,
            @RequestBody Map<String, Boolean> body) {
        try {
            Boolean enabled = body != null ? body.get("enabled") : null;
            if (enabled == null) {
                return Result.error("启用状态不能为空");
            }
            approvalWorkflowService.toggleEnabled(id, enabled);
            return Result.success(null, enabled ? "审批流程已启用" : "审批流程已禁用");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("切换审批流程启用状态失败: id={}", id, e);
            return Result.error("切换审批流程启用状态失败");
        }
    }

    /**
     * 获取审批记录列表
     */
    @Operation(summary = "获取审批记录",
            description = "根据业务ID获取该业务的所有审批记录，按操作时间倒序返回")
    @GetMapping("/{businessId}/logs")
    public Result<List<ApprovalAuditLogVO>> getApprovalLogs(
            @Parameter(description = "业务ID") @PathVariable String businessId) {
        try {
            List<ApprovalAuditLogVO> logs = approvalWorkflowService.getApprovalLogs(businessId);
            return Result.success(logs);
        } catch (Exception e) {
            log.error("获取审批记录失败: businessId={}", businessId, e);
            return Result.error("获取审批记录失败");
        }
    }
}
