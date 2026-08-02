package com.foodtraceability.controller;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.ApprovalRecordWithArchiveDTO;
import com.foodtraceability.entity.ApprovalRecord;
import com.foodtraceability.service.ApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/v1/onboarding/approval")
@Tag(name = "审批管理", description = "审批流程的管理和操作")
public class ApprovalController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ApprovalController.class);
    private final ApprovalService approvalService;

    @GetMapping("/archive/{archiveId}")
    @PreAuthorize("hasAuthority('hr:approval:view') or hasAuthority('*')")
    @Operation(summary = "获取档案审批记录", description = "根据档案ID获取所有审批记录")
    public Result<List<ApprovalRecord>> getApprovalsByArchiveId(@Parameter(description = "档案ID") @PathVariable Long archiveId) {
        log.info("获取档案审批记录，档案ID：{}", archiveId);
        List<ApprovalRecord> records = approvalService.getApprovalsByArchiveId(archiveId);
        return Result.success(records);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('hr:approval:view') or hasAuthority('*')")
    @Operation(summary = "获取待审批列表", description = "获取当前用户的待审批列表")
    public Result<List<ApprovalRecord>> getPendingApprovals() {
        Long reviewerId = 1L;
        log.info("获取待审批列表，审批人ID：{}", reviewerId);
        List<ApprovalRecord> records = approvalService.getPendingApprovals(reviewerId);
        return Result.success(records);
    }

    @GetMapping("/pending-with-archive")
    @PreAuthorize("hasAuthority('hr:approval:view') or hasAuthority('*')")
    @Operation(summary = "获取待审批列表（带档案信息）", description = "获取当前用户的待审批列表，包含关联的档案信息")
    public Result<List<ApprovalRecordWithArchiveDTO>> getPendingApprovalsWitArchive() {
        Long reviewerId = 1L;
        log.info("获取待审批列表（带档案信息），审批人ID：{}", reviewerId);
        List<ApprovalRecordWithArchiveDTO> records = approvalService.getPendingApprovalsWitArchive(reviewerId);
        return Result.success(records);
    }

    @PostMapping("/{recordId}/approve")
    @PreAuthorize("hasAuthority('hr:approval:manage') or hasAuthority('*')")
    @Operation(summary = "审批通过", description = "审批通过操作")
    public Result<Void> approve(@Parameter(description = "审批记录ID") @PathVariable Long recordId, @Parameter(description = "审批意见") @RequestParam(required = false) String comment) {
        Long reviewerId = 1L;
        log.info("审批通过，记录ID：{}，审批人：{}", recordId, reviewerId);
        boolean success = approvalService.approve(recordId, reviewerId, comment);
        if (success) {
            return Result.success();
        } else {
            return Result.error("审批失败");
        }
    }

    @PostMapping("/{recordId}/reject")
    @PreAuthorize("hasAuthority('hr:approval:manage') or hasAuthority('*')")
    @Operation(summary = "审批拒绝", description = "审批拒绝操作")
    public Result<Void> reject(@Parameter(description = "审批记录ID") @PathVariable Long recordId, @Parameter(description = "拒绝原因", required = true) @RequestParam String comment) {
        Long reviewerId = 1L;
        log.info("审批拒绝，记录ID：{}，审批人：{}", recordId, reviewerId);
        boolean success = approvalService.reject(recordId, reviewerId, comment);
        if (success) {
            return Result.success();
        } else {
            return Result.error("审批失败");
        }
    }

    @PostMapping("/start/{archiveId}")
    @PreAuthorize("hasAuthority('hr:approval:manage') or hasAuthority('*')")
    @Operation(summary = "启动审批流程", description = "为档案启动审批流程")
    public Result<Void> startApprovalProcess(@Parameter(description = "档案ID") @PathVariable Long archiveId) {
        Long hrId = 1L;
        log.info("启动审批流程，档案ID：{}，HR ID：{}", archiveId, hrId);
        boolean success = approvalService.startApprovalProcess(archiveId, hrId);
        if (success) {
            return Result.success();
        } else {
            return Result.error("启动失败");
        }
    }

    @GetMapping("/complete/{archiveId}")
    @PreAuthorize("hasAuthority('hr:approval:view') or hasAuthority('*')")
    @Operation(summary = "检查审批流程是否完成", description = "检查档案的审批流程是否已完成")
    public Result<Boolean> isApprovalProcessComplete(@Parameter(description = "档案ID") @PathVariable Long archiveId) {
        log.info("检查审批流程是否完成，档案ID：{}", archiveId);
        boolean complete = approvalService.isApprovalProcessComplete(archiveId);
        return Result.success(complete);
    }

    @GetMapping("/flow/{positionLevel}")
    @PreAuthorize("hasAuthority('hr:approval:view') or hasAuthority('*')")
    @Operation(summary = "获取审批流程", description = "根据职位级别获取审批流程")
    public Result<List<ApprovalService.ApprovalStep>> getApprovalFlow(@Parameter(description = "职位级别") @PathVariable String positionLevel) {
        log.info("获取审批流程，职位级别：{}", positionLevel);
        List<ApprovalService.ApprovalStep> flow = approvalService.getApprovalFlowByPositionLevel(positionLevel);
        return Result.success(flow);
    }

    @GetMapping("/history")
    @PreAuthorize("hasAuthority('hr:approval:view') or hasAuthority('*')")
    @Operation(summary = "获取审批历史", description = "获取所有已完成的审批记录（带档案信息）")
    public Result<List<ApprovalRecordWithArchiveDTO>> getApprovalHistory() {
        log.info("获取审批历史");
        List<ApprovalRecordWithArchiveDTO> records = approvalService.getApprovalHistory();
        return Result.success(records);
    }

    public ApprovalController(final ApprovalService approvalService) {
        this.approvalService = approvalService;
    }
}
