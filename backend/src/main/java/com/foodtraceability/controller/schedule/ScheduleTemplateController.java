package com.foodtraceability.controller.schedule;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.schedule.ScheduleTemplateCreateDTO;
import com.foodtraceability.dto.schedule.ScheduleTemplateUpdateDTO;
import com.foodtraceability.dto.schedule.ScheduleTemplateVO;
import com.foodtraceability.service.schedule.ScheduleTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 排班模板管理控制器
 * 提供排班模板的增删改查、复制、设为默认及状态管理接口
 */
@Tag(name = "排班管理-排班模板", description = "排班模板(F-006)相关接口")
@RestController
@RequestMapping("/v1/schedule/templates")
public class ScheduleTemplateController {

    private final ScheduleTemplateService scheduleTemplateService;

    public ScheduleTemplateController(ScheduleTemplateService scheduleTemplateService) {
        this.scheduleTemplateService = scheduleTemplateService;
    }

    /**
     * 查询门店的模板列表
     */
    @Operation(summary = "查询模板列表",
            description = "获取指定门店的所有排班模板，按创建时间倒序返回")
    @GetMapping
    @PreAuthorize("hasAuthority('schedule:template:manage')")
    public Result<List<ScheduleTemplateVO>> getTemplateList(
            @Parameter(description = "门店ID") @RequestParam Long storeId) {
        List<ScheduleTemplateVO> list = scheduleTemplateService.getTemplateList(storeId);
        return Result.success(list);
    }

    /**
     * 获取模板详情
     */
    @Operation(summary = "获取模板详情",
            description = "根据ID获取排班模板的详细信息")
    @GetMapping("/{templateId}")
    @PreAuthorize("hasAuthority('schedule:template:manage')")
    public Result<ScheduleTemplateVO> getTemplateById(
            @Parameter(description = "模板ID") @PathVariable Long templateId) {
        ScheduleTemplateVO vo = scheduleTemplateService.getTemplateById(templateId);
        if (vo == null) {
            return Result.error("排班模板不存在");
        }
        return Result.success(vo);
    }

    /**
     * 创建模板
     */
    @Operation(summary = "创建模板",
            description = "新增一个排班模板配置")
    @PostMapping
    @PreAuthorize("hasAuthority('schedule:template:manage')")
    public Result<ScheduleTemplateVO> createTemplate(
            @Valid @RequestBody ScheduleTemplateCreateDTO createDTO) {
        ScheduleTemplateVO vo = scheduleTemplateService.createTemplate(createDTO);
        return Result.success(vo, "模板创建成功");
    }

    /**
     * 编辑模板
     */
    @Operation(summary = "编辑模板",
            description = "修改已有排班模板的配置信息")
    @PutMapping("/{templateId}")
    @PreAuthorize("hasAuthority('schedule:template:manage')")
    public Result<ScheduleTemplateVO> updateTemplate(
            @Parameter(description = "模板ID") @PathVariable Long templateId,
            @Valid @RequestBody ScheduleTemplateUpdateDTO updateDTO) {
        try {
            ScheduleTemplateVO vo = scheduleTemplateService.updateTemplate(templateId, updateDTO);
            return Result.success(vo, "模板更新成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除模板(逻辑删除)
     * 需检查是否有关联的排班方案
     */
    @Operation(summary = "删除模板",
            description = "删除指定模板（已关联排班方案的模板无法删除）")
    @DeleteMapping("/{templateId}")
    @PreAuthorize("hasAuthority('schedule:template:manage')")
    public Result<String> deleteTemplate(
            @Parameter(description = "模板ID") @PathVariable Long templateId) {
        try {
            scheduleTemplateService.deleteTemplate(templateId);
            return Result.success("模板删除成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 复制模板
     * 新名称自动追加"副本"后缀
     */
    @Operation(summary = "复制模板",
            description = "基于指定模板创建副本，名称自动追加'副本'后缀")
    @PostMapping("/{templateId}/copy")
    @PreAuthorize("hasAuthority('schedule:template:manage')")
    public Result<ScheduleTemplateVO> copyTemplate(
            @Parameter(description = "源模板ID") @PathVariable Long templateId) {
        try {
            ScheduleTemplateVO vo = scheduleTemplateService.copyTemplate(templateId);
            return Result.success(vo, "模板复制成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 设为默认模板
     * 每个门店仅一个默认模板
     */
    @Operation(summary = "设为默认模板",
            description = "将指定模板设为门店默认模板（自动取消其他默认）")
    @PutMapping("/{templateId}/set-default")
    @PreAuthorize("hasAuthority('schedule:template:manage')")
    public Result<ScheduleTemplateVO> setDefault(
            @Parameter(description = "模板ID") @PathVariable Long templateId) {
        try {
            ScheduleTemplateVO vo = scheduleTemplateService.setDefault(templateId);
            return Result.success(vo, "已设为默认模板");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 启用/停用切换
     */
    @Operation(summary = "启用/停用模板",
            description = "切换排班模板的启用/停用状态")
    @PutMapping("/{templateId}/toggle-status")
    @PreAuthorize("hasAuthority('schedule:template:manage')")
    public Result<ScheduleTemplateVO> toggleStatus(
            @Parameter(description = "模板ID") @PathVariable Long templateId) {
        try {
            ScheduleTemplateVO vo = scheduleTemplateService.toggleStatus(templateId);
            return Result.success(vo, "状态切换成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
