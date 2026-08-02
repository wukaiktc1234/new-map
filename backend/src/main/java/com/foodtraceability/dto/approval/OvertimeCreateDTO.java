package com.foodtraceability.dto.approval;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 加班申请创建DTO
 * 用于加班类型审批的详细表单数据
 * 包含加班日期、时间段、补偿方式等信息
 */
@Schema(description = "加班申请创建DTO")
public class OvertimeCreateDTO {

    /** 加班日期 */
    @NotNull(message = "加班日期不能为空")
    @Schema(description = "加班日期", example = "2026-06-05",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate overtimeDate;

    /**
     * 开始时间（格式 HH:mm）
     * 使用24小时制
     */
    @NotNull(message = "开始时间不能为空")
    @Pattern(regexp = "^([01]?\\d|2[0-3]):[0-5]\\d$", message = "开始时间格式不正确（HH:mm）")
    @Schema(description = "开始时间（HH:mm）", example = "18:00",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String startTime;

    /**
     * 结束时间（格式 HH:mm）
     * 使用24小时制
     */
    @NotNull(message = "结束时间不能为空")
    @Pattern(regexp = "^([01]?\\d|2[0-3]):[0-5]\\d$", message = "结束时间格式不正确（HH:mm）")
    @Schema(description = "结束时间（HH:mm）", example = "22:00",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String endTime;

    /** 加班原因 */
    @NotBlank(message = "加班原因不能为空")
    @Size(max = 300, message = "加班原因长度不能超过300个字符")
    @Schema(description = "加班原因", example = "月末盘点需要延长工作时间",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String reason;

    /**
     * 补偿方式
     * overtime_pay=加班费, compensatory_leave=调休, mixed=混合补偿
     */
    @NotBlank(message = "补偿方式不能为空")
    @Pattern(regexp = "^(overtime_pay|compensatory_leave|mixed)$", message = "补偿方式不合法")
    @Schema(description = "补偿方式", example = "compensatory_leave",
            allowableValues = {"overtime_pay", "compensatory_leave", "mixed"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String compensateType;

    /**
     * 加班类型
     * weekday=工作日, weekend=周末, holiday=法定节假日
     * 可选，系统可根据日期自动判断
     */
    @Pattern(regexp = "^(weekday|weekend|holiday)?$", message = "加班类型不合法")
    @Schema(description = "加班类型", example = "weekend",
            allowableValues = {"weekday", "weekend", "holiday"})
    private String overtimeType;

    // ==================== Getter & Setter 方法 ====================

    public LocalDate getOvertimeDate() {
        return overtimeDate;
    }

    public void setOvertimeDate(LocalDate overtimeDate) {
        this.overtimeDate = overtimeDate;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getCompensateType() {
        return compensateType;
    }

    public void setCompensateType(String compensateType) {
        this.compensateType = compensateType;
    }

    public String getOvertimeType() {
        return overtimeType;
    }

    public void setOvertimeType(String overtimeType) {
        this.overtimeType = overtimeType;
    }
}
