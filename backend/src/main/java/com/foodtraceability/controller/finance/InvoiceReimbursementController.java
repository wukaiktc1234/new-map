package com.foodtraceability.controller.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.finance.BudgetVO;
import com.foodtraceability.dto.finance.InvoiceReimbursementApproveDTO;
import com.foodtraceability.dto.finance.InvoiceReimbursementPayDTO;
import com.foodtraceability.dto.finance.InvoiceReimbursementStatsVO;
import com.foodtraceability.dto.finance.InvoiceReimbursementUpdateDTO;
import com.foodtraceability.dto.finance.InvoiceReimbursementVO;
import com.foodtraceability.dto.finance.ReimbursementCreateDTO;
import com.foodtraceability.dto.finance.ReimbursementQueryDTO;
import com.foodtraceability.service.finance.InvoiceReimbursementService;
import com.foodtraceability.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 发票报销Controller
 *
 * <p>Sprint 3.1 P0 F-001：报销单 CRUD + 审批 + 付款 + 统计 + 预算信息查询。</p>
 *
 * <p>路径前缀：/v1/finance/invoice-reimbursements</p>
 *
 * <p>状态机（5 状态）：草稿(0) → 已审批(1) → 已付款(2)；
 * 草稿(0) → 已拒绝(4)；草稿(0)/已审批(1) → 已取消(3)；
 * 已取消(3)/已拒绝(4) 为终态。</p>
 */
@Tag(name = "发票报销管理", description = "报销单的创建、审批、付款等操作")
@RestController
@RequestMapping("/v1/finance/invoice-reimbursements")
public class InvoiceReimbursementController {

    private final InvoiceReimbursementService invoiceReimbursementService;

    public InvoiceReimbursementController(InvoiceReimbursementService invoiceReimbursementService) {
        this.invoiceReimbursementService = invoiceReimbursementService;
    }

    /**
     * 创建报销申请
     *
     * <p>Sprint 3.1 P0 修复：申请人信息（applicantId/applicantName）从 SecurityContext 获取，
     * 由 Controller 注入到 DTO，前端无需传入（传入也会被覆盖）。</p>
     *
     * @param dto 报销申请DTO（含明细列表）
     * @return 创建后的报销单VO
     */
    @Operation(summary = "创建报销申请", description = "创建新的报销申请单，初始状态为草稿(0)；申请人信息由后端从登录态自动填充")
    @PostMapping
    @PreAuthorize("hasAuthority('finance:reimbursement:create')")
    public Result<InvoiceReimbursementVO> create(@Valid @RequestBody ReimbursementCreateDTO dto) {
        // 从 SecurityContext 注入申请人信息（前端无需传 applicantId/applicantName）
        Long currentUserId = SecurityUtils.getCurrentUserId();
        String currentUsername = SecurityUtils.getCurrentUsername();
        if (currentUserId != null) {
            dto.setApplicantId(currentUserId);
        }
        if (currentUsername != null && (dto.getApplicantName() == null || dto.getApplicantName().isBlank())) {
            dto.setApplicantName(currentUsername);
        }
        return Result.success(invoiceReimbursementService.create(dto));
    }

    /**
     * 分页查询报销单列表
     *
     * @param query 查询条件
     * @return 分页结果
     */
    @Operation(summary = "分页查询报销单列表", description = "支持按报销单号/申请人/部门/状态等条件筛选")
    @GetMapping
    @PreAuthorize("hasAuthority('finance:reimbursement:view')")
    public Result<IPage<InvoiceReimbursementVO>> list(@Valid ReimbursementQueryDTO query) {
        return Result.success(invoiceReimbursementService.queryPage(query));
    }

    /**
     * 查询报销单详情
     *
     * @param reimbursementId 报销单ID
     * @return 报销单VO（含明细 + 审批记录）
     */
    @Operation(summary = "查询报销单详情", description = "根据ID查询报销单完整信息，包含明细和审批记录")
    @GetMapping("/{reimbursementId}")
    @PreAuthorize("hasAuthority('finance:reimbursement:view')")
    public Result<InvoiceReimbursementVO> getDetail(
            @Parameter(description = "报销单ID", required = true)
            @PathVariable Long reimbursementId) {
        return Result.success(invoiceReimbursementService.getById(reimbursementId));
    }

    /**
     * 更新报销单（仅草稿状态可更新）
     *
     * @param reimbursementId 报销单ID
     * @param dto             更新DTO
     * @return 更新后的报销单VO
     */
    @Operation(summary = "更新报销单", description = "仅草稿(0)状态的报销单可以更新")
    @PutMapping("/{reimbursementId}")
    @PreAuthorize("hasAuthority('finance:reimbursement:create')")
    public Result<InvoiceReimbursementVO> update(
            @Parameter(description = "报销单ID", required = true)
            @PathVariable Long reimbursementId,
            @Valid @RequestBody InvoiceReimbursementUpdateDTO dto) {
        dto.setReimbursementId(reimbursementId);
        return Result.success(invoiceReimbursementService.update(dto));
    }

