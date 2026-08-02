package com.foodtraceability.dto.approval;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 换班申请创建DTO
 * 用于换班类型审批的详细表单数据
 * 包含目标换班人、原班次/目标班次、换班原因等信息
 */
@Schema(description = "换班申请创建DTO")
public class SwapCreateDTO {

    /** 目标换班人ID */
    @NotBlank(message = "目标换班人不能为空")
    @Schema(description = "目标换班人ID", example = "emp002",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String targetEmployeeId;

    /** 原班次日期 */
    @NotNull(message = "原班次日期不能为空")
    @Schema(description = "原班次日期", example = "2026-06-08",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate originalDate;

    /**
     * 原班次类型
     * morning=早班, afternoon=中班, evening=晚班, night=夜班
     */
    @NotBlank(message = "原班次类型不能为空")
    @Pattern(regexp = "^(morning|afternoon|evening|night)$", message = "原班次类型不合法")
    @Schema(description = "原班次类型", example = "morning",
            allowableValues = {"morning", "afternoon", "evening", "night"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String originalShiftType;

    /** 目标班次日期 */
    @NotNull(message = "目标班次日期不能为空")
    @Schema(description = "目标班次日期", example = "2026-06-10",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate targetDate;

    /**
     * 目标班次类型
     * morning=早班, afternoon=中班, evening=晚班, night=夜班
     */
    @NotBlank(message = "目标班次类型不能为空")
    @Pattern(regexp = "^(morning|afternoon|evening|night)$", message = "目标班次类型不合法")
    @Schema(description = "目标班次类型", example = "afternoon",
            allowableValues = {"morning", "afternoon", "evening", "night"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String targetShiftType;

    /** 换班原因 */
    @NotBlank(message = "换班原因不能为空")
    @Size(max = 300, message = "换班原因长度不能超过300个字符")
    @Schema(description = "换班原因", example = "个人有事需要调换班次",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String reason;

    /** 覆盖方案说明（如何保证原班次有人值守） */
    @Size(max = 300, message = "覆盖方案说明长度不能超过300个字符")
    @Schema(description = "覆盖方案说明", example = "由王五临时顶替原班次")
    private String coveragePlan;

    // ==================== Getter & Setter 方法 ====================

    public String getTargetEmployeeId() {
        return targetEmployeeId;
    }

    public void setTargetEmployeeId(String targetEmployeeId) {
        this.targetEmployeeId = targetEmployeeId;
    }

    public LocalDate getOriginalDate() {
        return originalDate;
    }

    public void setOriginalDate(LocalDate originalDate) {
        this.originalDate = originalDate;
    }

    public String getOriginalShiftType() {
        return originalShiftType;
    }

    public void setOriginalShiftType(String originalShiftType) {
        this.originalShiftType = originalShiftType;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public String getTargetShiftType() {
        return targetShiftType;
    }

    public void setTargetShiftType(String targetShiftType) {
        this.targetShiftType = targetShiftType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getCoveragePlan() {
        return coveragePlan;
    }

    public void setCoveragePlan(String coveragePlan) {
        this.coveragePlan = coveragePlan;
    }
}
