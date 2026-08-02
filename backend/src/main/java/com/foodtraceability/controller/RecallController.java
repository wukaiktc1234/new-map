package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.trace.BatchRecallCreateDTO;
import com.foodtraceability.dto.trace.BatchRecallResultVO;
import com.foodtraceability.dto.trace.RecallAnalyzeQueryDTO;
import com.foodtraceability.dto.trace.RecallCreateDTO;
import com.foodtraceability.dto.trace.RecallQueryResultVO;
import com.foodtraceability.dto.trace.RecallStatisticsVO;
import com.foodtraceability.dto.trace.TraceCodeCreateDTO;
import com.foodtraceability.dto.trace.TraceCodeQueryDTO;
import com.foodtraceability.dto.trace.TraceCodeUpdateDTO;
import com.foodtraceability.dto.trace.TraceCodeVO;
import com.foodtraceability.entity.Member;
import com.foodtraceability.entity.OrderNew;
import com.foodtraceability.entity.RecallRecord;
import com.foodtraceability.service.trace.RecallService;
import com.foodtraceability.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 召回管理控制器
 * 提供反向召回相关的高级查询和管理功能
 * 仅负责请求路由、参数校验、调用Service和返回Result
 */
@Tag(name = "召回管理", description = "问题产品的反向追溯和召回管理")
@RestController
@RequestMapping("/v1/recalls")
public class RecallController {

    private final RecallService recallService;

    public RecallController(RecallService recallService) {
        this.recallService = recallService;
    }

    /** 召回影响范围分析（完整版） */
    @Operation(summary = "召回影响范围分析", description = "全面分析问题产品的影响范围，包括受影响的菜品和订单")
    @PostMapping("/analyze")
    @PreAuthorize("hasAuthority('trace:recall')")
    public Result<RecallQueryResultVO> analyzeRecall(@Valid @RequestBody RecallAnalyzeQueryDTO request) {
        RecallQueryResultVO result = recallService.analyzeRecall(request);
        return Result.success(result);
    }

    /** 批量召回 */
    @Operation(summary = "批量召回", description = "批量将多个追溯码标记为已召回")
    @PostMapping("/batch-recall")
    @PreAuthorize("hasAuthority('trace:recall')")
    public Result<BatchRecallResultVO> batchRecall(@Valid @RequestBody BatchRecallCreateDTO request) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        String operatorName = SecurityUtils.getCurrentUsername();
        BatchRecallResultVO result = recallService.batchRecall(request, operatorId, operatorName);
        return Result.success(result, "批量召回完成：成功" + result.getSuccessCount() + "条，失败" + result.getFailCount() + "条");
    }

    /** 召回统计概览 */
    @Operation(summary = "召回统计概览", description = "获取召回相关的统计数据")
    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('trace:recall')")
    public Result<RecallStatisticsVO> getStatistics() {
        RecallStatisticsVO statistics = recallService.getStatistics();
        return Result.success(statistics);
    }

    /** 查询批次受影响的所有订单（DF-032：订单级追溯） */
    @Operation(summary = "查询受影响订单列表", description = "根据批次号查询所有受影响的订单，用于召回订单级追溯")
    @GetMapping("/affected-orders/{batchNo}")
    @PreAuthorize("hasAuthority('trace:recall')")
    public Result<List<OrderNew>> findAffectedOrders(@PathVariable String batchNo) {
        List<OrderNew> orders = recallService.findAffectedOrders(batchNo);
        return Result.success(orders);
    }

    /** 查询批次受影响的所有客户（DF-034：受影响客户列表） */
    @Operation(summary = "查询受影响客户列表", description = "根据批次号查询所有受影响客户，用于召回通知和客户告知")
    @GetMapping("/affected-customers/{batchNo}")
    @PreAuthorize("hasAuthority('trace:recall')")
    public Result<List<Member>> findAffectedCustomers(@PathVariable String batchNo) {
        List<Member> customers = recallService.findAffectedCustomers(batchNo);
        return Result.success(customers);
    }

    /** 反向追溯：从溯源码查询所有受影响订单和客户（DF-033：反向追溯） */
    @Operation(summary = "溯源码反向追溯", description = "从溯源码反向追溯至批次号，再查询所有受影响订单和客户")
    @GetMapping("/reverse-trace/{traceCode}")
    @PreAuthorize("hasAuthority('trace:recall')")
    public Result<RecallQueryResultVO> reverseTrace(@PathVariable String traceCode) {
        RecallQueryResultVO result = recallService.reverseTrace(traceCode);
        return Result.success(result);
    }

    /** 创建召回记录（审计留痕，食品安全法合规要求） */
    @Operation(summary = "创建召回记录", description = "发起一次召回并写入审计记录，包含受影响订单/客户数快照")
    @PostMapping("/create-record")
    @PreAuthorize("hasAuthority('trace:recall')")
    public Result<RecallRecord> createRecallRecord(@Valid @RequestBody RecallCreateDTO dto) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        String operatorName = SecurityUtils.getCurrentUsername();
        RecallRecord record = recallService.createRecallRecord(dto, operatorId, operatorName);
        return Result.success(record, "召回记录创建成功");
    }

    /** 召回记录列表（分页） */
    @Operation(summary = "召回记录列表", description = "分页查询召回记录列表")
    @GetMapping
    @PreAuthorize("hasAuthority('trace:query')")
    public Result<IPage<TraceCodeVO>> list(TraceCodeQueryDTO queryDTO) {
        IPage<TraceCodeVO> page = recallService.queryPage(queryDTO);
        return Result.success(page);
    }

    /** 召回记录详情 */
    @Operation(summary = "召回记录详情", description = "根据ID查询召回记录详情（含完整追溯链）")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('trace:query')")
    public Result<TraceCodeVO> detail(@PathVariable Long id) {
        TraceCodeVO vo = recallService.getDetailById(id);
        return Result.success(vo);
    }

    /** 创建召回记录 */
    @Operation(summary = "创建召回记录", description = "新增召回记录并初始化追溯链")
    @PostMapping
    @PreAuthorize("hasAuthority('trace:create')")
    public Result<TraceCodeVO> create(@Valid @RequestBody TraceCodeCreateDTO dto) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        String operatorName = SecurityUtils.getCurrentUsername();
        TraceCodeVO vo = recallService.create(dto, operatorId, operatorName);
        return Result.success(vo, "召回记录创建成功");
    }

    /** 更新召回记录 */
    @Operation(summary = "更新召回记录", description = "根据ID更新召回记录")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('trace:update')")
    public Result<TraceCodeVO> update(@PathVariable Long id, @Valid @RequestBody TraceCodeUpdateDTO dto) {
        TraceCodeVO vo = recallService.update(id, dto);
        return Result.success(vo, "召回记录更新成功");
    }

    /** 删除召回记录（逻辑删除） */
    @Operation(summary = "删除召回记录", description = "逻辑删除召回记录")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('trace:delete')")
    public Result<Boolean> delete(@PathVariable Long id) {
        boolean result = recallService.delete(id);
        return Result.success(result, result ? "删除成功" : "删除失败");
    }
}
