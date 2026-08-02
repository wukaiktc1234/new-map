package com.foodtraceability.controller.h5;

import com.foodtraceability.common.Result;
import com.foodtraceability.entity.OnboardingRecord;
import com.foodtraceability.service.OnboardingRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/onboarding-records")
@Tag(name = "入职管理", description = "入职相关接口")
public class OnboardingRecordController {
    

    public OnboardingRecordController(OnboardingRecordService onboardingRecordService) {
        this.onboardingRecordService = onboardingRecordService;
    }

    private final OnboardingRecordService onboardingRecordService;
    
    @Operation(summary = "获取入职记录列表")
    @GetMapping
    @PreAuthorize("hasAnyRole('admin', 'hr', 'store_manager')")
    public Result<Map<String, Object>> getOnboardingRecords(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String interviewId) {
        Map<String, Object> result = onboardingRecordService.getOnboardingRecords(current, size, status, interviewId);
        return Result.success(result);
    }
    
    @Operation(summary = "创建入职记录")
    @PostMapping
    @PreAuthorize("hasAnyRole('admin', 'hr', 'store_manager')")
    public Result<OnboardingRecord> createOnboardingRecord(@RequestBody OnboardingRecord record) {
        OnboardingRecord created = onboardingRecordService.createOnboardingRecord(record);
        return Result.success(created);
    }
    
    @Operation(summary = "更新入职状态")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'store_manager')")
    public Result<Void> updateOnboardingStatus(
            @PathVariable String id,
            @RequestParam String status) {
        onboardingRecordService.updateOnboardingStatus(id, status);
        return Result.success();
    }
    
    @Operation(summary = "生成注册码")
    @PostMapping("/{id}/registration-code")
    @PreAuthorize("hasAnyRole('admin', 'hr')")
    public Result<String> generateRegistrationCode(@PathVariable String id) {
        String code = onboardingRecordService.generateRegistrationCode(id);
        return Result.success(code);
    }
    
    @Operation(summary = "根据面试ID获取入职记录")
    @GetMapping("/interview/{interviewId}")
    @PreAuthorize("hasAnyRole('admin', 'hr', 'store_manager')")
    public Result<OnboardingRecord> getOnboardingByInterviewId(@PathVariable String interviewId) {
        OnboardingRecord record = onboardingRecordService.getOnboardingByInterviewId(interviewId);
        return Result.success(record);
    }
    
    @Operation(summary = "完成入职")
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('admin', 'HR')")
    public Result<Void> completeOnboarding(@PathVariable String id) {
        onboardingRecordService.completeOnboarding(id);
        return Result.success();
    }
}
