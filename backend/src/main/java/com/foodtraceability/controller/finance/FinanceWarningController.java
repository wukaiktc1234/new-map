package com.foodtraceability.controller.finance;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.finance.FinanceWarningCreateDTO;
import com.foodtraceability.dto.finance.FinanceWarningProcessDTO;
import com.foodtraceability.dto.finance.FinanceWarningQueryDTO;
import com.foodtraceability.dto.finance.FinanceWarningStatsVO;
import com.foodtraceability.dto.finance.FinanceWarningVO;
import com.foodtraceability.service.finance.FinanceWarningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 财务预警管理Controller
 *
 * <p>Sprint F-020：财务风险预警管理，支持预警创建、处理流转与统计分析。</p>
 *
 * <p>路径前缀：/v1/finance/warnings</p>
 *
 * <p>状态机：UNHANDLED(未处理) → HANDLING(处理中) → RESOLVED(已解决)。</p>
 */
@Tag(name = "财务预警管理", description = "财务风险预警的创建、处理流转与统计分析")
@RestController
@RequestMapping("/v1/finance/warnings")
public class FinanceWarningController {

    private final FinanceWarningService financeWarningService;

    public FinanceWarningController(FinanceWarningService financeWarningService) {
        this.financeWarningService = financeWarningService;
    }

    /**
     * 分页查询预警列表
     *
     * @param query 查询条件
     * @return 分页结果
     */
    @Operation(summary = "分页查询预警列表", description = "支持按预警类型/级别/状态/日期范围筛选")
    @GetMapping
    @PreAuthorize("hasAuthority('finance:warning:view')")
    public Result<IPage<FinanceWarningVO>> queryPage(FinanceWarningQueryDTO query) {
        return Result.success(financeWarningService.queryPage(query));
    }

    /**
     * 预警统计
     *
     * @param startDate 起始日期（yyyy-MM-dd）
     * @param endDate   结束日期（yyyy-MM-dd）
     * @return 统计结果
     */
    @Operation(summary = "预警统计", description = "统计总数/待处理/处理中/已解决数量，支持日期范围过滤")
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('finance:warning:view')")
    public Result<FinanceWarningStatsVO> getStats(
            @Parameter(description = "起始日期（yyyy-MM-dd）")
            @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期（yyyy-MM-dd）")
            @RequestParam(required = false) String endDate) {
        return Result.success(financeWarningService.getStats(startDate, endDate));
    }

    /**
     * 获取预警详情
     *
     * @param id 预警ID
     * @return 预警VO
     */
    @Operation(summary = "获取预警详情", description = "根据ID查询预警完整信息")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:warning:view')")
    public Result<FinanceWarningVO> getDetail(
            @Parameter(description = "预警ID", required = true)
            @PathVariable Long id) {
        return Result.success(financeWarningService.getDetail(id));
    }

    /**
     * 创建预警记录
     *
     * @param dto 创建DTO
     * @return 创建后的预警VO
     */
    @Operation(summary = "创建预警记录", description = "创建新的财务预警，初始状态为 UNHANDLED")
    @PostMapping
    @PreAuthorize("hasAuthority('finance:warning:create')")
    public Result<FinanceWarningVO> create(@Valid @RequestBody FinanceWarningCreateDTO dto) {
        return Result.success(financeWarningService.create(dto));
    }

    /**
     * 处理预警
     *
     * @param id  预警ID
     * @param dto 处理DTO
     * @return 操作结果
     */
    @Operation(summary = "处理预警", description = "流转预警状态为 HANDLING 或 RESOLVED，记录处理人与处理结果")
    @PutMapping("/{id}/process")
    @PreAuthorize("hasAuthority('finance:warning:process')")
    public Result<Boolean> process(
            @Parameter(description = "预警ID", required = true)
            @PathVariable Long id,
            @Valid @RequestBody FinanceWarningProcessDTO dto) {
        return Result.success(financeWarningService.process(id, dto));
    }

    /**
     * 删除预警（逻辑删除）
     *
     * @param id 预警ID
     * @return 操作结果
     */
    @Operation(summary = "删除预警", description = "逻辑删除预警记录")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('finance:warning:delete')")
    public Result<Boolean> delete(
            @Parameter(description = "预警ID", required = true)
            @PathVariable Long id) {
        return Result.success(financeWarningService.delete(id));
    }
}
