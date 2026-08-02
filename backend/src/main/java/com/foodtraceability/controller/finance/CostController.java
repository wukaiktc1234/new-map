package com.foodtraceability.controller.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.service.finance.CostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 成本记录控制器
 * 提供成本记录的CRUD和统计分析功能
 */
@Tag(name = "成本记录管理", description = "企业各项成本支出的记录与分析")
@RestController
@RequestMapping("/v1/finance/costs")
public class CostController {

    private final CostService costService;

    public CostController(CostService costService) {
        this.costService = costService;
    }

    @Operation(summary = "创建成本记录", description = "新增成本支出记录")
    @PostMapping
    @PreAuthorize("hasAuthority('finance:cost:create')")
    public Result<CostRecordVO> create(@Valid @RequestBody CostRecordCreateDTO dto) {
        return Result.success(costService.create(dto));
    }

    @Operation(summary = "更新成本记录")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:cost:update')")
    public Result<Boolean> update(@PathVariable Long id, @Valid @RequestBody CostRecordUpdateDTO dto) {
        dto.setCostId(id);
        return Result.success(costService.update(dto));
    }

    @Operation(summary = "获取成本记录详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:cost:query')")
    public Result<CostRecordVO> getDetail(@PathVariable Long id) {
        return Result.success(costService.getDetail(id));
    }

    @Operation(summary = "分页查询成本记录", description = "支持按类型/期间/成本中心等条件筛选")
    @GetMapping
    @PreAuthorize("hasAuthority('finance:cost:query')")
    public Result<IPage<CostRecordVO>> getPage(CostRecordQueryDTO query) {
        return Result.success(costService.getPage(query));
    }

    @Operation(summary = "按期间汇总成本", description = "统计指定期间内各类型成本的总额")
    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('finance:cost:query')")
    public Result<Map<Integer, Long>> summarizeByPeriod(@RequestParam String period) {
        return Result.success(costService.summarizeByPeriod(period));
    }
}
