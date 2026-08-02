package com.foodtraceability.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.product.PricingCreateDTO;
import com.foodtraceability.dto.product.PricingQueryDTO;
import com.foodtraceability.dto.product.PricingVO;
import com.foodtraceability.service.PricingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 产品定价管理控制器
 * 提供价格调整、批量调价、历史记录查询接口
 * 权限要求：需要product:pricing基础权限
 */
@RestController
@RequestMapping("/v1/product-center/pricing")
@Tag(name = "产品定价", description = "产品价格调整、批量调价、历史记录查询接口")
@PreAuthorize("hasAuthority('product:pricing:view')")
public class PricingController {

    private final PricingService pricingService;

    public PricingController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    /**
     * 创建定价记录（调价）
     */
    @PostMapping
    @PreAuthorize("hasAuthority('product:pricing:adjust')")
    @Operation(summary = "调整价格", description = "对单个菜品或套餐进行价格调整，自动记录历史")
    public Result<PricingVO> create(@Valid @RequestBody PricingCreateDTO dto) {
        return Result.success(pricingService.create(dto));
    }

    /**
     * 批量调价
     */
    @PostMapping("/batch/{productType}")
    @PreAuthorize("hasAuthority('product:pricing:batch')")
    @Operation(summary = "批量调价", description = "对多个同类型产品进行批量价格调整")
    public Result<List<PricingVO>> batchPricing(
            @Parameter(description = "产品类型: FOOD/COMBO") @PathVariable String productType,
            @Valid @RequestBody List<PricingCreateDTO> pricingList) {
        return Result.success(pricingService.batchPricing(productType, pricingList));
    }

    /**
     * 查询定价历史记录
     */
    @GetMapping("/history")
    @Operation(summary = "查询定价历史", description = "分页查询价格变动历史记录")
    public Result<Page<PricingVO>> queryHistory(PricingQueryDTO queryDto) {
        return Result.success(pricingService.queryHistory(queryDto));
    }

    /**
     * 获取产品当前价格
     */
    @GetMapping("/current/{productType}/{productId}")
    @Operation(summary = "获取当前价格", description = "获取指定产品的当前售价（分）")
    public Result<Long> getCurrentPrice(
            @Parameter(description = "产品类型: FOOD/COMBO") @PathVariable String productType,
            @Parameter(description = "产品ID") @PathVariable Long productId) {
        Long price = pricingService.getCurrentPrice(productType, productId);
        if (price == null) {
            return Result.error(404, "产品不存在");
        }
        return Result.success(price);
    }

    /**
     * 获取产品价格变动历史
     */
    @GetMapping("/history/{productType}/{productId}")
    @Operation(summary = "获取价格变动历史", description = "获取指定产品的最近N条价格变动记录")
    public Result<List< PricingVO>> getPriceHistory(
            @Parameter(description = "产品类型: FOOD/COMBO") @PathVariable String productType,
            @Parameter(description = "产品ID") @PathVariable Long productId,
            @Parameter(description = "数量限制") @RequestParam(defaultValue = "20") int limit) {
        return Result.success(pricingService.getPriceHistory(productType, productId, limit));
    }
}
