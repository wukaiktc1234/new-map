package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.trace.InspectionCreateDTO;
import com.foodtraceability.dto.trace.InspectionQueryDTO;
import com.foodtraceability.dto.trace.InspectionStatisticsVO;
import com.foodtraceability.dto.trace.InspectionUpdateDTO;
import com.foodtraceability.dto.trace.InspectionVO;
import com.foodtraceability.service.trace.FoodInspectionService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 检验记录管理控制器
 * 提供食品检验记录的CRUD、按批次/供应商查询、报告上传和统计功能
 */
@Tag(name = "检验记录管理", description = "食品检验记录的CRUD和统计")
@RestController
@RequestMapping("/v1/inspections")
public class InspectionController {

    private final FoodInspectionService foodInspectionService;

    public InspectionController(FoodInspectionService foodInspectionService) {
        this.foodInspectionService = foodInspectionService;
    }

    /** 检验记录列表（分页） */
    @Operation(summary = "检验记录列表", description = "分页查询检验记录")
    @GetMapping
    @PreAuthorize("hasAuthority('trace:query')")
    public Result<IPage<InspectionVO>> list(InspectionQueryDTO queryDTO) {
        IPage<InspectionVO> page = foodInspectionService.queryPage(queryDTO);
        return Result.success(page);
    }

    /** 检验记录详情 */
    @Operation(summary = "检验记录详情", description = "根据ID查询检验记录详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('trace:query')")
    public Result<InspectionVO> detail(@PathVariable Long id) {
        InspectionVO vo = foodInspectionService.getDetailById(id);
        return Result.success(vo);
    }

    /** 创建检验记录 */
    @Operation(summary = "创建检验记录", description = "新增检验记录")
    @PostMapping
    @PreAuthorize("hasAuthority('trace:create')")
    public Result<InspectionVO> create(@Valid @RequestBody InspectionCreateDTO dto) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        String operatorName = SecurityUtils.getCurrentUsername();
        InspectionVO vo = foodInspectionService.create(dto, operatorId, operatorName);
        return Result.success(vo, "检验记录创建成功");
    }

    /** 更新检验记录 */
    @Operation(summary = "更新检验记录", description = "根据ID更新检验记录")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('trace:update')")
    public Result<InspectionVO> update(@PathVariable Long id, @Valid @RequestBody InspectionUpdateDTO dto) {
        dto.setInspectionId(id);
        InspectionVO vo = foodInspectionService.update(dto);
        return Result.success(vo, "检验记录更新成功");
    }

    /** 删除检验记录（逻辑删除） */
    @Operation(summary = "删除检验记录", description = "逻辑删除检验记录")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('trace:delete')")
    public Result<Boolean> delete(@PathVariable Long id) {
        boolean result = foodInspectionService.delete(id);
        return Result.success(result, result ? "删除成功" : "删除失败");
    }

    /** 按批次查询检验记录 */
    @Operation(summary = "按批次查询检验记录", description = "根据批次号查询关联的检验记录")
    @GetMapping("/by-batch/{batchNo}")
    @PreAuthorize("hasAuthority('trace:query')")
    public Result<List<InspectionVO>> queryByBatch(@PathVariable String batchNo) {
        List<InspectionVO> list = foodInspectionService.queryByBatchNo(batchNo);
        return Result.success(list);
    }

    /** 按供应商查询检验记录 */
    @Operation(summary = "按供应商查询检验记录", description = "根据供应商ID分页查询检验记录")
    @GetMapping("/by-supplier/{supplierId}")
    @PreAuthorize("hasAuthority('trace:query')")
    public Result<IPage<InspectionVO>> queryBySupplier(@PathVariable Long supplierId, InspectionQueryDTO queryDTO) {
        IPage<InspectionVO> page = foodInspectionService.queryBySupplier(supplierId, queryDTO);
        return Result.success(page);
    }

    /** 上传检验报告 */
    @Operation(summary = "上传检验报告", description = "更新检验记录的报告URL")
    @PostMapping("/{id}/upload-report")
    @PreAuthorize("hasAuthority('trace:update')")
    public Result<Boolean> uploadReport(@PathVariable Long id, @RequestParam String reportUrl) {
        boolean result = foodInspectionService.uploadReport(id, reportUrl);
        return Result.success(result, result ? "报告上传成功" : "报告上传失败");
    }

    /** 检验统计 */
    @Operation(summary = "检验统计", description = "按条件统计检验记录数据")
    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('trace:query')")
    public Result<InspectionStatisticsVO> statistics(InspectionQueryDTO queryDTO) {
        InspectionStatisticsVO vo = foodInspectionService.getStatistics(queryDTO);
        return Result.success(vo);
    }
}
