package com.foodtraceability.controller.h5;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.Result;
import com.foodtraceability.entity.InvitationSendRecord;
import com.foodtraceability.mapper.InvitationSendRecordMapper;
import com.foodtraceability.service.OnboardingInvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/onboarding/invitation")
@Tag(name = "入职邀请码管理", description = "入职邀请码的生成、验证、查询等操作")
public class OnboardingInvitationController {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OnboardingInvitationController.class);
    private final OnboardingInvitationService invitationService;
    private final InvitationSendRecordMapper invitationSendRecordMapper;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('hr:invitation-code:view') or hasAuthority('*')")
    @Operation(summary = "获取邀请码列表", description = "分页获取邀请码列表")
    public Result<Map<String, Object>> listInvitationCodes(@Parameter(description = "页码") @RequestParam(defaultValue = "1") int page, @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int size, @Parameter(description = "状态") @RequestParam(required = false) String status, @Parameter(description = "开始时间") @RequestParam(required = false) String startTime, @Parameter(description = "结束时间") @RequestParam(required = false) String endTime) {
        log.info("获取邀请码列表，页码：{}，每页数量：{}，状态：{}", page, size, status);
        Page<InvitationSendRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<InvitationSendRecord> queryWrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq(InvitationSendRecord::getStatus, status);
        }
        queryWrapper.orderByDesc(InvitationSendRecord::getCreateTime);
        IPage<InvitationSendRecord> pageResult = invitationSendRecordMapper.selectPage(pageParam, queryWrapper);
        Map<String, Object> result = new HashMap<>();
        result.put("records", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        return Result.success(result);
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('hr:invitation-code:view') or hasAuthority('*')")
    @Operation(summary = "获取邀请码统计", description = "获取邀请码各状态数量统计")
    public Result<Map<String, Integer>> getStatistics() {
        log.info("获取邀请码统计");
        Map<String, Integer> statistics = new HashMap<>();
        statistics.put("unused", Math.toIntExact(invitationSendRecordMapper.selectCount(new LambdaQueryWrapper<InvitationSendRecord>().eq(InvitationSendRecord::getStatus, "UNUSED"))));
        statistics.put("used", Math.toIntExact(invitationSendRecordMapper.selectCount(new LambdaQueryWrapper<InvitationSendRecord>().eq(InvitationSendRecord::getStatus, "USED"))));
        statistics.put("expired", Math.toIntExact(invitationSendRecordMapper.selectCount(new LambdaQueryWrapper<InvitationSendRecord>().eq(InvitationSendRecord::getStatus, "EXPIRED"))));
        statistics.put("revoked", Math.toIntExact(invitationSendRecordMapper.selectCount(new LambdaQueryWrapper<InvitationSendRecord>().eq(InvitationSendRecord::getStatus, "REVOKED"))));
        return Result.success(statistics);
    }

    @PostMapping("/generate/{archiveId}")
    @PreAuthorize("hasAuthority('hr:invitation-code:manage') or hasAuthority('*')")
    @Operation(summary = "生成邀请码", description = "为已通过的入职档案生成邀请码")
    public Result<InvitationSendRecord> generateInvitationCode(@Parameter(description = "档案ID") @PathVariable Long archiveId) {
        log.info("生成邀请码，档案ID：{}", archiveId);
        try {
            InvitationSendRecord record = invitationService.generateInvitationCode(archiveId);
            if (record != null) {
                return Result.success(record);
            } else {
                return Result.error("生成邀请码失败，请检查档案状态");
            }
        } catch (Exception e) {
            log.error("生成邀请码失败", e);
            return Result.error("生成邀请码失败：" + e.getMessage());
        }
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("hasAuthority('hr:invitation-code:view') or hasAuthority('*')")
    @Operation(summary = "查询邀请码", description = "根据邀请码查询详情")
    public Result<InvitationSendRecord> getInvitationCode(@Parameter(description = "邀请码") @PathVariable String code) {
        log.info("查询邀请码，邀请码：{}", code);
        try {
            InvitationSendRecord record = invitationService.getInvitationCode(code);
            if (record != null) {
                return Result.success(record);
            } else {
                return Result.error("邀请码不存在或已失效");
            }
        } catch (Exception e) {
            log.error("查询邀请码失败", e);
            return Result.error("邀请码验证失败，请稍后重试");
        }
    }

    @PostMapping("/validate")
    @PreAuthorize("hasAuthority('hr:invitation-code:view') or hasAuthority('*')")
    @Operation(summary = "验证邀请码", description = "验证邀请码是否有效")
    public Result<Boolean> validateInvitationCode(@Parameter(description = "邀请码") @RequestParam String code, @Parameter(description = "邮箱") @RequestParam(required = false) String email, @Parameter(description = "手机号") @RequestParam(required = false) String phone, @Parameter(description = "姓名") @RequestParam(required = false) String fullName) {
        log.info("验证邀请码，邀请码：{}，邮箱：{}，手机：{}，姓名：{}", code, email, phone, fullName);
        try {
            boolean valid = invitationService.validateInvitationCode(code, email, phone, fullName);
            return Result.success(valid);
        } catch (Exception e) {
            log.error("验证邀请码失败", e);
            return Result.error("邀请码验证失败");
        }
    }

    @PostMapping("/use/{code}")
    @PreAuthorize("hasAuthority('hr:invitation-code:manage') or hasAuthority('*')")
    @Operation(summary = "使用邀请码", description = "标记邀请码为已使用")
    public Result<Void> useInvitationCode(@Parameter(description = "邀请码") @PathVariable String code, @Parameter(description = "用户ID") @RequestParam Long userId) {
        log.info("使用邀请码，邀请码：{}，用户ID：{}", code, userId);
        try {
            boolean success = invitationService.useInvitationCode(code, userId);
            if (success) {
                return Result.success();
            } else {
                return Result.error("邀请码使用失败");
            }
        } catch (Exception e) {
            log.error("使用邀请码失败", e);
            return Result.error("邀请码使用失败");
        }
    }

    @PostMapping("/extend/{codeId}")
    @PreAuthorize("hasAuthority('hr:invitation-code:manage') or hasAuthority('*')")
    @Operation(summary = "延长邀请码有效期", description = "延长邀请码有效期")
    public Result<Void> extendExpiration(@Parameter(description = "邀请码ID") @PathVariable Long codeId, @Parameter(description = "延长天数") @RequestParam(defaultValue = "7") int addDays) {
        log.info("延长邀请码有效期，邀请码ID：{}，延长天数：{}", codeId, addDays);
        try {
            InvitationSendRecord record = invitationSendRecordMapper.selectById(codeId);
            if (record == null) {
                return Result.error("邀请码不存在");
            }
            record.setExpireTime(record.getExpireTime().plusDays(addDays));
            invitationSendRecordMapper.updateById(record);
            return Result.success();
        } catch (Exception e) {
            log.error("延长邀请码有效期失败", e);
            return Result.error("延长有效期失败");
        }
    }

    @PostMapping("/revoke/{codeId}")
    @PreAuthorize("hasAuthority('hr:invitation-code:manage') or hasAuthority('*')")
    @Operation(summary = "撤销邀请码", description = "撤销邀请码")
    public Result<Void> revokeInvitationCode(@Parameter(description = "邀请码ID") @PathVariable Long codeId) {
        log.info("撤销邀请码，邀请码ID：{}", codeId);
        try {
            InvitationSendRecord record = invitationSendRecordMapper.selectById(codeId);
            if (record == null) {
                return Result.error("邀请码不存在");
            }
            record.setStatus("REVOKED");
            invitationSendRecordMapper.updateById(record);
            return Result.success();
        } catch (Exception e) {
            log.error("撤销邀请码失败", e);
            return Result.error("撤销邀请码失败");
        }
    }

    public OnboardingInvitationController(final OnboardingInvitationService invitationService, final InvitationSendRecordMapper invitationSendRecordMapper) {
        this.invitationService = invitationService;
        this.invitationSendRecordMapper = invitationSendRecordMapper;
    }
}
