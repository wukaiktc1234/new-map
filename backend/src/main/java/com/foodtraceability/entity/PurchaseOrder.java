package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 采购订单实体类
 * 用于管理采购系统中的采购订单信息
 */
@TableName("purchase_orders")
@Schema(description = "采购订单实体")
public class PurchaseOrder {

    /**
     * 订单主键ID（自增）
     */
    @TableId(value = "order_id", type = IdType.AUTO)
    @Schema(description = "订单主键ID", example = "1")
    private Long orderId;

    /**
     * 订单ID字符串（对外唯一标识）
     */
    @TableField("order_id_str")
    @Schema(description = "订单ID字符串", example = "PO20260425001")
    private String orderIdStr;

    /**
     * 订单业务编号（唯一）
     */
    @TableField("order_no")
    @Schema(description = "订单业务编号", example = "PO20260425001")
    private String orderNo;

    /**
     * 订单编号（旧字段，对应DB的order_code列）
     */
    @TableField("order_code")
    @Schema(description = "订单编号")
    private String orderCode;

    /**
     * 供应商ID
     */
    @TableField("supplier_id")
    @Schema(description = "供应商ID", example = "1")
    private Long supplierId;

    /**
     * 供应商名称
     */
    @TableField("supplier_name")
    @Schema(description = "供应商名称")
    private String supplierName;

    /**
     * 入库仓库ID
     */
    @TableField("warehouse_id")
    @Schema(description = "入库仓库ID")
    private Long warehouseId;

    /**
     * 总金额（单位：分）
     */
    @TableField("total_amount")
    @Schema(description = "总金额（分）", example = "100000")
    private Long totalAmount;

    /**
     * 税额（单位：分）
     */
    @TableField("tax_amount")
    @Schema(description = "税额（分）")
    private Long taxAmount;

    /**
     * 折扣金额（单位：分）
     */
    @TableField("discount_amount")
    @Schema(description = "折扣金额（分）")
    private Long discountAmount;

    /**
     * 实付金额（单位：分）
     */
    @TableField("final_amount")
    @Schema(description = "实付金额（分）")
    private Long finalAmount;

    /**
     * 订单状态（数字编码：0草稿 1待审核 2已审核 3部分入库 4已完成 5已取消 6已下单）
     */
    @TableField("order_status")
    @Schema(description = "订单状态（数字编码）")
    private Integer orderStatus;

    /**
     * 付款状态（0未付 1部分支付 2已支付）
     */
    @TableField("payment_status")
    @Schema(description = "付款状态")
    private Integer paymentStatus;

    /**
     * 订单状态（DB字段，字符串语义化：pending/approved/etc）
     */
    @TableField("status")
    @Schema(description = "订单状态", example = "pending")
    private String status;

