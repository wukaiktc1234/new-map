package com.foodtraceability.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

/**
 * 采购订单查询DTO
 * 用于接收采购订单列表的查询条件
 */
@Schema(description = "采购订单查询条件")
public class PurchaseOrderQueryDTO {

    /**
     * 订单编号（模糊查询）
     */
    @Schema(description = "订单编号（模糊查询）")
    private String orderCode;
    private String requestNo;

    /**
     * 供应商ID
     */
    @Schema(description = "供应商ID")
    private Long supplierId;

    /**
     * 订单状态（0草稿 1待审核 2已审核 3部分入库 4已完成 5已取消）
     */
    @Schema(description = "订单状态（0-草稿, 1-待审核, 2-已审核, 3-部分入库, 4-已完成, 5-已取消）")
    private Integer orderStatus;

    /**
     * 付款状态（0未付 1部分支付 2已支付）
     */
    @Schema(description = "付款状态（0-未付, 1-部分支付, 2-已支付）")
    private Integer paymentStatus;

    /**
     * 开始日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "开始日期")
    private LocalDate startDate;

    /**
     * 结束日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "结束日期")
    private LocalDate endDate;

    /**
     * 创建人ID（我的单据筛选）
     */
    @Schema(description = "创建人ID（我的单据筛选）")
    private Long createUserId;

    /**
     * 当前页码
     */
    @Schema(description = "当前页码", example = "1")
    private Integer current = 1;

    /**
     * 每页条数
     */
    @Schema(description = "每页条数", example = "10")
    private Integer size = 10;

    // ==================== Getter & Setter ====================

    public String getOrderCode() {
        return orderCode;
    }

    public String getRequestNo() {
        return requestNo;
    }

    public void setRequestNo(String requestNo) {
        this.requestNo = requestNo;
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

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Long getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(Long createUserId) {
        this.createUserId = createUserId;
    }

    public Integer getCurrent() {
        return current;
    }

    public void setCurrent(Integer current) {
        this.current = current;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
