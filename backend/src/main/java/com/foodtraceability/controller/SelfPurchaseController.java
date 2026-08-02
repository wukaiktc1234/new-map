package com.foodtraceability.controller;

import com.foodtraceability.entity.SelfPurchase;
import com.foodtraceability.entity.SelfPurchaseItem;
import com.foodtraceability.service.SelfPurchaseService;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.SelfPurchaseRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 自采管理控制器
 * 实现自采相关的API接口
 */
@RestController
@RequestMapping("/v1/self-purchase")
@Tag(name = "自采管理", description = "自采记录管理相关接口")
public class SelfPurchaseController {
    

    public SelfPurchaseController(SelfPurchaseService selfPurchaseService) {
        this.selfPurchaseService = selfPurchaseService;
    }

    private final SelfPurchaseService selfPurchaseService;
    
    /**
     * 创建自采记录
     * @param selfPurchase 自采记录信息
     * @param items 自采商品明细列表
     * @return 创建结果
     */
    @PostMapping
    @Operation(summary = "创建自采记录", description = "创建自采记录并添加商品明细")
    public Result createSelfPurchase(@RequestBody SelfPurchaseRequest request) {
        boolean result = selfPurchaseService.createSelfPurchase(
                request.getSelfPurchase(), request.getItems());
        if (result) {
            return Result.success("自采记录创建成功");
        } else {
            return Result.error("自采记录创建失败");
        }
    }
    
    /**
     * 自采入库
     * @param selfPurchaseId 自采记录ID
     * @return 入库结果
     */
    @PostMapping("/inbound/{selfPurchaseId}")
    @Operation(summary = "自采入库", description = "处理自采记录的入库操作")
    public Result inboundSelfPurchase(@PathVariable String selfPurchaseId) {
        boolean result = selfPurchaseService.inboundSelfPurchase(selfPurchaseId);
        if (result) {
            return Result.success("自采入库成功");
        } else {
            return Result.error("自采入库失败");
        }
    }
    
    /**
     * 自采报销
     * @param selfPurchaseId 自采记录ID
     * @return 报销结果
     */
    @PostMapping("/reimburse/{selfPurchaseId}")
    @Operation(summary = "自采报销", description = "处理自采记录的报销操作")
    public Result reimburseSelfPurchase(@PathVariable String selfPurchaseId) {
        boolean result = selfPurchaseService.reimburseSelfPurchase(selfPurchaseId);
        if (result) {
            return Result.success("自采报销成功");
        } else {
            return Result.error("自采报销失败");
        }
    }
    
    /**
     * 获取自采记录详情
     * @param selfPurchaseId 自采记录ID
     * @return 自采记录信息
     */
    @GetMapping("/{selfPurchaseId}")
    @Operation(summary = "获取自采记录详情", description = "根据自采记录ID获取详细信息")
    public Result getSelfPurchaseById(@PathVariable String selfPurchaseId) {
        SelfPurchase selfPurchase = selfPurchaseService.getSelfPurchaseById(selfPurchaseId);
        if (selfPurchase != null) {
            return Result.success(selfPurchase);
        } else {
            return Result.error("自采记录不存在");
        }
    }
    
    /**
     * 获取自采商品明细
     * @param selfPurchaseId 自采记录ID
     * @return 自采商品明细列表
     */
    @GetMapping("/{selfPurchaseId}/items")
    @Operation(summary = "获取自采商品明细", description = "根据自采记录ID获取商品明细列表")
    public Result getSelfPurchaseItems(@PathVariable String selfPurchaseId) {
        List<SelfPurchaseItem> items = selfPurchaseService.getSelfPurchaseItems(selfPurchaseId);
        return Result.success(items);
    }
    
    /**
     * 取消自采记录
     * @param selfPurchaseId 自采记录ID
     * @return 取消结果
     */
    @PostMapping("/cancel/{selfPurchaseId}")
    @Operation(summary = "取消自采记录", description = "取消未完成的自采记录")
    public Result cancelSelfPurchase(@PathVariable String selfPurchaseId) {
        boolean result = selfPurchaseService.cancelSelfPurchase(selfPurchaseId);
        if (result) {
            return Result.success("自采记录取消成功");
        } else {
            return Result.error("自采记录取消失败");
        }
    }
    
}
