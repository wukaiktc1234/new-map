package com.foodtraceability.controller.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.service.finance.FinanceAuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 财务审计日志控制器
 * 提供财务操作审计日志的查询功能，审计日志仅允许INSERT，禁止UPDATE和DELETE
 */
@Tag(name = "财务审计日志", description = "财务操作审计日志的查询")
@RestController
@RequestMapping("/v1/finance/audit-logs")
public class FinanceAuditLogController {

    private final FinanceAuditLogService financeAuditLogService;

    public FinanceAuditLogController(FinanceAuditLogService financeAuditLogService) {
        this.financeAuditLogService = financeAuditLogService;
    }

    @Operation(summary = "获取审计日志详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:audit-log:view')")
    public Result<FinanceAuditLogVO> getDetail(@PathVariable Long id) {
        return Result.success(financeAuditLogService.getDetail(id));
    }

    @Operation(summary = "分页查询审计日志", description = "支持按操作类型、操作人、时间范围等条件筛选")
    @GetMapping
    @PreAuthorize("hasAuthority('finance:audit-log:view')")
    public Result<IPage<FinanceAuditLogVO>> getPage(FinanceAuditLogQueryDTO query) {
        return Result.success(financeAuditLogService.getPage(query));
    }
}
