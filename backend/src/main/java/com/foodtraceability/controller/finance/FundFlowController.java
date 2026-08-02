package com.foodtraceability.controller.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.service.finance.FundFlowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * 资金流水控制器
 * 记录银行账户的资金收支流水，支持资金监控与对账
 */
@Tag(name = "资金流水管理", description = "资金流水的创建、查询及收支统计")
@RestController
@RequestMapping("/v1/finance/fund-flows")
public class FundFlowController {

    private final FundFlowService fundFlowService;

    public FundFlowController(FundFlowService fundFlowService) {
        this.fundFlowService = fundFlowService;
    }

    @Operation(summary = "创建资金流水", description = "新增银行账户资金收支流水记录")
    @PostMapping
    public Result<FundFlowVO> create(@Valid @RequestBody FundFlowCreateDTO dto) {
        return Result.success(fundFlowService.create(dto));
    }

    @Operation(summary = "获取资金流水详情")
    @GetMapping("/{id}")
    public Result<FundFlowVO> getDetail(@PathVariable Long id) {
        return Result.success(fundFlowService.getDetail(id));
    }

    @Operation(summary = "分页查询资金流水", description = "支持按账户、方向、分类、日期等条件筛选")
    @GetMapping
    public Result<IPage<FundFlowVO>> getPage(FundFlowQueryDTO query) {
        return Result.success(fundFlowService.getPage(query));
    }

    @Operation(summary = "按账户查询资金流水", description = "分页查询指定银行账户的资金流水")
    @GetMapping("/account/{accountId}")
    public Result<IPage<FundFlowVO>> getByAccount(
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size) {
        FundFlowQueryDTO query = new FundFlowQueryDTO();
        query.setAccountId(accountId);
        query.setCurrent(current);
        query.setSize(size);
        return Result.success(fundFlowService.getPage(query));
    }

    @Operation(summary = "统计账户收支情况", description = "统计指定账户在指定期间的收入、支出、净额及笔数")
    @GetMapping("/account/{accountId}/statistics")
    public Result<Map<String, Object>> getStatistics(
            @PathVariable Long accountId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(fundFlowService.getStatistics(accountId, startDate, endDate));
    }
}
