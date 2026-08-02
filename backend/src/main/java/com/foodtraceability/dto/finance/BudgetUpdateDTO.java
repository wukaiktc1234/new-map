package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 预算更新DTO
 */
@Schema(description = "预算更新请求")
public class BudgetUpdateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "预算ID不能为空")
    @Schema(description = "预算ID")
    private Long budgetId;

    @Schema(description = "预算金额（分）")
    private Long budgetAmount;

    @Schema(description = "责任部门ID")
    private Long responsibleDeptId;

    @Schema(description = "备注")
    private String remark;

    public Long getBudgetId() { return budgetId; }
    public void setBudgetId(Long budgetId) { this.budgetId = budgetId; }
    public Long getBudgetAmount() { return budgetAmount; }
    public void setBudgetAmount(Long budgetAmount) { this.budgetAmount = budgetAmount; }
    public Long getResponsibleDeptId() { return responsibleDeptId; }
    public void setResponsibleDeptId(Long responsibleDeptId) { this.responsibleDeptId = responsibleDeptId; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
