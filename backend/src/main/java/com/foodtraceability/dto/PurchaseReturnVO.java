package com.foodtraceability.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 采购退货单VO
 */
@Schema(description = "采购退货单视图对象")
public class PurchaseReturnVO {

    @Schema(description = "退货单ID")
    private Long id;

    @Schema(description = "退货单号")
    private String returnNo;

    @Schema(description = "关联原入库单ID")
    private Long stockinId;

    @Schema(description = "关联原入库单号")
    private String stockinNo;

    @Schema(description = "关联采购订单ID")
    private Long orderId;

    @Schema(description = "关联采购订单号")
    private String orderNo;

    @Schema(description = "供应商ID")
    private Long supplierId;

    @Schema(description = "供应商名称")
    private String supplierName;

    @Schema(description = "退货仓库ID")
    private Long warehouseId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "退货日期")
    private LocalDate returnDate;

    @Schema(description = "退货总数量")
    private BigDecimal totalQuantity;

    @Schema(description = "退货总金额（单位：分）")
    private Long totalAmount;

    @Schema(description = "退款方式：offset(冲抵) / cash(现金退款)")
    private String refundMethod;

    @Schema(description = "状态：pending(待审批) / approved(已通过) / rejected(已驳回) / completed(已完成)")
    private String status;

    @Schema(description = "审批备注")
    private String approvalRemark;

    @Schema(description = "关联生成的红字应付单ID")
    private Long relatedPayableId;

    @Schema(description = "备注")
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "退货明细列表")
    private List<PurchaseReturnItemVO> items;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReturnNo() {
        return returnNo;
    }

    public void setReturnNo(String returnNo) {
        this.returnNo = returnNo;
    }

    public Long getStockinId() {
        return stockinId;
    }

    public void setStockinId(Long stockinId) {
        this.stockinId = stockinId;
    }

    public String getStockinNo() {
        return stockinNo;
    }

    public void setStockinNo(String stockinNo) {
        this.stockinNo = stockinNo;
    }

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

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public BigDecimal getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(BigDecimal totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getRefundMethod() {
        return refundMethod;
    }

    public void setRefundMethod(String refundMethod) {
        this.refundMethod = refundMethod;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getApprovalRemark() {
        return approvalRemark;
    }

    public void setApprovalRemark(String approvalRemark) {
        this.approvalRemark = approvalRemark;
    }

    public Long getRelatedPayableId() {
        return relatedPayableId;
    }

    public void setRelatedPayableId(Long relatedPayableId) {
        this.relatedPayableId = relatedPayableId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public List<PurchaseReturnItemVO> getItems() {
        return items;
    }

    public void setItems(List<PurchaseReturnItemVO> items) {
        this.items = items;
    }
}