    /**
     * 审批报销申请
     *
     * <p>审批通过：草稿(0) → 已审批(1)；审批拒绝：草稿(0) → 已拒绝(4)。</p>
     *
     * <p>Sprint 3.1 P0 修复：审批人信息（approverId/approverName）从 SecurityContext 获取，
     * 不再要求前端通过 query param 传入。前端只需在 body 中提交审批结果。</p>
     *
     * @param reimbursementId 报销单ID
     * @param dto             审批DTO（含 approved/remark/approvedAmount）
     * @return 审批后的报销单VO
     */
    @Operation(summary = "审批报销申请", description = "审批通过：草稿→已审批；审批拒绝：草稿→已拒绝；审批人信息由后端从登录态自动填充")
    @PutMapping("/{reimbursementId}/approve")
    @PreAuthorize("hasAuthority('finance:reimbursement:approve')")
    public Result<InvoiceReimbursementVO> approve(
            @Parameter(description = "报销单ID", required = true)
            @PathVariable Long reimbursementId,
            @Valid @RequestBody InvoiceReimbursementApproveDTO dto) {
        // 从 SecurityContext 注入审批人信息
        Long approverId = SecurityUtils.getCurrentUserId();
        String approverName = SecurityUtils.getCurrentUsername();
        return Result.success(invoiceReimbursementService.approve(
                reimbursementId, approverId, approverName, dto));
    }

    /**
     * 取消报销申请（仅草稿/已审批状态可取消）
     *
     * @param reimbursementId 报销单ID
     * @param cancelReason    取消原因
     * @return 取消后的报销单VO
     */
    @Operation(summary = "取消报销申请", description = "仅草稿(0)/已审批(1)状态的报销单可以取消")
    @PutMapping("/{reimbursementId}/cancel")
    @PreAuthorize("hasAuthority('finance:reimbursement:approve')")
    public Result<InvoiceReimbursementVO> cancel(
            @Parameter(description = "报销单ID", required = true)
            @PathVariable Long reimbursementId,
            @Parameter(description = "取消原因", required = true)
            @RequestParam String cancelReason) {
        return Result.success(invoiceReimbursementService.cancel(reimbursementId, cancelReason));
    }

    /**
     * 标记报销单已付款
     *
     * <p>状态流转：已审批(1) → 已付款(2)。</p>
     *
     * @param reimbursementId 报销单ID
     * @param dto             付款DTO（含付款凭证号 + 付款日期）
     * @return 付款后的报销单VO
     */
    @Operation(summary = "标记报销单已付款", description = "状态流转：已审批(1) → 已付款(2)")
    @PutMapping("/{reimbursementId}/pay")
    @PreAuthorize("hasAuthority('finance:reimbursement:pay')")
    public Result<InvoiceReimbursementVO> pay(
            @Parameter(description = "报销单ID", required = true)
            @PathVariable Long reimbursementId,
            @Valid @RequestBody InvoiceReimbursementPayDTO dto) {
        return Result.success(invoiceReimbursementService.pay(reimbursementId, dto));
    }

    /**
     * 获取报销统计数据
     *
     * <p>Sprint 3.1 P0 修复：返回专用的 InvoiceReimbursementStatsVO（含各状态数量与金额），
     * 替代之前复用的 FinanceStatisticsVO（财务概览）。</p>
     *
     * @param query 查询条件（含起止日期/部门/状态过滤）
     * @return 报销统计VO
     */
    @Operation(summary = "获取报销统计数据", description = "按查询条件聚合统计：各状态报销单数量、总金额、已审批金额、已付款金额等")
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('finance:reimbursement:view')")
    public Result<InvoiceReimbursementStatsVO> getStats(ReimbursementQueryDTO query) {
        return Result.success(invoiceReimbursementService.getStats(query));
    }

    /**
     * 获取预算信息（用于报销页面的预算执行率展示）
     *
     * @param departmentId 部门ID（可空，查询全部部门）
     * @param budgetYear   预算年度（可空，默认当前年）
     * @return 预算VO列表
     */
    @Operation(summary = "获取预算信息", description = "用于报销页面的预算执行率展示")
    @GetMapping("/budget-info")
    @PreAuthorize("hasAuthority('finance:reimbursement:view')")
    public Result<List<BudgetVO>> getBudgetInfo(
            @Parameter(description = "部门ID（可空，查询全部部门）")
            @RequestParam(required = false) Long departmentId,
            @Parameter(description = "预算年度（可空，默认当前年）")
            @RequestParam(required = false) Integer budgetYear) {
        return Result.success(invoiceReimbursementService.getBudgetInfo(departmentId, budgetYear));
    }
}
