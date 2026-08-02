package com.foodtraceability.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 退款查询条件DTO
 * 用于多条件筛选退款申请
 */
@Schema(description = "退款查询条件")
public class OrderRefundQueryDTO {

    /** 当前页码 */
    @Schema(description = "当前页码", example = "1")
    private Integer page = 1;

    /** 每页条数 */
    @Schema(description = "每页条数", example = "10")
    private Integer size = 10;

    /** 退款单号（模糊搜索） */
    @Schema(description = "退款单号（模糊搜索）")
    private String refundNo;

    /** 订单编号（模糊搜索） */
    @Schema(description = "订单编号（模糊搜索）")
    private String orderCode;

    /** 退款状态: 1待审核 2已通过 3已拒绝 4已执行 */
    @Schema(description = "退款状态: 1待审核 2已通过 3已拒绝 4已执行")
    private Integer refundStatus;

    /** 开始时间 */
    @Schema(description = "开始时间")
    private String startTime;

    /** 结束时间 */
    @Schema(description = "结束时间")
    private String endTime;

    // Getter和Setter方法
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
    public String getRefundNo() { return refundNo; }
    public void setRefundNo(String refundNo) { this.refundNo = refundNo; }
    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }
    public Integer getRefundStatus() { return refundStatus; }
    public void setRefundStatus(Integer refundStatus) { this.refundStatus = refundStatus; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
}
