package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.entity.Resume;
import com.foodtraceability.service.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/resumes")
@Tag(name = "简历管理", description = "简历相关接口")
public class ResumeController {


    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    private final ResumeService resumeService;

    @Operation(summary = "获取简历列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('admin', 'hr', 'store_manager')")
    public Result<Map<String, Object>> getResumes(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String requirementId) {
        Map<String, Object> result = resumeService.getResumes(current, size, status, requirementId);
        return Result.success(result);
    }

    @Operation(summary = "获取简历详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'store_manager')")
    public Result<Resume> getResume(@PathVariable String id) {
        Resume resume = resumeService.getById(id);
        return Result.success(resume);
    }

    @Operation(summary = "创建简历")
    @PostMapping
    @PreAuthorize("hasAnyRole('admin', 'hr', 'store_manager')")
    public Result<Resume> createResume(@RequestBody Resume resume) {
        Resume created = resumeService.createResume(resume);
        return Result.success(created);
    }

    @Operation(summary = "更新简历")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'store_manager')")
    public Result<Resume> updateResume(
            @PathVariable String id,
            @RequestBody Resume resume) {
        Resume updated = resumeService.updateResume(id, resume);
        return Result.success(updated);
    }

    @Operation(summary = "删除简历")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('admin', 'hr')")
    public Result<Void> deleteResume(@PathVariable String id) {
        resumeService.deleteResume(id);
        return Result.success();
    }

    @Operation(summary = "更新简历状态")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'store_manager')")
    public Result<Void> updateResumeStatus(
            @PathVariable String id,
            @RequestParam String status) {
        resumeService.updateResumeStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "获取招聘需求的简历列表")
    @GetMapping("/requirement/{requirementId}")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'store_manager')")
    public Result<List<Resume>> getResumesByRequirement(@PathVariable String requirementId) {
        List<Resume> resumes = resumeService.getResumesByRequirement(requirementId);
        return Result.success(resumes);
    }
}
