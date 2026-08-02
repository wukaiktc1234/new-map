package com.foodtraceability.controller.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.finance.AccountMappingRuleCreateDTO;
import com.foodtraceability.dto.finance.AccountMappingRuleQueryDTO;
import com.foodtraceability.dto.finance.AccountMappingRuleUpdateDTO;
import com.foodtraceability.dto.finance.AccountMappingRuleVO;
import com.foodtraceability.dto.finance.AutoVoucherQueryDTO;
import com.foodtraceability.dto.finance.AutoVoucherVO;
import com.foodtraceability.dto.finance.MappingRuleToggleDTO;
import com.foodtraceability.dto.finance.TestMappingResultVO;
import com.foodtraceability.dto.finance.VoucherBatchPostDTO;
import com.foodtraceability.dto.finance.VoucherRegenerateDTO;
import com.foodtraceability.dto.finance.VoucherStatsVO;
import com.foodtraceability.dto.finance.VoucherVoidDTO;
import com.foodtraceability.service.finance.AutoVoucherManageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 自动凭证管理Controller
 *
 * <p>F-018：自动凭证管理，提供科目映射规则管理和凭证生命周期管理。
 * 实现业务事件到会计凭证的自动化映射配置与凭证审核/过账/作废等操作。</p>
 *
 * <p>路径前缀：/v1/finance/auto-voucher</p>
 *
 * <p>包含15个端点：
 * <ul>
 *   <li>映射规则管理（6个）：分页查询/创建/更新/删除/启用禁用/测试匹配</li>
 *   <li>凭证管理（9个）：分页查询/详情/审核/过账/作废/批量过账/重新生成/统计/导出</li>
 * </ul>
 * </p>
 *
 * <p>注意：/stats、/vouchers/export、/vouchers/batch-post、/vouchers/regenerate
 * 等固定路径需定义在 /{id} 类路径之前，避免路径变量匹配冲突。</p>
 */
@Tag(name = "自动凭证管理", description = "自动凭证的映射规则配置与凭证生命周期管理")
@RestController
@RequestMapping("/v1/finance/auto-vouchers")
public class AutoVoucherController {

    private final AutoVoucherManageService autoVoucherManageService;

    public AutoVoucherController(AutoVoucherManageService autoVoucherManageService) {
        this.autoVoucherManageService = autoVoucherManageService;
    }

    // ==================== 映射规则管理 ====================

    /**
     * 分页查询科目映射规则
     */
    @Operation(summary = "分页查询科目映射规则", description = "支持按事件类型/关键词筛选")
    @GetMapping("/rules")
    @PreAuthorize("hasAuthority('finance:auto-voucher:view')")
    public Result<IPage<AccountMappingRuleVO>> getMappingRules(AccountMappingRuleQueryDTO query) {
        return Result.success(autoVoucherManageService.getMappingRulesPage(query));
    }

    /**
     * 创建科目映射规则
     */
    @Operation(summary = "创建科目映射规则", description = "创建新的科目映射规则，默认启用")
    @PostMapping("/rules")
    @PreAuthorize("hasAuthority('finance:auto-voucher:create')")
    public Result<AccountMappingRuleVO> createMappingRule(@Valid @RequestBody AccountMappingRuleCreateDTO dto) {
        return Result.success(autoVoucherManageService.createMappingRule(dto));
    }

    /**
     * 测试规则匹配
     * 固定路径，需在 /rules/{ruleId} 之前定义
     */
    @Operation(summary = "测试规则匹配", description = "根据事件类型和金额模拟匹配规则并生成模拟分录")
    @GetMapping("/rules/test")
    @PreAuthorize("hasAuthority('finance:auto-voucher:view')")
    public Result<TestMappingResultVO> testMappingRule(
            @Parameter(description = "事件类型", required = true)
            @RequestParam String eventType,
            @Parameter(description = "金额（分）", required = true)
            @RequestParam Long amount) {
        return Result.success(autoVoucherManageService.testMappingRule(eventType, amount));
    }

    /**
     * 更新科目映射规则
     */
    @Operation(summary = "更新科目映射规则", description = "更新指定规则的字段，仅传入的字段会被更新")
    @PutMapping("/rules/{ruleId}")
    @PreAuthorize("hasAuthority('finance:auto-voucher:update')")
    public Result<AccountMappingRuleVO> updateMappingRule(
            @Parameter(description = "规则ID", required = true)
            @PathVariable Long ruleId,
            @Valid @RequestBody AccountMappingRuleUpdateDTO dto) {
        return Result.success(autoVoucherManageService.updateMappingRule(ruleId, dto));
    }

    /**
     * 删除科目映射规则
     */
    @Operation(summary = "删除科目映射规则", description = "删除指定的科目映射规则")
    @DeleteMapping("/rules/{ruleId}")
    @PreAuthorize("hasAuthority('finance:auto-voucher:delete')")
    public Result<Boolean> deleteMappingRule(
            @Parameter(description = "规则ID", required = true)
            @PathVariable Long ruleId) {
        return Result.success(autoVoucherManageService.deleteMappingRule(ruleId));
    }

