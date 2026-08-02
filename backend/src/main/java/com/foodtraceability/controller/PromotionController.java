package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.marketing.PromotionCreateDTO;
import com.foodtraceability.dto.marketing.PromotionUpdateDTO;
import com.foodtraceability.entity.MarketingPromotion;
import com.foodtraceability.service.PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 促销活动管理控制器
 */
@RestController
@RequestMapping("/v1/promotions")
@Tag(name = "促销活动管理", description = "促销活动的创建、启停、效果统计")
public class PromotionController {

    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @PostMapping
    @Operation(summary = "创建促销活动")
    public Result<MarketingPromotion> create(@Valid @RequestBody PromotionCreateDTO createDTO) {
        try {
            MarketingPromotion promotion = promotionService.createPromotion(createDTO);
            return Result.success(promotion, "活动创建成功");
        } catch (RuntimeException e) {
            return Result.error(7001, e.getMessage());
        }
    }

    @PutMapping("/{promotionId}")
    @Operation(summary = "更新促销活动")
    public Result<MarketingPromotion> update(@PathVariable Long promotionId,
                                            @Valid @RequestBody PromotionUpdateDTO updateDTO) {
        try {
            MarketingPromotion promotion = promotionService.updatePromotion(promotionId, updateDTO);
            return Result.success(promotion, "更新成功");
        } catch (RuntimeException e) {
            return Result.error(7002, e.getMessage());
        }
    }

    @GetMapping
    @Operation(summary = "查询所有活动")
    public Result<List<MarketingPromotion>> listAll() {
        List<MarketingPromotion> list = promotionService.list();
        return Result.success(list);
    }

    @GetMapping("/active")
    @Operation(summary = "获取进行中的活动列表")
    public Result<List<MarketingPromotion>> getActive() {
        List<MarketingPromotion> list = promotionService.getActivePromotions();
        return Result.success(list);
    }

    @PostMapping("/{promotionId}/start")
    @Operation(summary = "启动活动")
    public Result<Void> start(@PathVariable Long promotionId) {
        try {
            promotionService.startPromotion(promotionId);
            return Result.success(null, "活动已启动");
        } catch (RuntimeException e) {
            return Result.error(7003, e.getMessage());
        }
    }

    @PostMapping("/{promotionId}/pause")
    @Operation(summary = "暂停活动")
    public Result<Void> pause(@PathVariable Long promotionId) {
        try {
            promotionService.pausePromotion(promotionId);
            return Result.success(null, "活动已暂停");
        } catch (RuntimeException e) {
            return Result.error(7004, e.getMessage());
        }
    }

    @PostMapping("/{promotionId}/end")
    @Operation(summary = "结束活动")
    public Result<Void> end(@PathVariable Long promotionId) {
        try {
            promotionService.endPromotion(promotionId);
            return Result.success(null, "活动已结束");
        } catch (RuntimeException e) {
            return Result.error(7005, e.getMessage());
        }
    }

    @PostMapping("/{promotionId}/cancel")
    @Operation(summary = "作废活动")
    public Result<Void> cancel(@PathVariable Long promotionId) {
        try {
            promotionService.cancelPromotion(promotionId);
            return Result.success(null, "活动已作废");
        } catch (RuntimeException e) {
            return Result.error(7006, e.getMessage());
        }
    }

    @GetMapping("/{promotionId}")
    @Operation(summary = "获取活动详情")
    public Result<MarketingPromotion> getDetail(@PathVariable Long promotionId) {
        MarketingPromotion promotion = promotionService.getById(promotionId);
        if (promotion == null) {
            return Result.error(7007, "活动不存在");
        }
        return Result.success(promotion);
    }

    @GetMapping("/statistics/effect")
    @Operation(summary = "获取活动效果统计汇总")
    public Result<Map<String, Object>> getEffectSummary() {
        Map<String, Object> summary = promotionService.getEffectSummary();
        return Result.success(summary);
    }

    @PostMapping("/{promotionId}/participate")
    @Operation(summary = "记录活动参与数据（内部调用）")
    public Result<Void> recordParticipation(@PathVariable Long promotionId,
                                           @RequestParam(required = false) Long salesAmount,
                                           @RequestParam(required = false) Long discountAmount) {
        promotionService.recordParticipation(promotionId, salesAmount, discountAmount);
        return Result.success(null);
    }
}
