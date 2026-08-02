package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.trace.*;
import com.foodtraceability.service.trace.PurchaseLedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 进货台账管理控制器
 */
@Tag(name = "进货台账管理", description = "电子化进货台账的CRUD操作")
@RestController
@RequestMapping("/v1/purchase-ledger")
public class PurchaseLedgerController {

    private final PurchaseLedgerService purchaseLedgerService;

    public PurchaseLedgerController(PurchaseLedgerService purchaseLedgerService) {
        this.purchaseLedgerService = purchaseLedgerService;
    }

    @Operation(summary = "创建进货台账", description = "新增一条进货台账记录")
    @PostMapping
    public Result<PurchaseLedgerVO> create(@Valid @RequestBody PurchaseLedgerCreateDTO dto) {
        PurchaseLedgerVO vo = purchaseLedgerService.create(dto);
        return Result.success(vo, "台账创建成功");
    }

    @Operation(summary = "分页查询台账列表", description = "支持多条件筛选")
    @GetMapping("/page")
    public Result<IPage<PurchaseLedgerVO>> queryPage(PurchaseLedgerQueryDTO queryDTO) {
        IPage<PurchaseLedgerVO> page = purchaseLedgerService.queryPage(queryDTO);
        return Result.success(page);
    }

    @Operation(summary = "查询台账详情", description = "根据ID查询单条台账记录")
    @GetMapping("/{id}")
    public Result<PurchaseLedgerVO> getDetail(@PathVariable Long id) {
        PurchaseLedgerVO vo = purchaseLedgerService.getDetailById(id);
        return Result.success(vo);
    }

    @Operation(summary = "更新质检结果", description = "更新台账记录的质检结果")
    @PutMapping("/{id}/quality-result")
    public Result<Boolean> updateQualityResult(@PathVariable Long id,
                                                @RequestParam Integer result) {
        // TODO: 从认证上下文获取质检人ID
        Long inspectorId = 1L;
        boolean success = purchaseLedgerService.updateQualityResult(id, result, inspectorId);
        return Result.success(success, success ? "质检结果更新成功" : "更新失败");
    }
}
