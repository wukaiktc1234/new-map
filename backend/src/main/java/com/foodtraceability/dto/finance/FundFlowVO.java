package com.foodtraceability.dto.finance;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 资金流水VO
 */
@Schema(description = "资金流水信息")
public class FundFlowVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "流水ID")
    private Long flowId;

    @Schema(description = "流水号")
    private String flowNo;

    @Schema(description = "银行账户ID")
    private Long accountId;

    @Schema(description = "银行账户名称")
    private String accountName;

    @Schema(description = "方向：1-收入 2-支出")
    private Integer flowDirection;

    @Schema(description = "分类：1-销售收款 2-采购付款 3-工资发放 4-税费缴纳 5-内部转账 6-其他")
    private Integer flowCategory;

    @Schema(description = "金额（单位：分）")
    private Long amount;

    @Schema(description = "交易后余额（单位：分）")
    private Long balanceAfter;

    @Schema(description = "对方名称")
    private String counterpartyName;

    @Schema(description = "对方账号")
    private String counterpartyAccount;

    @Schema(description = "业务日期")
    private LocalDate businessDate;

    @Schema(description = "关联凭证ID")
    private Long voucherId;

    @Schema(description = "备注")
    private String remark;

    /** 业务订单ID：供内部穿透/按单号搜索使用；前端列表不展示（避免外露内部ID） */
    @Schema(description = "业务订单ID（内部用，前端不展示）")
    private String orderId;

    @Schema(description = "创建人ID")
    private Long createUserId;

    /** 操作人展示名（fund_flows.created_by，如 vcashier01），供前端「操作人」列/详情展示 */
    private String createdBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "流水状态：1-待处理 2-已完成 3-已取消")
    private Integer status;

    @Schema(description = "流水状态名称：待处理/已完成/已取消")
    private String statusName;

    /**
     * POS 订单摘要（可选字段，仅详情接口返回；非 POS 销售入账流水为 null）。
     *
     * <p>刻意<b>不</b>透出 orderId：前端详情弹窗只展示人类可读信息，内部订单ID对使用者无意义。</p>
     */
    @Schema(description = "POS 订单摘要（仅详情接口，非 POS 入账流水为 null）")
    private PosOrderSummary posOrder;

    /** POS 订单摘要 */
    @Schema(description = "POS 订单摘要")
    public static class PosOrderSummary implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "订单号", example = "T202610230150")
        private String orderNumber;

        @Schema(description = "下单时间", example = "2026-10-23 22:56:20")
        private String orderTime;

        @Schema(description = "支付方式中文", example = "现金")
        private String paymentMethodName;

        @Schema(description = "订单状态中文", example = "已完成")
        private String orderStatusName;

        @Schema(description = "订单状态原始码（供前端选色）", example = "2")
        private Integer orderStatusCode;

        @Schema(description = "订单金额（元）", example = "18.00")
        private BigDecimal orderAmount;

        @Schema(description = "订单明细")
        private List<PosOrderItem> items;

        public String getOrderNumber() { return orderNumber; }

        public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

        public String getOrderTime() { return orderTime; }

        public void setOrderTime(String orderTime) { this.orderTime = orderTime; }

        public String getPaymentMethodName() { return paymentMethodName; }

        public void setPaymentMethodName(String paymentMethodName) { this.paymentMethodName = paymentMethodName; }

        public String getOrderStatusName() { return orderStatusName; }

        public void setOrderStatusName(String orderStatusName) { this.orderStatusName = orderStatusName; }

        public Integer getOrderStatusCode() { return orderStatusCode; }

        public void setOrderStatusCode(Integer orderStatusCode) { this.orderStatusCode = orderStatusCode; }

        public BigDecimal getOrderAmount() { return orderAmount; }

        public void setOrderAmount(BigDecimal orderAmount) { this.orderAmount = orderAmount; }

        public List<PosOrderItem> getItems() { return items; }

        public void setItems(List<PosOrderItem> items) { this.items = items; }
    }

    /** POS 订单明细行（金额单位：元） */
    @Schema(description = "POS 订单明细行")
    public static class PosOrderItem implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "菜品名称")
        private String name;

        @Schema(description = "单价（元）")
        private BigDecimal price;

        @Schema(description = "数量")
        private Integer quantity;

        @Schema(description = "小计（元）")
        private BigDecimal amount;

        public String getName() { return name; }

        public void setName(String name) { this.name = name; }

        public BigDecimal getPrice() { return price; }

        public void setPrice(BigDecimal price) { this.price = price; }

        public Integer getQuantity() { return quantity; }

        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public BigDecimal getAmount() { return amount; }

        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }

    public PosOrderSummary getPosOrder() { return posOrder; }

    public void setPosOrder(PosOrderSummary posOrder) { this.posOrder = posOrder; }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public Long getFlowId() {
        return flowId;
    }

    public void setFlowId(Long flowId) {
        this.flowId = flowId;
    }

    public String getFlowNo() {
        return flowNo;
    }

    public void setFlowNo(String flowNo) {
        this.flowNo = flowNo;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public Integer getFlowDirection() {
        return flowDirection;
    }

    public void setFlowDirection(Integer flowDirection) {
        this.flowDirection = flowDirection;
    }

    public Integer getFlowCategory() {
        return flowCategory;
    }

    public void setFlowCategory(Integer flowCategory) {
        this.flowCategory = flowCategory;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public Long getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(Long balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public String getCounterpartyName() {
        return counterpartyName;
    }

    public void setCounterpartyName(String counterpartyName) {
        this.counterpartyName = counterpartyName;
    }

    public String getCounterpartyAccount() {
        return counterpartyAccount;
    }

    public void setCounterpartyAccount(String counterpartyAccount) {
        this.counterpartyAccount = counterpartyAccount;
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public void setBusinessDate(LocalDate businessDate) {
        this.businessDate = businessDate;
    }

    public Long getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(Long voucherId) {
        this.voucherId = voucherId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }
}
