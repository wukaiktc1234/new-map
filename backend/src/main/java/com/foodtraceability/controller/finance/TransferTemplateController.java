package com.foodtraceability.controller.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.service.finance.TransferTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 结转模板控制器
 * 管理期末结转模板，支持损益结转、定期计提等自动化结转配置
 */
@Tag(name = "结转模板管理", description = "期末结转模板的增删改查及启用/禁用管理")
@RestController
@RequestMapping("/v1/finance/transfer-templates")
public class TransferTemplateController {

    private final TransferTemplateService transferTemplateService;

    public TransferTemplateController(TransferTemplateService transferTemplateService) {
        this.transferTemplateService = transferTemplateService;
    }

    @Operation(summary = "创建结转模板", description = "新增期末结转模板，配置源科目、目标科目及金额表达式")
    @PostMapping
    public Result<TransferTemplateVO> create(@Valid @RequestBody TransferTemplateCreateDTO dto) {
        return Result.success(transferTemplateService.create(dto));
    }

    @Operation(summary = "更新结转模板")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @Valid @RequestBody TransferTemplateUpdateDTO dto) {
        return Result.success(transferTemplateService.update(id, dto));
    }

    @Operation(summary = "删除结转模板", description = "逻辑删除指定结转模板")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(transferTemplateService.delete(id));
    }

    @Operation(summary = "获取结转模板详情")
    @GetMapping("/{id}")
    public Result<TransferTemplateVO> getDetail(@PathVariable Long id) {
        return Result.success(transferTemplateService.getDetail(id));
    }

    @Operation(summary = "分页查询结转模板", description = "支持按模板类型等条件筛选")
    @GetMapping
    public Result<IPage<TransferTemplateVO>> getPage(TransferTemplateQueryDTO query) {
        return Result.success(transferTemplateService.getPage(query));
    }

    @Operation(summary = "启用/禁用结转模板", description = "切换结转模板的启用状态")
    @PutMapping("/{id}/toggle-enabled")
    public Result<Boolean> toggleEnabled(@PathVariable Long id, @RequestParam Boolean enabled) {
        return Result.success(transferTemplateService.toggleEnabled(id, enabled));
    }
}
