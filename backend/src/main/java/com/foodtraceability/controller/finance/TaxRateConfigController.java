package com.foodtraceability.controller.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.service.finance.TaxRateConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 税率配置控制器
 * 管理各税种的税率配置，支持按税种和纳税人类型查询有效税率
 */
@Tag(name = "税率配置管理", description = "税种税率配置的增删改查及有效税率查询")
@RestController
@RequestMapping("/v1/finance/tax-rate-configs")
public class TaxRateConfigController {

    private final TaxRateConfigService taxRateConfigService;

    public TaxRateConfigController(TaxRateConfigService taxRateConfigService) {
        this.taxRateConfigService = taxRateConfigService;
    }

    @Operation(summary = "创建税率配置", description = "新增税种税率配置，需指定税种、纳税人类型及税率")
    @PostMapping
    public Result<TaxRateConfigVO> create(@Valid @RequestBody TaxRateConfigCreateDTO dto) {
        return Result.success(taxRateConfigService.create(dto));
    }

    @Operation(summary = "更新税率配置")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @Valid @RequestBody TaxRateConfigUpdateDTO dto) {
        return Result.success(taxRateConfigService.update(id, dto));
    }

    @Operation(summary = "删除税率配置", description = "逻辑删除指定税率配置")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(taxRateConfigService.delete(id));
    }

    @Operation(summary = "获取税率配置详情")
    @GetMapping("/{id}")
    public Result<TaxRateConfigVO> getDetail(@PathVariable Long id) {
        return Result.success(taxRateConfigService.getDetail(id));
    }

    @Operation(summary = "分页查询税率配置", description = "支持按税种、纳税人类型等条件筛选")
    @GetMapping
    public Result<IPage<TaxRateConfigVO>> getPage(TaxRateConfigQueryDTO query) {
        return Result.success(taxRateConfigService.getPage(query));
    }

    @Operation(summary = "获取当前有效税率", description = "根据税种和纳税人类型查询当前生效的税率配置")
    @GetMapping("/effective")
    public Result<TaxRateConfigVO> getEffectiveRate(
            @RequestParam Integer taxType,
            @RequestParam Integer taxpayerType) {
        return Result.success(taxRateConfigService.getEffectiveRate(taxType, taxpayerType));
    }
}
