package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.trace.*;
import com.foodtraceability.service.trace.FoodTraceabilityService;
import com.foodtraceability.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 追溯码管理控制器
 * 系统核心API，提供追溯码的生成、查询、扫描和召回功能
 */
@Tag(name = "食品追溯码管理", description = "追溯码的生成、查询、正向追溯和反向召回")
@RestController
@RequestMapping("/v1/trace-codes")
public class TraceCodeController {

    private final FoodTraceabilityService foodTraceabilityService;

    public TraceCodeController(FoodTraceabilityService foodTraceabilityService) {
        this.foodTraceabilityService = foodTraceabilityService;
    }

    /** 查询追溯码列表 */
    @GetMapping
    @Operation(summary = "查询追溯码列表", description = "分页查询所有追溯码")
    @PreAuthorize("hasAuthority('trace:query')")
    public Result<IPage<TraceCodeVO>> list(TraceCodeQueryDTO queryDTO) {
        IPage<TraceCodeVO> page = foodTraceabilityService.queryPage(queryDTO);
        return Result.success(page);
    }

    /** 生成追溯码 */
    @PostMapping
    @Operation(summary = "生成追溯码", description = "创建新的追溯码并初始化追溯链")
    @PreAuthorize("hasAuthority('trace:create')")
    public Result<TraceCodeVO> generate(@Valid @RequestBody TraceCodeCreateDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        TraceCodeVO vo = foodTraceabilityService.generateTraceCode(dto, userId);
        return Result.success(vo, "追溯码生成成功");
    }

    /** 正向追溯查询（扫码查询） */
    @GetMapping("/{traceCode}")
    @Operation(summary = "正向追溯查询", description = "根据追溯码查询完整的追溯链信息")
    @PreAuthorize("hasAuthority('trace:query')")
    public Result<TraceCodeVO> queryByTraceCode(@PathVariable String traceCode) {
        TraceCodeVO vo = foodTraceabilityService.queryByTraceCode(traceCode);
        return Result.success(vo);
    }

    /** 分页查询追溯码列表 */
    @GetMapping("/page")
    @Operation(summary = "分页查询追溯码列表", description = "支持多条件组合查询")
    @PreAuthorize("hasAuthority('trace:query')")
    public Result<IPage<TraceCodeVO>> queryPage(TraceCodeQueryDTO queryDTO) {
        IPage<TraceCodeVO> page = foodTraceabilityService.queryPage(queryDTO);
        return Result.success(page);
    }

    /** 更新追溯码状态 */
    @PutMapping("/{id}/status")
    @Operation(summary = "更新追溯码状态", description = "更新追溯码的状态信息")
    @PreAuthorize("hasAuthority('trace:update')")
    public Result<Boolean> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        boolean result = foodTraceabilityService.updateStatus(id, status);
        return Result.success(result, result ? "状态更新成功" : "状态更新失败");
    }

    /** 执行召回操作 */
    @PostMapping("/{id}/recall")
    @Operation(summary = "执行召回", description = "将追溯码标记为已召回状态")
    @PreAuthorize("hasAuthority('trace:recall')")
    public Result<Boolean> recall(@PathVariable Long id,
                                   @RequestParam String recallReason,
                                   @RequestParam(required = false) String operatorName) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        if (operatorName == null || operatorName.isEmpty()) {
            operatorName = SecurityUtils.getCurrentUsername();
        }
        boolean result = foodTraceabilityService.executeRecall(id, recallReason, operatorId, operatorName);
        return Result.success(result, result ? "召回操作成功" : "召回操作失败");
    }

    /** 记录消费者扫码行为 */
    @PostMapping("/{traceCode}/scan")
    @Operation(summary = "记录扫码", description = "记录消费者扫码查看追溯信息的行为")
    @PreAuthorize("hasAuthority('trace:query')")
    public Result<Boolean> scan(@PathVariable String traceCode) {
        boolean result = foodTraceabilityService.recordScanEvent(traceCode);
        return Result.success(result);
    }

    /** 反向召回查询 */
    @GetMapping("/recall-query")
    @Operation(summary = "反向召回查询", description = "根据批次号/供应商/物料名查询受影响范围")
    @PreAuthorize("hasAuthority('trace:recall')")
    public Result<RecallQueryResultVO> queryRecallImpact(
            @RequestParam(required = false) String batchNo,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String targetName) {
        RecallQueryResultVO result = foodTraceabilityService.queryRecallImpact(batchNo, supplierId, targetName);
        return Result.success(result);
    }

    /** 追溯链展示 - 根据追溯码ID查询完整追溯链 */
    @GetMapping("/{id}/chain")
    @Operation(summary = "追溯链展示", description = "根据追溯码ID查询完整的追溯链信息")
    @PreAuthorize("hasAuthority('trace:query')")
    public Result<TraceCodeVO> getChain(@PathVariable Long id) {
        com.foodtraceability.entity.TraceCode entity = foodTraceabilityService.getById(id);
        if (entity == null) {
            return Result.error(404, "追溯码不存在");
        }
        TraceCodeVO vo = foodTraceabilityService.queryByTraceCode(entity.getTraceCode());
        return Result.success(vo);
    }

    /** 添加追溯链节点 */
    @PostMapping("/{id}/chain-nodes")
    @Operation(summary = "添加追溯链节点", description = "向追溯码添加新的链路环节")
    @PreAuthorize("hasAuthority('trace:update')")
    public Result<TraceChainNodeVO> addChainNode(@PathVariable Long id,
                                                  @RequestParam Integer nodeType,
                                                  @RequestParam String location,
                                                  @RequestParam(required = false) Object detailJson) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        String operatorName = SecurityUtils.getCurrentUsername();
        TraceChainNodeVO vo = foodTraceabilityService.addChainNode(id, nodeType, location, operatorId, operatorName, detailJson);
        return Result.success(vo, "追溯链节点添加成功");
    }

    /** 刷新风险等级 */
    @PostMapping("/{id}/refresh-risk")
    @Operation(summary = "刷新风险等级", description = "根据有效期等信息重新计算风险等级")
    @PreAuthorize("hasAuthority('trace:update')")
    public Result<Integer> refreshRiskLevel(@PathVariable Long id) {
        Integer level = foodTraceabilityService.refreshRiskLevel(id);
        return Result.success(level, "当前风险等级: " + getRiskLevelName(level));
    }

    private static String getRiskLevelName(Integer level) {
        if (level == null) return "未知";
        switch (level) {
            case 1: return "低风险";
            case 2: return "中风险";
            case 3: return "高风险";
            default: return "未知";
        }
    }
}
