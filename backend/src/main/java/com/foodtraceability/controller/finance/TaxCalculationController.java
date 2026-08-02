package com.foodtraceability.controller.finance;

import com.foodtraceability.common.Result;
import com.foodtraceability.service.finance.ElectronicTaxBureauService;
import com.foodtraceability.service.finance.TaxCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 税务计算控制器
 * <p>
 * 提供自动计税、纳税申报表生成、电子税务局对接等功能。
 * 电子税务局接口当前为占位实现，待后续版本对接。
 * </p>
 */
@Tag(name = "税务计算管理", description = "自动计税、纳税申报表生成与电子税务局对接")
@RestController
@RequestMapping("/v1/finance/tax")
public class TaxCalculationController {

    private final TaxCalculationService taxCalculationService;
    private final ElectronicTaxBureauService electronicTaxBureauService;

    public TaxCalculationController(
            TaxCalculationService taxCalculationService,
            ElectronicTaxBureauService electronicTaxBureauService) {
        this.taxCalculationService = taxCalculationService;
        this.electronicTaxBureauService = electronicTaxBureauService;
    }

    /**
     * 计算税款
     *
     * @param taxType 税种类型：VAT（增值税）、INCOME_TAX（所得税）
     * @param storeId 门店ID（可选）
     * @param year    年份
     * @param month   月份
     * @return 税款计算结果
     */
    @Operation(summary = "计算税款", description = "根据税种和期间自动计算应缴税款（增值税/所得税）")
    @GetMapping("/calculate/{taxType}")
    @PreAuthorize("hasAuthority('finance:tax:view')")
    public Result<Map<String, Object>> calculateTax(
            @Parameter(description = "税种类型：VAT（增值税）、INCOME_TAX（所得税）") @PathVariable String taxType,
            @Parameter(description = "门店ID") @RequestParam(required = false) Long storeId,
            @Parameter(description = "年份，如 2026") @RequestParam String year,
            @Parameter(description = "月份，如 06") @RequestParam String month) {

        Map<String, Object> result;
        if ("VAT".equalsIgnoreCase(taxType)) {
            result = taxCalculationService.calculateVAT(storeId != null ? storeId : 1L, year, month);
        } else if ("INCOME_TAX".equalsIgnoreCase(taxType)) {
            result = taxCalculationService.calculateIncomeTax(storeId != null ? storeId : 1L, year, month);
        } else {
            return Result.error("不支持的税种类型: " + taxType);
        }
        return Result.success(result);
    }

    /**
     * 获取纳税申报表列表
     *
     * @param storeId 门店ID（可选）
     * @param taxType 税种类型（可选）
     * @return 申报表列表
     */
    @Operation(summary = "纳税申报表列表", description = "获取纳税申报表列表，支持按税种筛选")
    @GetMapping("/returns")
    @PreAuthorize("hasAuthority('finance:tax:view')")
    public Result<Map<String, Object>> getReturnList(
            @Parameter(description = "门店ID") @RequestParam(required = false) Long storeId,
            @Parameter(description = "税种类型") @RequestParam(required = false) String taxType) {

        return Result.success(taxCalculationService.getTaxReturnList(storeId, taxType));
    }

    /**
     * 获取纳税申报表详情
     *
     * @param id 申报表ID
     * @return 申报表详情
     */
    @Operation(summary = "申报表详情", description = "根据ID获取纳税申报表详情")
    @GetMapping("/returns/{id}")
    @PreAuthorize("hasAuthority('finance:tax:view')")
    public Result<Map<String, Object>> getReturnDetail(
            @Parameter(description = "申报表ID") @PathVariable Long id) {
        return Result.success(taxCalculationService.getTaxReturnDetail(id));
    }

    /**
     * 生成纳税申报表
     *
     * @param request 生成请求，包含 taxType、period、totalTax
     * @return 生成的申报表
     */
    @Operation(summary = "生成纳税申报表", description = "根据税种和期间生成纳税申报表")
    @PostMapping("/returns/generate")
    @PreAuthorize("hasAuthority('finance:tax:create')")
    public Result<Map<String, Object>> generateReturn(
            @RequestBody Map<String, Object> request) {

        String taxType = (String) request.get("taxType");
        String period = (String) request.get("period");
        Long storeId = request.get("storeId") != null
                ? Long.valueOf(request.get("storeId").toString())
                : 1L;

        if (taxType == null || period == null) {
            return Result.error("税种和期间不能为空");
        }

        Map<String, Object> result = taxCalculationService.generateTaxReturn(storeId, taxType, period);
        return Result.success(result);
    }

    /**
     * 提交纳税申报表到电子税务局
     *
     * @param request 提交请求，包含申报表数据
     * @return 提交结果
     */
    @Operation(summary = "提交到电子税务局", description = "将纳税申报表提交至电子税务局（当前为占位实现）")
    @PostMapping("/bureau/submit")
    @PreAuthorize("hasAuthority('finance:tax:create')")
    public Result<Map<String, Object>> submitToBureau(
            @RequestBody Map<String, Object> request) {
        return electronicTaxBureauService.submitTaxReturn(request);
    }

    /**
     * 查询电子税务局申报状态
     *
     * @param submissionId 提交单号
     * @return 申报状态
     */
    @Operation(summary = "查询电子税务局状态", description = "查询纳税申报在电子税务局的处理状态（当前为占位实现）")
    @GetMapping("/bureau/status/{submissionId}")
    @PreAuthorize("hasAuthority('finance:tax:view')")
    public Result<Map<String, Object>> getBureauStatus(
            @Parameter(description = "电子税务局提交单号") @PathVariable String submissionId) {
        return electronicTaxBureauService.querySubmissionStatus(submissionId);
    }
}
