package com.foodtraceability.dto.purchase;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * 采购计划更新 DTO（部分更新）
 *
 * <p>仅允许在 status=0（草稿）状态下更新。</p>
 */
@Schema(description = "采购计划更新 DTO")
public class PurchasePlanUpdateDTO {

    /** 部门ID */
    @NotNull(message = "部门ID不能为空")
    @Schema(description = "部门ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long departmentId;

    /** 部门名称 */
    @Size(max = 100, message = "部门名称长度不能超过100")
    @Schema(description = "部门名称")
    private String departmentName;

    /** 计划日期 */
    @NotNull(message = "计划日期不能为空")
    @Schema(description = "计划日期", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate planDate;

    /** 备注 */
    @Size(max = 500, message = "备注长度不能超过500")
    @Schema(description = "备注")
    private String remark;

    /** 计划明细列表（覆盖式更新：先删后插） */
    @NotEmpty(message = "计划明细不能为空")
    @Valid
    @Schema(description = "计划明细列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<PurchasePlanItemDTO> items;

    // ==================== Getter & Setter ====================

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public LocalDate getPlanDate() {
        return planDate;
    }

    public void setPlanDate(LocalDate planDate) {
        this.planDate = planDate;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public List<PurchasePlanItemDTO> getItems() {
        return items;
    }

    public void setItems(List<PurchasePlanItemDTO> items) {
        this.items = items;
    }
}
