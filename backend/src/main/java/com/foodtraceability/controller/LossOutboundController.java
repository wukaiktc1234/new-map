package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.LossOutboundCreateDTO;
import com.foodtraceability.entity.LossOutbound;
import com.foodtraceability.service.LossOutboundService;
import com.foodtraceability.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/loss-outbound")
@Tag(name = "报损出库管理")
public class LossOutboundController {


    public LossOutboundController(LossOutboundService lossOutboundService) {
        this.lossOutboundService = lossOutboundService;
    }

    private final LossOutboundService lossOutboundService;

    @GetMapping("/page")
    @Operation(summary = "分页查询报损单列表")
    @PreAuthorize("hasAuthority('loss-outbound:list') or hasAuthority('*')")
    public Result<IPage<LossOutbound>> getPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String lossNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        IPage<LossOutbound> result = lossOutboundService.getPage(page, pageSize, lossNo, status, startDate, endDate);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询报损单详情")
    @PreAuthorize("hasAuthority('loss-outbound:view') or hasAuthority('*')")
    public Result<LossOutbound> getById(@PathVariable Long id) {
        LossOutbound lossOutbound = lossOutboundService.getById(id);
        if (lossOutbound == null) {
            return Result.error(404, "报损单不存在");
        }
        return Result.success(lossOutbound);
    }

    @PostMapping
    @Operation(summary = "创建报损单")
    @PreAuthorize("hasAuthority('loss-outbound:create') or hasAuthority('*')")
    public Result<LossOutbound> create(@Valid @RequestBody LossOutboundCreateDTO dto) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        String operatorName = SecurityUtils.getCurrentUsername();
        LossOutbound result = lossOutboundService.create(dto, operatorId, operatorName);
        return Result.success(result);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "审核报损单")
    @PreAuthorize("hasAuthority('loss-outbound:approve') or hasAuthority('*')")
    public Result<LossOutbound> approve(@PathVariable Long id) {
        Long approverId = SecurityUtils.getCurrentUserId();
        String approverName = SecurityUtils.getCurrentUsername();
        LossOutbound result = lossOutboundService.approve(id, approverId, approverName);
        return Result.success(result);
    }
}
