package com.foodtraceability.controller.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.finance.InvoiceReimbursementApproveDTO;
import com.foodtraceability.dto.finance.InvoiceReimbursementVO;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 财务审批Controller
 *
 * <p>Sprint 3.1 P0 F-002/F-003/F-017：处理财务审批相关的HTTP请求。</p>
 *
 * <p>路径前缀：/v1/finance/approvals</p>
 *
 * <p>真实化：移除 mock {count: 0} 硬编码数据，对接 InvoiceReimbursementService
 * 查询 status=pending 的报销单数量。</p>
 */
@Tag(name = "财务审批管理", description = "财务审批相关接口，包括待审批单据查询")
@RestController
@RequestMapping("/v1/finance/approvals")
public class FinanceApprovalController {

    /** 报销单待审批状态码：0=草稿（待审批） */
    private static final int REIMBURSEMENT_STATUS_PENDING = 0;

    private final InvoiceReimbursementService invoiceReimbursementService;

    public FinanceApprovalController(InvoiceReimbursementService invoiceReimbursementService) {
        this.invoiceReimbursementService = invoiceReimbursementService;
    }

    /**
     * 获取当前用户待审批的财务单据数量
     *
     * <p>聚合查询：报销单 status=0（草稿/待审批）的数量。</p>
     *
     * @return 待审批数量统计（含 reimbursement 字段）
     */
    @Operation(summary = "获取当前待审批财务单据数量",
            description = "聚合查询报销单等财务单据的待审批数量")
    @GetMapping("/pending/current")
    @PreAuthorize("hasAuthority('finance:approval:view')")
    public Result<Map<String, Object>> getPendingApprovalCount() {
        // 查询待审批报销单数量（status=0 草稿状态）
        ReimbursementQueryDTO query = new ReimbursementQueryDTO();
        query.setStatus(REIMBURSEMENT_STATUS_PENDING);
        query.setCurrent(1);
        query.setSize(1);
        IPage<InvoiceReimbursementVO> page = invoiceReimbursementService.queryPage(query);

        Map<String, Object> result = new HashMap<>();
        result.put("count", page.getTotal());
        result.put("reimbursementCount", page.getTotal());
        return Result.success(result);
    }

    /**
     * 分页查询待审批的财务单据列表
     *
     * <p>查询报销单 status=0（草稿/待审批）的分页列表。</p>
     *
     * @param query 查询条件（含分页参数）
     * @return 待审批报销单分页结果
     */
    @Operation(summary = "分页查询待审批财务单据列表",
            description = "查询状态为待审批（草稿）的报销单分页列表")
    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('finance:approval:view')")
    public Result<IPage<InvoiceReimbursementVO>> getPendingList(ReimbursementQueryDTO query) {
        // 强制按待审批状态（status=0 草稿）查询
        query.setStatus(REIMBURSEMENT_STATUS_PENDING);
        return Result.success(invoiceReimbursementService.queryPage(query));
    }

    /**
     * 获取审批详情
     *
     * @param id 报销单ID
     * @return 报销单VO（含明细 + 审批记录）
     */
    @Operation(summary = "获取审批详情", description = "根据ID查询报销单完整信息，包含明细和审批记录")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:approval:view')")
    public Result<InvoiceReimbursementVO> getDetail(
            @Parameter(description = "报销单ID", required = true)
            @PathVariable Long id) {
        return Result.success(invoiceReimbursementService.getById(id));
    }

    /**
     * 审批通过
     *
     * <p>状态流转：草稿(0) → 已审批(1)；审批人信息由后端从登录态自动填充。</p>
     *
     * @param id  报销单ID
     * @param dto 审批DTO（含 remark/approvedAmount）
     * @return 审批后的报销单VO
     */
    @Operation(summary = "审批通过", description = "审批通过：草稿→已审批；审批人信息由后端从登录态自动填充")
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('finance:approval:approve')")
    public Result<InvoiceReimbursementVO> approve(
            @Parameter(description = "报销单ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody InvoiceReimbursementApproveDTO dto) {
        // 强制审批通过，approved 由接口语义决定而非前端传入
        dto.setApproved(true);
        Long approverId = SecurityUtils.getCurrentUserId();
        String approverName = SecurityUtils.getCurrentUsername();
        return Result.success(invoiceReimbursementService.approve(id, approverId, approverName, dto));
    }

    /**
     * 审批拒绝
     *
     * <p>状态流转：草稿(0) → 已拒绝(4)；remark 必填作为拒绝原因；审批人信息由后端从登录态自动填充。</p>
     *
     * @param id  报销单ID
     * @param dto 审批DTO（含 remark 拒绝原因）
     * @return 审批后的报销单VO
     */
    @Operation(summary = "审批拒绝", description = "审批拒绝：草稿→已拒绝；remark 必填作为拒绝原因")
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('finance:approval:approve')")
    public Result<InvoiceReimbursementVO> reject(
            @Parameter(description = "报销单ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody InvoiceReimbursementApproveDTO dto) {
        // 强制审批拒绝，approved 由接口语义决定而非前端传入
        dto.setApproved(false);
        Long approverId = SecurityUtils.getCurrentUserId();
        String approverName = SecurityUtils.getCurrentUsername();
        return Result.success(invoiceReimbursementService.approve(id, approverId, approverName, dto));
    }
}
