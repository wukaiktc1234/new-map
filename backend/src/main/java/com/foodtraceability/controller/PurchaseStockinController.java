package com.foodtraceability.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.PurchaseQualityCheckDTO;
import com.foodtraceability.dto.PurchaseStockinCreateDTO;
import com.foodtraceability.entity.PurchaseStockin;
import com.foodtraceability.service.PurchaseStockinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 采购入库单控制器
 * 提供入库验收流程的API接口：创建入库单、质检、确认入库
 */
@Tag(name = "采购入库管理", description = "采购入库相关接口")
@RestController
@RequestMapping("/v1/purchase/stockins")
public class PurchaseStockinController {

    private static final Logger log = LoggerFactory.getLogger(PurchaseStockinController.class);

    private final PurchaseStockinService purchaseStockinService;

    public PurchaseStockinController(PurchaseStockinService purchaseStockinService) {
        this.purchaseStockinService = purchaseStockinService;
    }

    @Operation(summary = "分页查询入库单列表")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Result<Page<PurchaseStockin>> getStockinPage(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") int current,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "关联的订单ID") @RequestParam(required = false) Long orderId,
            @Parameter(description = "入库状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "入库单号") @RequestParam(required = false) String stockinNo,
            @Parameter(description = "采购订单号") @RequestParam(required = false) String orderNo,
            @Parameter(description = "供应商名称") @RequestParam(required = false) String supplierName) {
        try {
            Page<PurchaseStockin> result = purchaseStockinService.getStockinPage(
                    current, size, orderId, status, stockinNo, orderNo, supplierName);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询入库单列表失败", e);
            return Result.error("查询入库单列表失败");
        }
    }

    @Operation(summary = "获取入库单详情")
    @GetMapping("/{stockinId}")
    @PreAuthorize("isAuthenticated()")
    public Result<PurchaseStockin> getStockinDetail(@PathVariable Long stockinId) {
        try {
            PurchaseStockin stockin = purchaseStockinService.getStockinDetail(stockinId);
            return Result.success(stockin);
        } catch (Exception e) {
            log.error("获取入库单详情失败，ID：{}", stockinId, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "获取入库单详情失败");
        }
    }

    @Operation(summary = "创建入库单")
    @PostMapping
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager', 'warehouse_keeper')")
    public Result<PurchaseStockin> createStockin(@Valid @RequestBody PurchaseStockinCreateDTO createDTO) {
        try {
            PurchaseStockin stockin = purchaseStockinService.createStockin(createDTO);
            return Result.success(stockin);
        } catch (Exception e) {
            log.error("创建入库单失败", e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "创建入库单失败");
        }
    }

    @Operation(summary = "质检操作")
    @PutMapping("/{stockinId}/quality-check")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager', 'qc_user')")
    public Result<PurchaseStockin> qualityCheck(@PathVariable Long stockinId,
                                                  @Valid @RequestBody PurchaseQualityCheckDTO qualityCheckDTO) {
        try {
            PurchaseStockin stockin = purchaseStockinService.qualityCheck(stockinId, qualityCheckDTO);
            return Result.success(stockin);
        } catch (Exception e) {
            log.error("入库单质检失败，ID：{}", stockinId, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "质检操作失败");
        }
    }

    @Operation(summary = "确认入库")
    @PutMapping("/{stockinId}/confirm")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager', 'warehouse_keeper')")
    public Result<PurchaseStockin> confirmStockin(@PathVariable Long stockinId) {
        try {
            PurchaseStockin stockin = purchaseStockinService.confirmStockin(stockinId);
            return Result.success(stockin);
        } catch (Exception e) {
            log.error("确认入库失败，ID：{}", stockinId, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "确认入库失败");
        }
    }

    @Operation(summary = "作废入库单", description = "作废已入库的入库单，联动回滚库存、订单已收货数量及未付款应付账款")
    @PostMapping("/{stockinId}/void")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager', 'warehouse_keeper')")
    public Result<PurchaseStockin> voidStockin(@PathVariable Long stockinId,
                                                @RequestBody(required = false) java.util.Map<String, Object> body) {
        try {
            String remark = body != null && body.get("remark") != null ? String.valueOf(body.get("remark")) : null;
            Long voidBy = body != null && body.get("voidBy") != null ? Long.valueOf(body.get("voidBy").toString()) : null;
            PurchaseStockin stockin = purchaseStockinService.voidStockin(stockinId, remark, voidBy);
            return Result.success(stockin, "作废成功");
        } catch (Exception e) {
            log.error("作废入库单失败，ID：{}", stockinId, e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "作废入库单失败");
        }
    }

    /**
     * 更新入库单
     */
    @Operation(summary = "更新入库单", description = "更新采购收货入库单信息")
    @PutMapping("/{stockinId}")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager', 'purchaser', 'warehouse_manager')")
    public Result<PurchaseStockin> updateStockin(
            @PathVariable Long stockinId,
            @RequestBody Map<String, Object> data) {
        try {
            log.info("更新入库单：{}", stockinId);
            PurchaseStockin stockin = purchaseStockinService.getById(stockinId);
            if (stockin == null) {
                return Result.error("入库单不存在");
            }
            // 简化方案：无 UpdateDTO，从 Map 中提取可更新字段
            Object qualityRemark = data.get("qualityRemark");
            if (qualityRemark != null) {
                stockin.setQualityRemark(String.valueOf(qualityRemark));
            }
            Object status = data.get("status");
            if (status != null) {
                stockin.setStatus(Integer.valueOf(status.toString()));
            }
            Object stockinType = data.get("stockinType");
            if (stockinType != null) {
                stockin.setStockinType(Integer.valueOf(stockinType.toString()));
            }
            Object stockinDate = data.get("stockinDate");
            if (stockinDate != null) {
                stockin.setStockinDate(java.time.LocalDate.parse(String.valueOf(stockinDate)));
            }
            purchaseStockinService.updateById(stockin);
            return Result.success(stockin, "更新成功");
        } catch (Exception e) {
            log.error("更新入库单失败", e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "更新入库单失败");
        }
    }

    /**
     * 删除入库单
     */
    @Operation(summary = "删除入库单", description = "删除采购收货入库单")
    @DeleteMapping("/{stockinId}")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager')")
    public Result<Void> deleteStockin(@PathVariable Long stockinId) {
        try {
            log.info("删除入库单：{}", stockinId);
            boolean success = purchaseStockinService.deleteStockin(stockinId);
            if (!success) {
                return Result.error("删除失败，入库单可能不存在");
            }
            return Result.success(null, "删除成功");
        } catch (Exception e) {
            log.error("删除入库单失败", e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "删除入库单失败");
        }
    }

    /**
     * 开始检验
     */
    @Operation(summary = "开始检验", description = "将入库单状态置为检验中")
    @PutMapping("/{stockinId}/start-inspection")
    @PreAuthorize("hasAnyRole('admin', 'purchase_manager', 'warehouse_manager')")
    public Result<PurchaseStockin> startInspection(@PathVariable Long stockinId) {
        try {
            log.info("开始检验入库单：{}", stockinId);
            PurchaseStockin stockin = purchaseStockinService.getById(stockinId);
            if (stockin == null) {
                return Result.error("入库单不存在");
            }
            // 设置质检结果为"待检/检验中"（3表示待检）
            stockin.setQualityCheckResult(3);
            purchaseStockinService.updateById(stockin);
            return Result.success(stockin, "开始检验成功");
        } catch (Exception e) {
            log.error("开始检验失败", e);
            return Result.error(e.getMessage() != null ? e.getMessage() : "开始检验失败");
        }
    }
}
