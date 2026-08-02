package com.foodtraceability.controller.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.service.finance.FinanceRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 收支流水控制器
 * 提供收支记录的CRUD和审批功能
 */
@Tag(name = "收支流水管理", description = "企业收入、支出和转账记录的管理")
@RestController
@RequestMapping("/v1/finance/records")
public class RecordController {

    private final FinanceRecordService recordService;

    public RecordController(FinanceRecordService recordService) {
        this.recordService = recordService;
    }

    @Operation(summary = "创建收支记录", description = "创建新的收支记录，默认待审批状态")
    @PostMapping
    @PreAuthorize("hasAuthority('finance:record:create')")
    public Result<FinanceRecordVO> create(@Valid @RequestBody FinanceRecordCreateDTO dto) {
        return Result.success(recordService.create(dto));
    }

    @Operation(summary = "更新收支记录", description = "仅待审批状态的记录可修改")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:record:update')")
    public Result<Boolean> update(@PathVariable Long id, @Valid @RequestBody FinanceRecordUpdateDTO dto) {
        dto.setRecordId(id);
        return Result.success(recordService.update(dto));
    }

    @Operation(summary = "获取收支记录详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:record:view')")
    public Result<FinanceRecordVO> getDetail(@PathVariable Long id) {
        return Result.success(recordService.getDetail(id));
    }

    @Operation(summary = "分页查询收支记录", description = "支持按类型/类别/日期等条件筛选")
    @GetMapping
    @PreAuthorize("hasAuthority('finance:record:view')")
    public Result<IPage<FinanceRecordVO>> getPage(FinanceRecordQueryDTO query) {
        return Result.success(recordService.getPage(query));
    }

    @Operation(summary = "审批收支记录", description = "通过或驳回收支记录")
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('finance:record:approve')")
    public Result<Boolean> approve(@PathVariable Long id, @RequestParam boolean approved) {
        return Result.success(recordService.approve(id, approved));
    }
}
