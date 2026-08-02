package com.foodtraceability.dto.approval;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 请假申请创建DTO
 * 用于请假类型审批的详细表单数据
 * 包含请假类型、日期范围、原因、交接信息等字段
 */
@Schema(description = "请假申请创建DTO")
public class LeaveCreateDTO {

    /**
     * 请假类型
     * annual=年假, sick=病假, personal=事假, compensatory=调休,
     * marriage=婚假, funeral=丧假, paternity=陪产假
     */
    @NotBlank(message = "请假类型不能为空")
    @Pattern(regexp = "^(annual|sick|personal|compensatory|marriage|funeral|paternity)$",
             message = "请假类型不合法")
    @Schema(description = "请假类型", example = "annual",
            allowableValues = {"annual", "sick", "personal", "compensatory", "marriage", "funeral", "paternity"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String leaveType;

    /** 开始日期 */
    @NotNull(message = "开始日期不能为空")
    @Schema(description = "开始日期", example = "2026-06-10",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private java.time.LocalDate startDate;

    /** 结束日期 */
    @NotNull(message = "结束日期不能为空")
    @Schema(description = "结束日期", example = "2026-06-12",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private java.time.LocalDate endDate;

    /** 请假原因 */
    @NotBlank(message = "请假原因不能为空")
    @Size(max = 500, message = "请假原因长度不能超过500个字符")
    @Schema(description = "请假原因", example = "家庭事务处理",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String reason;

    /**
     * 紧急联系电话
     * 支持中国大陆手机号格式
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "紧急联系电话", example = "13800138000")
    private String contactPhone;

    /** 工作交接人姓名 */
    @Schema(description = "工作交接人", example = "李四")
    private String handoverTo;

    /** 交接说明 */
    @Size(max = 500, message = "交接说明长度不能超过500个字符")
    @Schema(description = "交接说明", example = "负责前厅接待工作交接给李四")
    private String handoverNote;

    // ==================== Getter & Setter 方法 ====================

    public String getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(String leaveType) {
        this.leaveType = leaveType;
    }

    public java.time.LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(java.time.LocalDate startDate) {
        this.startDate = startDate;
    }

    public java.time.LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(java.time.LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getHandoverTo() {
        return handoverTo;
    }

    public void setHandoverTo(String handoverTo) {
        this.handoverTo = handoverTo;
    }

    public String getHandoverNote() {
        return handoverNote;
    }

    public void setHandoverNote(String handoverNote) {
        this.handoverNote = handoverNote;
    }
}
