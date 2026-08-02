package com.foodtraceability.dto.approval;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Size;

/**
 * 员工日常审批更新DTO
 * 用于更新审批信息（仅限撤回时补充说明等有限场景）
 * 大部分字段不可修改，需通过特定操作接口处理
 */
@Schema(description = "员工日常审批更新DTO")
public class EmployeeApprovalUpdateDTO {

    /** 备注说明（补充或修改备注信息） */
    @Size(max = 500, message = "备注长度不能超过500个字符")
    @Schema(description = "备注", example = "补充：已与部门经理确认工作安排")
    private String remark;

    // ==================== Getter & Setter 方法 ====================

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