    /**
     * 启用/禁用映射规则
     */
    @Operation(summary = "启用/禁用映射规则", description = "切换映射规则的启用状态")
    @PutMapping("/rules/{ruleId}/toggle")
    @PreAuthorize("hasAuthority('finance:auto-voucher:update')")
    public Result<Boolean> toggleMappingRule(
            @Parameter(description = "规则ID", required = true)
            @PathVariable Long ruleId,
            @Valid @RequestBody MappingRuleToggleDTO dto) {
        return Result.success(autoVoucherManageService.toggleMappingRule(ruleId, dto.getEnabled()));
    }

    // ==================== 凭证统计 ====================

    /**
     * 获取凭证统计信息
     * 固定路径，位于根级别，无路径冲突
     */
    @Operation(summary = "获取凭证统计信息", description = "统计凭证总数/待审核/今日过账/本月数量/失败数及热门事件类型")
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('finance:auto-voucher:view')")
    public Result<VoucherStatsVO> getVoucherStats(
            @Parameter(description = "起始日期（yyyy-MM-dd）")
            @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期（yyyy-MM-dd）")
            @RequestParam(required = false) String endDate) {
        return Result.success(autoVoucherManageService.getVoucherStats(startDate, endDate));
    }

    // ==================== 凭证管理 ====================

    /**
     * 分页查询自动凭证列表
     */
    @Operation(summary = "分页查询自动凭证列表", description = "支持按日期范围/状态/事件类型筛选")
    @GetMapping("/vouchers")
    @PreAuthorize("hasAuthority('finance:auto-voucher:view')")
    public Result<IPage<AutoVoucherVO>> getVouchers(AutoVoucherQueryDTO query) {
        return Result.success(autoVoucherManageService.getVouchersPage(query));
    }

    /**
     * 批量过账
     * 固定路径，需在 /vouchers/{voucherId} 之前定义
     */
    @Operation(summary = "批量过账", description = "批量将已审核凭证过账，跳过状态不符的凭证")
    @PostMapping("/vouchers/batch-post")
    @PreAuthorize("hasAuthority('finance:auto-voucher:update')")
    public Result<Boolean> batchPostVouchers(@RequestBody VoucherBatchPostDTO dto) {
        return Result.success(autoVoucherManageService.batchPostVouchers(dto.getVoucherIds()));
    }

    /**
     * 重新生成凭证
     * 固定路径，需在 /vouchers/{voucherId} 之前定义
     */
    @Operation(summary = "重新生成凭证", description = "根据来源事件ID重新生成凭证")
    @PostMapping("/vouchers/regenerate")
    @PreAuthorize("hasAuthority('finance:auto-voucher:create')")
    public Result<AutoVoucherVO> regenerateVoucher(@RequestBody VoucherRegenerateDTO dto) {
        return Result.success(autoVoucherManageService.regenerateVoucher(dto.getSourceEventId()));
    }

    /**
     * 导出凭证数据
     * 固定路径，需在 /vouchers/{voucherId} 之前定义
     */
    @Operation(summary = "导出凭证数据", description = "按查询条件导出凭证为Excel文件")
    @GetMapping("/vouchers/export")
    @PreAuthorize("hasAuthority('finance:auto-voucher:export')")
    public ResponseEntity<byte[]> exportVouchers(AutoVoucherQueryDTO query) {
        byte[] data = autoVoucherManageService.exportVouchers(query);
        String fileName = URLEncoder.encode("auto_vouchers.xlsx", StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");

        return ResponseEntity.ok().headers(headers).body(data);
    }

    /**
     * 获取凭证详情
     */
    @Operation(summary = "获取凭证详情", description = "根据ID查询凭证完整信息，含分录明细")
    @GetMapping("/vouchers/{voucherId}")
    @PreAuthorize("hasAuthority('finance:auto-voucher:view')")
    public Result<AutoVoucherVO> getVoucherDetail(
            @Parameter(description = "凭证ID", required = true)
            @PathVariable Long voucherId) {
        return Result.success(autoVoucherManageService.getVoucherDetail(voucherId));
    }

    /**
     * 审核凭证
     */
    @Operation(summary = "审核凭证", description = "状态流转：pending → reviewed")
    @PutMapping("/vouchers/{voucherId}/review")
    @PreAuthorize("hasAuthority('finance:auto-voucher:update')")
    public Result<Boolean> reviewVoucher(
            @Parameter(description = "凭证ID", required = true)
            @PathVariable Long voucherId) {
        return Result.success(autoVoucherManageService.reviewVoucher(voucherId));
    }

    /**
     * 过账凭证
     */
    @Operation(summary = "过账凭证", description = "状态流转：reviewed → posted")
    @PutMapping("/vouchers/{voucherId}/post")
    @PreAuthorize("hasAuthority('finance:auto-voucher:update')")
    public Result<Boolean> postVoucher(
            @Parameter(description = "凭证ID", required = true)
            @PathVariable Long voucherId) {
        return Result.success(autoVoucherManageService.postVoucher(voucherId));
    }

    /**
     * 作废凭证
     */
    @Operation(summary = "作废凭证", description = "作废凭证，需提供作废原因。已过账凭证不能直接作废")
    @PutMapping("/vouchers/{voucherId}/void")
    @PreAuthorize("hasAuthority('finance:auto-voucher:delete')")
    public Result<Boolean> voidVoucher(
            @Parameter(description = "凭证ID", required = true)
            @PathVariable Long voucherId,
            @Valid @RequestBody VoucherVoidDTO dto) {
        return Result.success(autoVoucherManageService.voidVoucher(voucherId, dto.getReason()));
    }
}
