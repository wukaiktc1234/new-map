package com.foodtraceability.controller.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.service.finance.StandardCostCardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 标准成本卡控制器
 * 管理菜品的标准成本卡，用于成本控制与差异分析
 */
@Tag(name = "标准成本卡管理", description = "菜品标准成本卡的增删改查及预警状态管理")
@RestController
@RequestMapping("/v1/finance/standard-cost-cards")
public class StandardCostCardController {

    private final StandardCostCardService standardCostCardService;

    public StandardCostCardController(StandardCostCardService standardCostCardService) {
        this.standardCostCardService = standardCostCardService;
    }

    @Operation(summary = "创建标准成本卡", description = "新增菜品标准成本卡，配置标准成本与损耗系数")
    @PostMapping
    public Result<StandardCostCardVO> create(@Valid @RequestBody StandardCostCardCreateDTO dto) {
        return Result.success(standardCostCardService.create(dto));
    }

    @Operation(summary = "更新标准成本卡")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @Valid @RequestBody StandardCostCardUpdateDTO dto) {
        return Result.success(standardCostCardService.update(id, dto));
    }

    @Operation(summary = "删除标准成本卡", description = "逻辑删除指定标准成本卡")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(standardCostCardService.delete(id));
    }

    @Operation(summary = "获取标准成本卡详情")
    @GetMapping("/{id}")
    public Result<StandardCostCardVO> getDetail(@PathVariable Long id) {
        return Result.success(standardCostCardService.getDetail(id));
    }

    @Operation(summary = "分页查询标准成本卡", description = "支持按菜品名称等条件筛选")
    @GetMapping
    public Result<IPage<StandardCostCardVO>> getPage(StandardCostCardQueryDTO query) {
        return Result.success(standardCostCardService.getPage(query));
    }

    @Operation(summary = "更新预警状态", description = "更新标准成本卡的预警状态，用于成本异常监控")
    @PutMapping("/{id}/warning-status")
    public Result<Boolean> updateWarningStatus(@PathVariable Long id, @RequestParam Integer warningStatus) {
        return Result.success(standardCostCardService.updateWarningStatus(id, warningStatus));
    }

    @Operation(summary = "按菜品ID查询标准成本卡", description = "根据菜品ID获取对应的标准成本卡信息")
    @GetMapping("/dish/{dishId}")
    public Result<StandardCostCardVO> getByDishId(@PathVariable Long dishId) {
        return Result.success(standardCostCardService.getByDishId(dishId));
    }
}
