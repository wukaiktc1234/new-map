package com.foodtraceability.dto.trace;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 召回查询结果VO
 * 用于反向召回查询的返回结果
 */
@Schema(description = "召回查询结果")
public class RecallQueryResultVO {

    /** 问题原料/批次信息 */
    @Schema(description = "问题目标信息")
    private TargetInfo targetInfo;

    /** 受影响的追溯码列表 */
    @Schema(description = "受影响的追溯码列表")
    private List<AffectedTraceCodeVO> affectedTraceCodes;

    /** 受影响的菜品列表 */
    @Schema(description = "受影响的菜品列表")
    private List<AffectedDishVO> affectedDishes;

    /** 受影响的订单列表 */
    @Schema(description = "受影响的订单列表")
    private List<AffectedOrderVO> affectedOrders;

    /** 统计汇总 */
    @Schema(description = "统计汇总")
    private RecallSummary summary;

    /**
     * 目标信息内部类
     */
    @Schema(description = "问题目标信息")
    public static class TargetInfo {
        @Schema(description = "目标ID")
        private Long targetId;
        @Schema(description = "目标名称")
        private String targetName;
        @Schema(description = "批次号")
        private String batchNo;
        @Schema(description = "供应商名称")
        private String supplierName;
        @Schema(description = "问题类型描述")
        private String problemDescription;

        public Long getTargetId() { return targetId; }
        public void setTargetId(Long targetId) { this.targetId = targetId; }
        public String getTargetName() { return targetName; }
        public void setTargetName(String targetName) { this.targetName = targetName; }
        public String getBatchNo() { return batchNo; }
        public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
        public String getSupplierName() { return supplierName; }
        public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
        public String getProblemDescription() { return problemDescription; }
        public void setProblemDescription(String problemDescription) { this.problemDescription = problemDescription; }
    }

    /**
     * 受影响追溯码内部类
     */
    @Schema(description = "受影响的追溯码")
    public static class AffectedTraceCodeVO {
        @Schema(description = "追溯码ID")
        private Long traceCodeId;
        @Schema(description = "追溯码")
        private String traceCode;
        @Schema(description = "目标名称")
        private String targetName;
        @Schema(description = "状态")
        private Integer status;
        @Schema(description = "状态名称")
        private String statusName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @Schema(description = "生成时间")
        private LocalDateTime createTime;

        public Long getTraceCodeId() { return traceCodeId; }
        public void setTraceCodeId(Long traceCodeId) { this.traceCodeId = traceCodeId; }
        public String getTraceCode() { return traceCode; }
        public void setTraceCode(String traceCode) { this.traceCode = traceCode; }
        public String getTargetName() { return targetName; }
        public void setTargetName(String targetName) { this.targetName = targetName; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
        public String getStatusName() { return statusName; }
        public void setStatusName(String statusName) { this.statusName = statusName; }
        public LocalDateTime getCreateTime() { return createTime; }
        public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    }

    /**
     * 受影响菜品内部类
     */
    @Schema(description = "受影响的菜品")
    public static class AffectedDishVO {
        @Schema(description = "菜品ID")
        private Long dishId;
        @Schema(description = "菜品名称")
        private String dishName;
        @Schema(description = "涉及数量")
        private Integer quantity;

        public Long getDishId() { return dishId; }
        public void setDishId(Long dishId) { this.dishId = dishId; }
        public String getDishName() { return dishName; }
        public void setDishName(String dishName) { this.dishName = dishName; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }

    /**
     * 受影响订单内部类
     * 注意：orderId 为 String 类型（订单编码如 "O1783341970544"），与 OrderNew.orderId 实际类型一致
     */
    @Schema(description = "受影响的订单")
    public static class AffectedOrderVO {
        @Schema(description = "订单ID（字符串编码）")
        private String orderId;
        @Schema(description = "订单编号")
        private String orderNumber;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @Schema(description = "下单时间")
        private LocalDateTime orderTime;
        @Schema(description = "客户信息(脱敏)")
        private String customerInfo;
        @Schema(description = "涉及份数")
        private Integer quantity;

        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public String getOrderNumber() { return orderNumber; }
        public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
        public LocalDateTime getOrderTime() { return orderTime; }
        public void setOrderTime(LocalDateTime orderTime) { this.orderTime = orderTime; }
        public String getCustomerInfo() { return customerInfo; }
        public void setCustomerInfo(String customerInfo) { this.customerInfo = customerInfo; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }

    /**
     * 召回统计汇总
     */
    @Schema(description = "召回统计汇总")
    public static class RecallSummary {
        @Schema(description = "受影响追溯码总数")
        private Integer totalTraceCodes;
        @Schema(description = "受影响菜品种类数")
        private Integer totalDishTypes;
        @Schema(description = "受影响订单总数")
        private Integer totalOrders;
        @Schema(description = "已售出数量")
        private Integer soldQuantity;
        @Schema(description = "库存中数量")
        private Integer stockQuantity;

        public Integer getTotalTraceCodes() { return totalTraceCodes; }
        public void setTotalTraceCodes(Integer totalTraceCodes) { this.totalTraceCodes = totalTraceCodes; }
        public Integer getTotalDishTypes() { return totalDishTypes; }
        public void setTotalDishTypes(Integer totalDishTypes) { this.totalDishTypes = totalDishTypes; }
        public Integer getTotalOrders() { return totalOrders; }
        public void setTotalOrders(Integer totalOrders) { this.totalOrders = totalOrders; }
        public Integer getSoldQuantity() { return soldQuantity; }
        public void setSoldQuantity(Integer soldQuantity) { this.soldQuantity = soldQuantity; }
        public Integer getStockQuantity() { return stockQuantity; }
        public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }
    }

    // 主对象Getter和Setter

    public TargetInfo getTargetInfo() { return targetInfo; }
    public void setTargetInfo(TargetInfo targetInfo) { this.targetInfo = targetInfo; }
    public List<AffectedTraceCodeVO> getAffectedTraceCodes() { return affectedTraceCodes; }
    public void setAffectedTraceCodes(List<AffectedTraceCodeVO> affectedTraceCodes) { this.affectedTraceCodes = affectedTraceCodes; }
    public List<AffectedDishVO> getAffectedDishes() { return affectedDishes; }
    public void setAffectedDishes(List<AffectedDishVO> affectedDishes) { this.affectedDishes = affectedDishes; }
    public List<AffectedOrderVO> getAffectedOrders() { return affectedOrders; }
    public void setAffectedOrders(List<AffectedOrderVO> affectedOrders) { this.affectedOrders = affectedOrders; }
    public RecallSummary getSummary() { return summary; }
    public void setSummary(RecallSummary summary) { this.summary = summary; }
}
