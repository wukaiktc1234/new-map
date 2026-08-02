package com.foodtraceability.dto.approval;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 员工日常审批创建DTO
 * 用于提交各类审批申请的通用请求体
 * 包含审批类型、标题、优先级、备注及各类型的表单JSON数据
 */
@Schema(description = "员工日常审批创建DTO")
public class EmployeeApprovalCreateDTO {

    /**
     * 审批类型
     * leave=请假, overtime=加班, swap=换班, travel=出差, reimbursement=报销, requisition=领用
     */
    @NotBlank(message = "审批类型不能为空")
    @Pattern(regexp = "^(leave|overtime|swap|travel|reimbursement|requisition)$", message = "审批类型不合法")
    @Schema(description = "审批类型", example = "leave",
            allowableValues = {"leave", "overtime", "swap", "travel", "reimbursement", "requisition"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String type;

    /** 标题（如 "年假申请-张三"） */
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200个字符")
    @Schema(description = "标题", example = "年假申请-张三", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    /**
     * 优先级
     * normal=普通(默认), urgent=紧急, critical=特急
     * 允许为空，默认为normal
     */
    @Pattern(regexp = "^(normal|urgent|critical)$", message = "优先级不合法")
    @Schema(description = "优先级（默认normal）", example = "normal",
            allowableValues = {"normal", "urgent", "critical"})
    private String priority;

    /** 备注（可选，用于补充说明） */
    @Size(max = 500, message = "备注长度不能超过500个字符")
    @Schema(description = "备注", example = "需要提前安排工作交接")
    private String remark;

    /**
     * 各类型的表单JSON数据
     * 根据type不同，JSON结构对应不同的CreateDTO
     * leave -> LeaveCreateDTO
     * overtime -> OvertimeCreateDTO
     * swap -> SwapCreateDTO
     * travel -> TravelCreateDTO
     * reimbursement -> ReimbursementCreateDTO
     * requisition -> RequisitionCreateDTO
     */
    @NotBlank(message = "表单数据不能为空")
    @Schema(description = "各类型的表单JSON数据", example = "{\"leaveType\":\"annual\",\"startDate\":\"2026-06-10\",\"endDate\":\"2026-06-12\",\"reason\":\"家庭事务\"}",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String formJson;

    // ==================== Getter & Setter 方法 ====================

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getFormJson() {
        return formJson;
    }

    public void setFormJson(String formJson) {
        this.formJson = formJson;
    }
}
