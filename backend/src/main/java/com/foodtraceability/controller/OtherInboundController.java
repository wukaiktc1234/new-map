package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.OtherInboundCreateDTO;
import com.foodtraceability.entity.OtherInbound;
import com.foodtraceability.service.OtherInboundService;
import com.foodtraceability.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/other-inbound")
@Tag(name = "其他入库管理")
public class OtherInboundController {
    

    public OtherInboundController(OtherInboundService otherInboundService) {
        this.otherInboundService = otherInboundService;
    }

    private final OtherInboundService otherInboundService;
    
    @GetMapping("/page")
    @Operation(summary = "分页查询其他入库单")
    @PreAuthorize("hasAuthority('other-inbound:list') or hasAuthority('*')")
    public Result<IPage<OtherInbound>> getPage(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam(required = false) String inboundType,
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate
    ) {
        return Result.success(otherInboundService.getPage(page, pageSize, inboundType, startDate, endDate));
    }
    
    @PostMapping
    @Operation(summary = "创建其他入库单")
    @PreAuthorize("hasAuthority('other-inbound:create') or hasAuthority('*')")
    public Result<OtherInbound> create(@Valid @RequestBody OtherInboundCreateDTO dto) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        String operatorName = SecurityUtils.getCurrentUsername();
        return Result.success(otherInboundService.create(dto, operatorId, operatorName));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "获取其他入库单详情")
    @PreAuthorize("hasAuthority('other-inbound:view') or hasAuthority('*')")
    public Result<OtherInbound> getById(@PathVariable Long id) {
        return Result.success(otherInboundService.getById(id));
    }
}