    /**
     * 期望到货日期
     */
    @TableField("expected_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "期望到货日期")
    private LocalDate expectedDate;

    /**
     * 下单日期
     */
    @TableField("order_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "下单日期")
    private LocalDate orderDate;

    /**
     * 门店ID
     */
    @TableField("store_id")
    @Schema(description = "门店ID")
    private Long storeId;

    /**
     * 采购申请ID
     */
    @TableField("request_id")
    @Schema(description = "采购申请ID")
    private String requestId;

    /**
     * 采购申请编号
     */
    @TableField("request_no")
    @Schema(description = "采购申请编号")
    private String requestNo;

    /**
     * 采购合同ID
     */
    @TableField("contract_id")
    @Schema(description = "采购合同ID")
    private String contractId;

    /**
     * 采购合同编号
     */
    @TableField("contract_no")
    @Schema(description = "采购合同编号")
    private String contractNo;

    /**
     * 来源类型（manual/manual等）
     */
    @TableField("source_type")
    @Schema(description = "来源类型", example = "manual")
    private String sourceType;

    /**
     * 优先级（normal/high/low等）
     */
    @TableField("priority")
    @Schema(description = "优先级", example = "normal")
    private String priority;

    /**
     * 采购类型（direct-直采，agency-代采等）
     */
    @TableField("purchase_type")
    @Schema(description = "采购类型")
    private String purchaseType;

    /**
     * 联系人
     */
    @TableField("contact_person")
    @Schema(description = "联系人")
    private String contactPerson;

    /**
     * 联系电话
     */
    @TableField("contact_phone")
    @Schema(description = "联系电话")
    private String contactPhone;

    /**
     * 已付金额（单位：分）
     */
    @TableField("paid_amount")
    @Schema(description = "已付金额（分）")
    private Long paidAmount;

    /**
     * 预算ID
     */
    @TableField("budget_id")
    @Schema(description = "预算ID")
    private String budgetId;

    /**
     * 预算状态
     */
    @TableField("budget_status")
    @Schema(description = "预算状态")
    private String budgetStatus;

    /**
     * 关联采购计划ID（purchase_plan.plan_id）
     */
    @TableField("plan_id")
    @Schema(description = "关联采购计划ID")
    private Long planId;

    /**
     * 审批人ID
     */
    @TableField("approval_user_id")
    @Schema(description = "审批人ID")
    private Long approvalUserId;

    /**
     * 审批时间
     */
    @TableField("approval_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "审批时间")
    private LocalDateTime approvalTime;

    /**
     * 审批备注
     */
    @TableField("approval_remark")
    @Schema(description = "审批备注")
    private String approvalRemark;

    /**
     * 订单备注
     */
    @TableField("remark")
    @Schema(description = "订单备注")
    private String remark;

    /**
     * 创建人ID
     */
    @TableField("create_user_id")
    @Schema(description = "创建人ID")
    private Long createUserId;
    /** 创建人姓名（非表字段，查询时填充） */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String createByName;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标记（0未删除 1已删除）
     */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记", example = "0")
    private Integer deleted;

    /**
     * 乐观锁版本号
     */
    @Version
    @TableField("version")
    @Schema(description = "版本号", example = "0")
    private Integer version;

    /**
     * 订单明细列表（非数据库字段）
     */
    @TableField(exist = false)
    @Schema(description = "订单明细列表")
    private List<PurchaseOrderItem> items;

    // ==================== Getter & Setter ====================

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderIdStr() {
        return orderIdStr;
    }

    public void setOrderIdStr(String orderIdStr) {
        this.orderIdStr = orderIdStr;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
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

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Long getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(Long taxAmount) {
        this.taxAmount = taxAmount;
    }

    public Long getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(Long discountAmount) {
        this.discountAmount = discountAmount;
    }

    public Long getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(Long finalAmount) {
        this.finalAmount = finalAmount;
    }

    public Integer getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(Integer orderStatus) {
        this.orderStatus = orderStatus;
    }

    public Integer getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(Integer paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getExpectedDate() {
        return expectedDate;
    }

    public void setExpectedDate(LocalDate expectedDate) {
        this.expectedDate = expectedDate;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
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

    public Long getApprovalUserId() {
        return approvalUserId;
    }

    public void setApprovalUserId(Long approvalUserId) {
        this.approvalUserId = approvalUserId;
    }

    public LocalDateTime getApprovalTime() {
        return approvalTime;
    }

    public void setApprovalTime(LocalDateTime approvalTime) {
        this.approvalTime = approvalTime;
    }

    public String getApprovalRemark() {
        return approvalRemark;
    }

    public void setApprovalRemark(String approvalRemark) {
        this.approvalRemark = approvalRemark;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getCreateByName() { return createByName; }
    public void setCreateByName(String createByName) { this.createByName = createByName; }

    public Long getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(Long createUserId) {
        this.createUserId = createUserId;
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

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public List<PurchaseOrderItem> getItems() {
        return items;
    }

    public void setItems(List<PurchaseOrderItem> items) {
        this.items = items;
    }
}
