package com.foodtraceability.dto.purchase;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 采购结算单创建 DTO
 *
 * <p>settlementNo 由后端自动生成（STL+yyyyMMdd+4位序号），不接受前端传入。
 * paidAmount/unpaidAmount 由后端根据 totalAmount 自动计算（初始 paidAmount=0，unpaidAmount=totalAmount）。
 * status 由后端初始化为 0（待结算），invoiceStatus 初始化为 0（未开票）。</p>
 *
 * <p>金额单位：分（Long），前端传入时需将元转换为分。</p>
 */
@Schema(description = "采购结算单创建 DTO")
public class PurchaseSettlementCreateDTO {

    /** 关联的采购订单ID */
    @NotNull(message = "采购订单ID不能为空")
    @Schema(description = "采购订单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long orderId;

    /** 采购订单编号（冗余，前端从订单选择时带入） */
    @Size(max = 50, message = "采购订单编号长度不能超过50")
    @Schema(description = "采购订单编号")
    private String orderNo;

    /** 供应商ID（前端从订单选择时带入，后端不强制校验） */
    @Schema(description = "供应商ID")
    private Long supplierId;

    /** 供应商名称（冗余，前端从订单选择时带入） */
    @Size(max = 200, message = "供应商名称长度不能超过200")
    @Schema(description = "供应商名称")
    private String supplierName;

    /** 总金额（单位：分） */
    @NotNull(message = "总金额不能为空")
    @Min(value = 0, message = "总金额不能为负数")
    @Schema(description = "总金额（分）", requiredMode = Schema.RequiredMode.REQUIRED, example = "159000")
    private Long totalAmount;

    /** 到期日期 */
    @NotNull(message = "到期日期不能为空")
    @Schema(description = "到期日期", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate dueDate;

    /** 付款方式 */
    @Size(max = 50, message = "付款方式长度不能超过50")
    @Schema(description = "付款方式", example = "银行转账")
    private String paymentMethod;

    /** 备注 */
    @Size(max = 500, message = "备注长度不能超过500")
    @Schema(description = "备注")
    private String remark;

    // ==================== Getter & Setter ====================

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
