package com.foodtraceability.controller.h5;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.OnboardingArchiveDTO;
import com.foodtraceability.entity.OnboardingArchive;
import com.foodtraceability.service.OnboardingArchiveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/v1/onboarding/archive")
@Tag(name = "入职档案管理", description = "入职档案的创建、查询、更新、删除等操作")
public class OnboardingArchiveController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OnboardingArchiveController.class);
    private final OnboardingArchiveService archiveService;

    @PostMapping
    @Operation(summary = "创建入职档案", description = "HR创建新的入职档案")
    @PreAuthorize("hasAuthority('hr:onboarding:manage') or hasAuthority('*')")
    public Result<OnboardingArchive> createArchive(@Valid @RequestBody OnboardingArchiveDTO dto) {
        log.info("创建入职档案，候选人：{}", dto.getCandidateName());
        OnboardingArchive archive = new OnboardingArchive();
        BeanUtils.copyProperties(dto, archive);
        archive.setCreateBy(1L);
        OnboardingArchive savedArchive = archiveService.createArchive(archive);
        return Result.success(savedArchive);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新入职档案", description = "更新入职档案信息")
    @PreAuthorize("hasAuthority('hr:onboarding:manage') or hasAuthority('*')")
    public Result<OnboardingArchive> updateArchive(@Parameter(description = "档案ID") @PathVariable Long id, @Valid @RequestBody OnboardingArchiveDTO dto) {
        log.info("更新入职档案，档案ID：{}", id);
        OnboardingArchive existingArchive = archiveService.getById(id);
        if (existingArchive == null) {
            return Result.error("档案不存在");
        }
        BeanUtils.copyProperties(dto, existingArchive);
        existingArchive.setId(id);
        OnboardingArchive updatedArchive = archiveService.updateArchive(existingArchive);
        return Result.success(updatedArchive);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除入职档案", description = "逻辑删除入职档案")
    @PreAuthorize("hasAuthority('hr:onboarding:manage') or hasAuthority('*')")
    public Result<Void> deleteArchive(@Parameter(description = "档案ID") @PathVariable Long id) {
        log.info("删除入职档案，档案ID：{}", id);
        boolean success = archiveService.removeById(id);
        if (success) {
            return Result.success();
        } else {
            return Result.error("删除失败");
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取档案详情", description = "根据ID获取入职档案详情")
    @PreAuthorize("hasAuthority('hr:onboarding:view') or hasAuthority('hr:onboarding:manage') or hasAuthority('*')")
    public Result<OnboardingArchive> getArchiveById(@Parameter(description = "档案ID") @PathVariable Long id) {
        log.info("获取档案详情，档案ID：{}", id);
        OnboardingArchive archive = archiveService.getById(id);
        if (archive == null) {
            return Result.error("档案不存在");
        }
        return Result.success(archive);
    }

    @GetMapping("/list")
    @Operation(summary = "分页查询档案列表", description = "分页查询入职档案列表")
    @PreAuthorize("hasAuthority('hr:onboarding:view') or hasAuthority('hr:onboarding:manage') or hasAuthority('*')")
    public Result<Page<OnboardingArchive>> listArchives(@Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page, @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer size, @Parameter(description = "状态") @RequestParam(required = false) String status, @Parameter(description = "关键词") @RequestParam(required = false) String keyword) {
        log.info("分页查询档案列表，页码：{}，每页大小：{}，状态：{}，关键词：{}", page, size, status, keyword);
        Page<OnboardingArchive> pageParam = new Page<>(page, size);
        QueryWrapper<OnboardingArchive> queryWrapper = new QueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq("status", status);
        }
        if (keyword != null && !keyword.isEmpty()) {
            queryWrapper.and(wrapper -> wrapper.like("candidate_name", keyword).or().like("email", keyword).or().like("phone", keyword));
        }
        queryWrapper.orderByDesc("create_time");
        Page<OnboardingArchive> result = archiveService.page(pageParam, queryWrapper);
        return Result.success(result);
    }

    @GetMapping("/by-status/{status}")
    @Operation(summary = "根据状态查询档案", description = "根据状态查询入职档案列表")
    @PreAuthorize("hasAuthority('hr:onboarding:view') or hasAuthority('hr:onboarding:manage') or hasAuthority('*')")
    public Result<List<OnboardingArchive>> getArchivesByStatus(@Parameter(description = "状态") @PathVariable String status) {
        log.info("根据状态查询档案，状态：{}", status);
        List<OnboardingArchive> archives = archiveService.getArchivesByStatus(status);
        return Result.success(archives);
    }

    @GetMapping("/pending-hr")
    @Operation(summary = "获取待HR审查档案", description = "获取待HR形式审查的档案列表")
    @PreAuthorize("hasAuthority('hr:onboarding:view') or hasAuthority('hr:onboarding:manage') or hasAuthority('*')")
    public Result<List<OnboardingArchive>> getPendingHrReviewArchives() {
        log.info("获取待HR审查的档案列表");
        List<OnboardingArchive> archives = archiveService.getPendingHrReviewArchives();
        return Result.success(archives);
    }

    @GetMapping("/pending-substantive")
    @Operation(summary = "获取待实质审查档案", description = "获取待实质审查的档案列表")
    @PreAuthorize("hasAuthority('hr:onboarding:view') or hasAuthority('hr:onboarding:manage') or hasAuthority('*')")
    public Result<List<OnboardingArchive>> getPendingSubstantiveReviewArchives() {
        log.info("获取待实质审查的档案列表");
        List<OnboardingArchive> archives = archiveService.getPendingSubstantiveReviewArchives();
        return Result.success(archives);
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交档案审批", description = "提交入职档案进行审批")
    @PreAuthorize("hasAuthority('hr:onboarding:manage') or hasAuthority('*')")
    public Result<Void> submitForApproval(@Parameter(description = "档案ID") @PathVariable Long id) {
        log.info("提交档案审批，档案ID：{}", id);
        Long hrId = 1L;
        try {
            boolean success = archiveService.submitForApproval(id, hrId);
            if (success) {
                return Result.success();
            } else {
                return Result.error("提交失败，请检查档案状态");
            }
        } catch (Exception e) {
            log.error("提交档案审批异常，档案ID：{}", id, e);
            return Result.error("提交审批失败：" + e.getMessage());
        }
    }

    @GetMapping("/position-levels")
    @Operation(summary = "获取职位级别", description = "获取所有可用的职位级别")
    @PreAuthorize("hasAuthority('hr:onboarding:view') or hasAuthority('hr:onboarding:manage') or hasAuthority('*')")
    public Result<List<String>> getPositionLevels() {
        return Result.success(List.of(OnboardingArchive.LEVEL_STAFF, OnboardingArchive.LEVEL_MANAGER, OnboardingArchive.LEVEL_DIRECTOR, OnboardingArchive.LEVEL_EXECUTIVE));
    }

    @GetMapping("/statuses")
    @Operation(summary = "获取档案状态", description = "获取所有可用的档案状态")
    @PreAuthorize("hasAuthority('hr:onboarding:view') or hasAuthority('hr:onboarding:manage') or hasAuthority('*')")
    public Result<List<String>> getStatuses() {
        return Result.success(List.of(OnboardingArchive.STATUS_CREATED, OnboardingArchive.STATUS_PENDING_HR, OnboardingArchive.STATUS_PENDING_SUBSTANTIVE, OnboardingArchive.STATUS_APPROVED, OnboardingArchive.STATUS_REJECTED, OnboardingArchive.STATUS_REGISTERED));
    }

    public OnboardingArchiveController(final OnboardingArchiveService archiveService) {
        this.archiveService = archiveService;
    }
}
