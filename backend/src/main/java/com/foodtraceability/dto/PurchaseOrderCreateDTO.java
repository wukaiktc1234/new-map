package com.foodtraceability.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 采购订单创建DTO
 * 用于接收创建采购订单时的请求数据
 */
@Schema(description = "采购订单创建请求")
public class PurchaseOrderCreateDTO {

    /**
     * 供应商ID
     */
    @NotNull(message = "供应商ID不能为空")
    @Schema(description = "供应商ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long supplierId;

    /**
     * 入库仓库ID
     */
    @Schema(description = "入库仓库ID", example = "1")
    private Long warehouseId;

    /**
     * 订单明细列表
     */
    @Valid
    @NotEmpty(message = "订单明细不能为空")
    @Size(max = 50, message = "订单明细不能超过50条")
    @Schema(description = "订单明细列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<PurchaseOrderItemDTO> items;

    /**
     * 折扣金额（单位：分）
     */
    @Min(value = 0, message = "折扣金额不能为负数")
    @Schema(description = "折扣金额（分）", example = "0")
    private Long discountAmount = 0L;

    /**
     * 期望到货日期
     */
    @Schema(description = "期望到货日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expectedDate;

    /**
     * 备注
     */
    @Size(max = 500, message = "备注不能超过500个字符")
    @Schema(description = "备注")
    private String remark;

    /**
     * 采购申请ID
     */
    @Schema(description = "采购申请ID")
    private String requestId;

    /**
     * 采购申请编号
     */
    @Schema(description = "采购申请编号")
    private String requestNo;

    /**
     * 采购合同ID
     */
    @Schema(description = "采购合同ID")
    private String contractId;

    /**
     * 采购合同编号
     */
    @Schema(description = "采购合同编号")
    private String contractNo;

    /**
     * 来源类型（manual-手工创建，request-申请转单等）
     */
    @Schema(description = "来源类型")
    private String sourceType;

    /**
     * 优先级（normal-普通，urgent-紧急，high-高，low-低）
     */
    @Schema(description = "优先级")
    private String priority;

    /**
     * 采购类型（direct-直采，agency-代采等）
     */
    @Schema(description = "采购类型")
    private String purchaseType;

    /**
     * 联系人
     */
    @Size(max = 100, message = "联系人不能超过100个字符")
    @Schema(description = "联系人")
    private String contactPerson;

    /**
     * 联系电话
     */
    @Size(max = 50, message = "联系电话不能超过50个字符")
    @Schema(description = "联系电话")
    private String contactPhone;

    /**
     * 已付金额（单位：分）
     */
    @Min(value = 0, message = "已付金额不能为负数")
    @Schema(description = "已付金额（分）")
    private Long paidAmount = 0L;

    /**
     * 预算ID
     */
    @Schema(description = "预算ID")
    private String budgetId;

    /**
     * 预算状态
     */
    @Schema(description = "预算状态")
    private String budgetStatus;

    /**
     * 关联采购计划ID（purchase_plan.plan_id）
     */
    @Schema(description = "关联采购计划ID")
    private Long planId;

    // ==================== Getter & Setter ====================

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public List<PurchaseOrderItemDTO> getItems() {
        return items;
    }

    public void setItems(List<PurchaseOrderItemDTO> items) {
        this.items = items;
    }

    public Long getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(Long discountAmount) {
        this.discountAmount = discountAmount;
    }

    public LocalDate getExpectedDate() {
        return expectedDate;
    }

    public void setExpectedDate(LocalDate expectedDate) {
        this.expectedDate = expectedDate;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getRequestNo() {
        return requestNo;
    }

    public void setRequestNo(String requestNo) {
        this.requestNo = requestNo;
    }

    public String getContractId() {
        return contractId;
    }

    public void setContractId(String contractId) {
        this.contractId = contractId;
    }

    public String getContractNo() {
        return contractNo;
    }

    public void setContractNo(String contractNo) {
        this.contractNo = contractNo;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getPurchaseType() {
        return purchaseType;
    }

    public void setPurchaseType(String purchaseType) {
        this.purchaseType = purchaseType;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public Long getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(Long paidAmount) {
        this.paidAmount = paidAmount;
    }

    public String getBudgetId() {
        return budgetId;
    }

    public void setBudgetId(String budgetId) {
        this.budgetId = budgetId;
    }

    public String getBudgetStatus() {
        return budgetStatus;
    }

    public void setBudgetStatus(String budgetStatus) {
        this.budgetStatus = budgetStatus;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    /**
     * 计算订单总金额（所有明细金额之和）
     * @return 总金额（分）
     */
    public Long calculateTotalAmount() {
        if (items == null || items.isEmpty()) {
            return 0L;
        }
        long total = 0L;
        for (PurchaseOrderItemDTO item : items) {
            total += item.calculateAmount();
        }
        return total;
    }

    /**
     * 计算实付金额（总金额 - 折扣金额）
     * @return 实付金额（分）
     */
    public Long calculateFinalAmount() {
        long total = calculateTotalAmount();
        long discount = discountAmount != null ? discountAmount : 0L;
        return Math.max(total - discount, 0L);
    }
}
