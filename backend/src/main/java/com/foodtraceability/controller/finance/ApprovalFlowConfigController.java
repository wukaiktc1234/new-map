package com.foodtraceability.controller.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.finance.*;
import com.foodtraceability.service.finance.ApprovalFlowConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 审批流配置控制器
 * 管理财务单据的审批流配置，支持按单据类型查询审批流
 */
@Tag(name = "审批流配置管理", description = "审批流配置的增删改查及启用/禁用管理")
@RestController
@RequestMapping("/v1/finance/approval-flow-configs")
public class ApprovalFlowConfigController {

    private final ApprovalFlowConfigService approvalFlowConfigService;

    public ApprovalFlowConfigController(ApprovalFlowConfigService approvalFlowConfigService) {
        this.approvalFlowConfigService = approvalFlowConfigService;
    }

    @Operation(summary = "创建审批流配置", description = "新增审批流配置，需指定单据类型及审批节点")
    @PostMapping
    @PreAuthorize("hasAuthority('finance:approval-flow-config:manage')")
    public Result<ApprovalFlowConfigVO> create(@Valid @RequestBody ApprovalFlowConfigCreateDTO dto) {
        return Result.success(approvalFlowConfigService.create(dto));
    }

    @Operation(summary = "更新审批流配置")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:approval-flow-config:manage')")
    public Result<Boolean> update(@PathVariable Long id, @Valid @RequestBody ApprovalFlowConfigUpdateDTO dto) {
        return Result.success(approvalFlowConfigService.update(id, dto));
    }

    @Operation(summary = "删除审批流配置", description = "逻辑删除指定审批流配置")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:approval-flow-config:manage')")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(approvalFlowConfigService.delete(id));
    }

    @Operation(summary = "获取审批流配置详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:approval-flow-config:view')")
    public Result<ApprovalFlowConfigVO> getDetail(@PathVariable Long id) {
        return Result.success(approvalFlowConfigService.getDetail(id));
    }

    @Operation(summary = "分页查询审批流配置", description = "支持按单据类型等条件筛选")
    @GetMapping
    @PreAuthorize("hasAuthority('finance:approval-flow-config:view')")
    public Result<IPage<ApprovalFlowConfigVO>> getPage(ApprovalFlowConfigQueryDTO query) {
        return Result.success(approvalFlowConfigService.getPage(query));
    }

    @Operation(summary = "按单据类型查询审批流配置", description = "根据单据类型获取对应的审批流配置")
    @GetMapping("/by-document-type/{documentType}")
    @PreAuthorize("hasAuthority('finance:approval-flow-config:view')")
    public Result<ApprovalFlowConfigVO> getByDocumentType(@PathVariable String documentType) {
        return Result.success(approvalFlowConfigService.getByDocumentType(documentType));
    }

    @Operation(summary = "启用/禁用审批流配置", description = "切换审批流配置的启用状态")
    @PutMapping("/{id}/toggle-enabled")
    @PreAuthorize("hasAuthority('finance:approval-flow-config:manage')")
    public Result<Boolean> toggleEnabled(@PathVariable Long id, @RequestParam Boolean enabled) {
        return Result.success(approvalFlowConfigService.toggleEnabled(id, enabled));
    }
}
