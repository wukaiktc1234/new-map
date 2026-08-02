package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.trace.QualityRecordCreateDTO;
import com.foodtraceability.dto.trace.QualityRecordVO;
import com.foodtraceability.dto.trace.QualityStandardCreateDTO;
import com.foodtraceability.dto.trace.QualityStandardQueryDTO;
import com.foodtraceability.dto.trace.QualityStandardUpdateDTO;
import com.foodtraceability.dto.trace.QualityStandardVO;
import com.foodtraceability.service.trace.FoodQualityService;
import com.foodtraceability.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 质量追溯管理控制器
 * 提供质量记录管理、质量异常处理、质量标准维护和统计功能
 */
@Tag(name = "质量追溯管理", description = "质量记录和质量标准管理")
@RestController
@RequestMapping("/v1/quality")
public class QualityController {

    private final FoodQualityService foodQualityService;

    public QualityController(FoodQualityService foodQualityService) {
        this.foodQualityService = foodQualityService;
    }

    // ==================== 质量记录 ====================

    /** 质量记录列表 */
    @Operation(summary = "质量记录列表", description = "分页查询质量记录")
    @GetMapping("/records")
    public Result<IPage<QualityRecordVO>> recordList(QualityStandardQueryDTO queryDTO) {
        IPage<QualityRecordVO> page = foodQualityService.queryRecordsPage(queryDTO);
        return Result.success(page);
    }

    /** 质量记录详情 */
    @Operation(summary = "质量记录详情", description = "根据ID查询质量记录详情")
    @GetMapping("/records/{id}")
    public Result<QualityRecordVO> recordDetail(@PathVariable Long id) {
        QualityRecordVO vo = foodQualityService.getRecordDetail(id);
        return Result.success(vo);
    }

    /** 创建质量记录 */
    @Operation(summary = "创建质量记录", description = "新增质量记录")
    @PostMapping("/records")
    public Result<QualityRecordVO> createRecord(@Valid @RequestBody QualityRecordCreateDTO dto) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        String operatorName = SecurityUtils.getCurrentUsername();
        QualityRecordVO vo = foodQualityService.createRecord(dto, operatorId, operatorName);
        return Result.success(vo, "质量记录创建成功");
    }

    /** 删除质量记录 */
    @Operation(summary = "删除质量记录", description = "逻辑删除质量记录")
    @DeleteMapping("/records/{id}")
    public Result<Boolean> deleteRecord(@PathVariable Long id) {
        boolean result = foodQualityService.deleteRecord(id);
        return Result.success(result, result ? "删除成功" : "删除失败");
    }

    /** 质量异常列表 */
    @Operation(summary = "质量异常列表", description = "分页查询质量异常记录")
    @GetMapping("/records/abnormal")
    public Result<IPage<QualityRecordVO>> abnormalList(QualityStandardQueryDTO queryDTO) {
        IPage<QualityRecordVO> page = foodQualityService.queryAbnormalRecords(queryDTO);
        return Result.success(page);
    }

    /** 处理质量异常
     * 前端请求体：{ handlingResult: string }
     */
    @Operation(summary = "处理质量异常", description = "记录质量异常的处理结果")
    @PostMapping("/records/{id}/handle")
    public Result<Boolean> handleAbnormal(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        String operatorName = SecurityUtils.getCurrentUsername();
        String handlingResult = body == null ? null : body.get("handlingResult");
        if (handlingResult == null || handlingResult.trim().isEmpty()) {
            return Result.error(400, "处理结果不能为空");
        }
        boolean result = foodQualityService.handleAbnormal(id, handlingResult, operatorId, operatorName);
        return Result.success(result, result ? "异常处理成功" : "异常处理失败");
    }

    /** 质量统计 */
    @Operation(summary = "质量统计", description = "获取质量记录统计数据")
    @GetMapping("/records/statistics")
    public Result<Map<String, Object>> statistics() {
        Map<String, Object> statistics = foodQualityService.getStatistics();
        return Result.success(statistics);
    }

    // ==================== 质量标准 ====================

    /** 质量标准列表 */
    @Operation(summary = "质量标准列表", description = "分页查询质量标准")
    @GetMapping("/standards")
    public Result<IPage<QualityStandardVO>> standardList(QualityStandardQueryDTO queryDTO) {
        IPage<QualityStandardVO> page = foodQualityService.queryStandardsPage(queryDTO);
        return Result.success(page);
    }

    /** 质量标准详情 */
    @Operation(summary = "质量标准详情", description = "根据ID查询质量标准详情")
    @GetMapping("/standards/{id}")
    public Result<QualityStandardVO> standardDetail(@PathVariable Long id) {
        QualityStandardVO vo = foodQualityService.getStandardDetail(id);
        return Result.success(vo);
    }

    /** 创建质量标准 */
    @Operation(summary = "创建质量标准", description = "新增质量标准")
    @PostMapping("/standards")
    public Result<QualityStandardVO> createStandard(@Valid @RequestBody QualityStandardCreateDTO dto) {
        QualityStandardVO vo = foodQualityService.createStandard(dto);
        return Result.success(vo, "质量标准创建成功");
    }

    /** 更新质量标准 */
    @Operation(summary = "更新质量标准", description = "根据ID更新质量标准")
    @PutMapping("/standards/{id}")
    public Result<QualityStandardVO> updateStandard(@PathVariable Long id, @Valid @RequestBody QualityStandardUpdateDTO dto) {
        dto.setStandardId(id);
        QualityStandardVO vo = foodQualityService.updateStandard(dto);
        return Result.success(vo, "质量标准更新成功");
    }

    /** 删除质量标准 */
    @Operation(summary = "删除质量标准", description = "逻辑删除质量标准")
    @DeleteMapping("/standards/{id}")
    public Result<Boolean> deleteStandard(@PathVariable Long id) {
        boolean result = foodQualityService.deleteStandard(id);
        return Result.success(result, result ? "删除成功" : "删除失败");
    }
}
