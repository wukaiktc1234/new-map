package com.foodtraceability.controller.schedule;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.schedule.ScheduleShiftTypeCreateDTO;
import com.foodtraceability.dto.schedule.ScheduleShiftTypeReorderDTO;
import com.foodtraceability.dto.schedule.ScheduleShiftTypeUpdateDTO;
import com.foodtraceability.dto.schedule.ScheduleShiftTypeVO;
import com.foodtraceability.service.schedule.ScheduleShiftTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 班次类型配置控制器
 * 提供班次类型的增删改查及状态管理接口
 */
@Tag(name = "排班管理-班次类型配置", description = "班次类型(F-007)相关接口")
@RestController
@RequestMapping("/v1/schedule/shift-types")
public class ScheduleShiftTypeController {

    private final ScheduleShiftTypeService scheduleShiftTypeService;

    public ScheduleShiftTypeController(ScheduleShiftTypeService scheduleShiftTypeService) {
        this.scheduleShiftTypeService = scheduleShiftTypeService;
    }

    /**
     * 查询启用的班次列表
     */
    @Operation(summary = "查询启用的班次列表", description = "获取当前门店所有启用状态的班次类型，按排序顺序返回")
    @GetMapping
    @PreAuthorize("hasAuthority('schedule:shift:configure')")
    public Result<List<ScheduleShiftTypeVO>> getActiveShiftTypes(
            @Parameter(description = "门店ID") @RequestParam Long storeId) {
        List<ScheduleShiftTypeVO> list = scheduleShiftTypeService.getActiveShiftTypes(storeId);
        return Result.success(list);
    }

    /**
     * 查询全部班次含停用
     */
    @Operation(summary = "查询全部班次(含停用)", description = "获取当前门店所有班次类型，包含停用状态，按排序顺序返回")
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('schedule:shift:configure')")
    public Result<List<ScheduleShiftTypeVO>> getAllShiftTypes(
            @Parameter(description = "门店ID") @RequestParam Long storeId) {
        List<ScheduleShiftTypeVO> list = scheduleShiftTypeService.getAllShiftTypes(storeId);
        return Result.success(list);
    }

    /**
     * 获取班次详情
     */
    @Operation(summary = "获取班次详情", description = "根据ID获取班次类型的详细信息")
    @GetMapping("/{shiftTypeId}")
    @PreAuthorize("hasAuthority('schedule:shift:configure')")
    public Result<ScheduleShiftTypeVO> getShiftTypeById(
            @Parameter(description = "班次类型ID") @PathVariable Long shiftTypeId) {
        ScheduleShiftTypeVO vo = scheduleShiftTypeService.getShiftTypeById(shiftTypeId);
        if (vo == null) {
            return Result.error("班次类型不存在");
        }
        return Result.success(vo);
    }

    /**
     * 创建班次类型
     */
    @Operation(summary = "创建班次类型", description = "新增一个班次类型配置")
    @PostMapping
    @PreAuthorize("hasAuthority('schedule:shift:configure')")
    public Result<ScheduleShiftTypeVO> createShiftType(
            @Valid @RequestBody ScheduleShiftTypeCreateDTO createDTO) {
        ScheduleShiftTypeVO vo = scheduleShiftTypeService.createShiftType(createDTO);
        return Result.success(vo, "班次类型创建成功");
    }

    /**
     * 编辑班次类型
     */
    @Operation(summary = "编辑班次类型", description = "修改已有班次类型的配置信息")
    @PutMapping("/{shiftTypeId}")
    @PreAuthorize("hasAuthority('schedule:shift:configure')")
    public Result<ScheduleShiftTypeVO> updateShiftType(
            @Parameter(description = "班次类型ID") @PathVariable Long shiftTypeId,
            @Valid @RequestBody ScheduleShiftTypeUpdateDTO updateDTO) {
        try {
            ScheduleShiftTypeVO vo = scheduleShiftTypeService.updateShiftType(shiftTypeId, updateDTO);
            return Result.success(vo, "班次类型更新成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 启用/停用切换
     */
    @Operation(summary = "启用/停用班次类型", description = "切换班次类型的启用/停用状态")
    @PutMapping("/{shiftTypeId}/toggle-status")
    @PreAuthorize("hasAuthority('schedule:shift:configure')")
    public Result<ScheduleShiftTypeVO> toggleStatus(
            @Parameter(description = "班次类型ID") @PathVariable Long shiftTypeId) {
        try {
            ScheduleShiftTypeVO vo = scheduleShiftTypeService.toggleStatus(shiftTypeId);
            return Result.success(vo, "状态切换成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 调整排序
     */
    @Operation(summary = "调整班次排序", description = "批量调整门店下班次类型的显示顺序")
    @PutMapping("/reorder")
    @PreAuthorize("hasAuthority('schedule:shift:configure')")
    public Result<String> reorderShiftTypes(
            @Valid @RequestBody ScheduleShiftTypeReorderDTO reorderDTO) {
        scheduleShiftTypeService.reorderShiftTypes(reorderDTO);
        return Result.success("排序调整成功");
    }
}
