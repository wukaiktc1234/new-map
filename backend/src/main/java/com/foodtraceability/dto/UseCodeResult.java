package com.foodtraceability.dto;

import com.foodtraceability.entity.OnboardingRecord;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "使用注册码结果")
public class UseCodeResult {

    @Schema(description = "是否使用成功")
    private boolean success;

    @Schema(description = "注册码")
    private String code;

    @Schema(description = "关联的入职记录")
    private OnboardingRecord onboardingRecord;

    public UseCodeResult() {
    }

    public UseCodeResult(boolean success, String code, OnboardingRecord onboardingRecord) {
        this.success = success;
        this.code = code;
        this.onboardingRecord = onboardingRecord;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public OnboardingRecord getOnboardingRecord() {
        return onboardingRecord;
    }

    public void setOnboardingRecord(OnboardingRecord onboardingRecord) {
        this.onboardingRecord = onboardingRecord;
    }
}
