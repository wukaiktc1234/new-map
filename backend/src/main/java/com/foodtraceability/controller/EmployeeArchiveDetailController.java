package com.foodtraceability.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.entity.EmployeeArchiveDetail;
import com.foodtraceability.service.EmployeeArchiveDetailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.foodtraceability.utils.SecurityUtils;
import java.util.Map;

/**
 * @author Liberty
 * @version 1.0
 * @since 2026-03-20
 */
@RestController
@RequestMapping("/v1/hr/archive-detail")
@Tag(name = "员工档案详情管理", description = "员工档案详细信息的增删改查和审核")
public class EmployeeArchiveDetailController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EmployeeArchiveDetailController.class);

    public EmployeeArchiveDetailController(EmployeeArchiveDetailService archiveDetailService) {
        this.archiveDetailService = archiveDetailService;
    }

    private final EmployeeArchiveDetailService archiveDetailService;

    @PostMapping
    @Operation(summary = "创建档案详情")
    @PreAuthorize("hasAuthority('hr:archive:manage') or hasAuthority('*')")
    public Result<EmployeeArchiveDetail> createDetail(@RequestBody EmployeeArchiveDetail detail) {
        log.info("创建档案详情，档案ID：{}", detail.getArchiveId());
        // 检查是否已存在
        EmployeeArchiveDetail existing = archiveDetailService.getByArchiveId(detail.getArchiveId());
        if (existing != null) {
            return Result.error("该档案已存在详情记录");
        }
        // 计算完整度
        int completeness = archiveDetailService.calculateCompleteness(detail);
        detail.setCompletenessScore(completeness);
        archiveDetailService.save(detail);
        return Result.success(detail);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新档案详情")
    @PreAuthorize("hasAuthority('hr:archive:manage') or hasAuthority('*')")
    public Result<EmployeeArchiveDetail> updateDetail(@PathVariable Long id, @RequestBody EmployeeArchiveDetail detail) {
        log.info("更新档案详情：{}", id);
        EmployeeArchiveDetail existing = archiveDetailService.getById(id);
        if (existing == null) {
            return Result.error(404, "档案详情不存在");
        }
        detail.setId(id);
        // 重新计算完整度
        int completeness = archiveDetailService.calculateCompleteness(detail);
        detail.setCompletenessScore(completeness);
        archiveDetailService.updateById(detail);
        return Result.success(detail);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取档案详情")
    @PreAuthorize("hasAuthority('hr:archive:view') or hasAuthority('*')")
    public Result<EmployeeArchiveDetail> getDetail(@PathVariable Long id) {
        EmployeeArchiveDetail detail = archiveDetailService.getById(id);
        if (detail == null) {
            return Result.error(404, "档案详情不存在");
        }
        return Result.success(detail);
    }

    @GetMapping("/archive/{archiveId}")
    @Operation(summary = "根据档案ID获取详情")
    @PreAuthorize("hasAuthority('hr:archive:view') or hasAuthority('*')")
    public Result<EmployeeArchiveDetail> getByArchiveId(@PathVariable Long archiveId) {
        EmployeeArchiveDetail detail = archiveDetailService.getByArchiveId(archiveId);
        if (detail == null) {
            return Result.error(404, "档案详情不存在");
        }
        return Result.success(detail);
    }

    @GetMapping("/employee/{employeeId}")
    @Operation(summary = "根据员工ID获取详情")
    @PreAuthorize("hasAuthority('hr:archive:view') or hasAuthority('*')")
    public Result<EmployeeArchiveDetail> getByEmployeeId(@PathVariable String employeeId) {
        EmployeeArchiveDetail detail = archiveDetailService.getByEmployeeId(employeeId);
        if (detail == null) {
            return Result.error(404, "档案详情不存在");
        }
        return Result.success(detail);
    }

    @GetMapping("/list")
    @Operation(summary = "获取档案详情列表")
    @PreAuthorize("hasAuthority('hr:archive:view') or hasAuthority('*')")
    public Result<Map<String, Object>> listDetails(@Parameter(description = "页码") @RequestParam(defaultValue = "1") int page, @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size, @Parameter(description = "审核状态") @RequestParam(required = false) String reviewStatus, @Parameter(description = "关键词") @RequestParam(required = false) String keyword) {
        Page<EmployeeArchiveDetail> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<EmployeeArchiveDetail> wrapper = new LambdaQueryWrapper<>();
        if (reviewStatus != null && !reviewStatus.isEmpty()) {
            wrapper.eq(EmployeeArchiveDetail::getReviewStatus, reviewStatus);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(EmployeeArchiveDetail::getRealName, keyword);
        }
        wrapper.orderByDesc(EmployeeArchiveDetail::getCreateTime);
        IPage<EmployeeArchiveDetail> pageResult = archiveDetailService.page(pageParam, wrapper);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("records", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        return Result.success(result);
    }

    @PostMapping("/{id}/review")
    @Operation(summary = "审核档案")
    @PreAuthorize("hasAuthority('hr:archive:manage') or hasAuthority('*')")
    public Result<Void> reviewArchive(@PathVariable Long id, @RequestParam String reviewStatus, @RequestParam(required = false) String reviewComment) {
        try {
            Long reviewerId = SecurityUtils.getCurrentUserId();
            archiveDetailService.reviewArchive(id, reviewStatus, reviewComment, reviewerId);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/confirm-privacy")
    @Operation(summary = "确认隐私协议")
    @PreAuthorize("hasAuthority('hr:archive:manage') or hasAuthority('*')")
    public Result<Void> confirmPrivacy(@PathVariable Long id) {
        EmployeeArchiveDetail detail = archiveDetailService.getById(id);
        if (detail == null) {
            return Result.error(404, "档案详情不存在");
        }
        detail.setPrivacyAgreementSigned(1);
        detail.setPrivacyAgreementTime(java.time.LocalDateTime.now());
        archiveDetailService.updateById(detail);
        archiveDetailService.updateCompleteness(id);
        return Result.success();
    }

    @PostMapping("/{id}/confirm-accuracy")
    @Operation(summary = "确认数据准确性")
    @PreAuthorize("hasAuthority('hr:archive:manage') or hasAuthority('*')")
    public Result<Void> confirmAccuracy(@PathVariable Long id) {
        EmployeeArchiveDetail detail = archiveDetailService.getById(id);
        if (detail == null) {
            return Result.error(404, "档案详情不存在");
        }
        detail.setDataAccuracyConfirmed(1);
        detail.setDataConfirmTime(java.time.LocalDateTime.now());
        archiveDetailService.updateById(detail);
        archiveDetailService.updateCompleteness(id);
        return Result.success();
    }

    @PostMapping("/{id}/sync-to-employee")
    @Operation(summary = "同步到员工表")
    @PreAuthorize("hasAuthority('hr:archive:manage') or hasAuthority('*')")
    public Result<Void> syncToEmployee(@PathVariable Long id, @RequestParam String employeeCode) {
        try {
            archiveDetailService.syncToEmployee(employeeCode);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取档案统计")
    @PreAuthorize("hasAuthority('hr:archive:view') or hasAuthority('*')")
    public Result<Map<String, Object>> getStatistics() {
        return Result.success(archiveDetailService.getStatistics());
    }

    @GetMapping("/{id}/completeness")
    @Operation(summary = "获取档案完整度")
    @PreAuthorize("hasAuthority('hr:archive:view') or hasAuthority('*')")
    public Result<Map<String, Object>> getCompleteness(@PathVariable Long id) {
        EmployeeArchiveDetail detail = archiveDetailService.getById(id);
        if (detail == null) {
            return Result.error(404, "档案详情不存在");
        }
        int score = archiveDetailService.calculateCompleteness(detail);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("score", score);
        result.put("level", getCompletenessLevel(score));
        return Result.success(result);
    }

    private String getCompletenessLevel(int score) {
        if (score >= 90) return "优秀";
        if (score >= 80) return "良好";
        if (score >= 60) return "合格";
        return "待完善";
    }
}
