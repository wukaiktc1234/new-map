package com.foodtraceability.controller.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.finance.ManualInvoiceInputDTO;
import com.foodtraceability.dto.finance.ManualInvoiceQueryDTO;
import com.foodtraceability.dto.finance.ManualInvoiceStatsVO;
import com.foodtraceability.dto.finance.ManualInvoiceUpdateDTO;
import com.foodtraceability.dto.finance.ManualInvoiceVO;
import com.foodtraceability.entity.ElectronicVoucher;
import com.foodtraceability.service.finance.ManualInvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

/**
 * 手动开票Controller
 *
 * <p>F-019：手动开票管理，用于OCR识别失败或用户主动选择手动输入发票信息，
 * 支持手动发票的创建、查询、更新、删除、验真与统计。</p>
 *
 * <p>路径前缀：/v1/finance/manual-invoices</p>
 */
@Tag(name = "手动开票", description = "手动输入发票的创建、查询、更新、删除、验真与统计")
@RestController
@RequestMapping("/v1/finance/manual-invoices")
public class ManualInvoiceController {

    private final ManualInvoiceService manualInvoiceService;

    public ManualInvoiceController(ManualInvoiceService manualInvoiceService) {
        this.manualInvoiceService = manualInvoiceService;
    }

    /**
     * 创建手动发票
     *
     * @param input 手动输入信息
     * @return 创建的电子凭证
     */
    @Operation(summary = "创建手动发票", description = "手动输入发票信息创建电子凭证，用于OCR识别失败或用户主动选择手动输入")
    @PostMapping
    @PreAuthorize("hasAuthority('finance:manual-invoice:create')")
    public Result<ElectronicVoucher> create(@Valid @RequestBody ManualInvoiceInputDTO input) {
        return Result.success(manualInvoiceService.createFromManualInput(input));
    }

    /**
     * 分页查询手动发票
     *
     * @param query 查询条件
     * @return 分页结果
     */
    @Operation(summary = "分页查询手动发票", description = "支持按发票号码、类型、日期范围筛选")
    @GetMapping
    @PreAuthorize("hasAuthority('finance:manual-invoice:view')")
    public Result<IPage<ManualInvoiceVO>> queryPage(ManualInvoiceQueryDTO query) {
        return Result.success(manualInvoiceService.queryPage(query));
    }

    /**
     * 统计手动发票
     *
     * @param startDate 起始日期（yyyy-MM-dd）
     * @param endDate   结束日期（yyyy-MM-dd）
     * @return 统计信息
     */
    @Operation(summary = "统计手动发票", description = "按日期范围统计手动发票总数、已验真数、待验真数")
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('finance:manual-invoice:view')")
    public Result<ManualInvoiceStatsVO> getStats(
            @Parameter(description = "起始日期（yyyy-MM-dd）")
            @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期（yyyy-MM-dd）")
            @RequestParam(required = false) String endDate) {
        return Result.success(manualInvoiceService.getStats(startDate, endDate));
    }

    /**
     * 获取手动发票详情
     *
     * @param id 凭证ID
     * @return 手动发票VO
     */
    @Operation(summary = "获取手动发票详情", description = "根据ID查询手动发票完整信息")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:manual-invoice:view')")
    public Result<ManualInvoiceVO> getDetail(
            @Parameter(description = "凭证ID", required = true)
            @PathVariable Long id) {
        return Result.success(manualInvoiceService.getDetail(id));
    }

    /**
     * 更新手动发票
     *
     * @param id  凭证ID
     * @param dto 更新DTO
     * @return 操作结果
     */
    @Operation(summary = "更新手动发票", description = "更新发票代码、号码、金额、备注等信息")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:manual-invoice:update')")
    public Result<Boolean> update(
            @Parameter(description = "凭证ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody ManualInvoiceUpdateDTO dto) {
        return Result.success(manualInvoiceService.update(id, dto));
    }

    /**
     * 删除手动发票（逻辑删除）
     *
     * @param id 凭证ID
     * @return 操作结果
     */
    @Operation(summary = "删除手动发票", description = "逻辑删除手动发票记录")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:manual-invoice:delete')")
    public Result<Boolean> delete(
            @Parameter(description = "凭证ID", required = true)
            @PathVariable Long id) {
        return Result.success(manualInvoiceService.delete(id));
    }

    /**
     * 发票验真
     *
     * @param id 凭证ID
     * @return 操作结果
     */
    @Operation(summary = "发票验真", description = "对手动发票进行验真，标记为已验证状态")
    @PostMapping("/{id}/verify")
    @PreAuthorize("hasAuthority('finance:manual-invoice:verify')")
    public Result<Boolean> verify(
            @Parameter(description = "凭证ID", required = true)
            @PathVariable Long id) {
        return Result.success(manualInvoiceService.verify(id));
    }
}
