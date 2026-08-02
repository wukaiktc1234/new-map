package com.foodtraceability.entity.approval;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 出差申请详情实体
 * 对应数据库表 travel_requests
 * 存储出差目的地、目的、预算、交通方式等详细信息
 */
@TableName("travel_requests")
@Schema(description = "出差申请详情实体")
public class TravelRequestEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 出差记录ID（UUID自动生成） */
    @TableId(type = IdType.ASSIGN_UUID, value = "request_id")
    @Schema(description = "出差记录ID", example = "t1u2v3w4x5y6...")
    private String id;

    /** 关联审批主表ID */
    @TableField("approval_id")
    @Schema(description = "关联审批主表ID", example = "approval001")
    private String approvalId;

    /** 申请人ID */
    @TableField("employee_id")
    @Schema(description = "申请人ID", example = "emp001")
    private String employeeId;

    /** 目的地城市 */
    @TableField("destination")
    @Schema(description = "目的地城市", example = "上海")
    private String destination;

    /** 出差目的 */
    @TableField("travel_purpose")
    @Schema(description = "出差目的", example = "参加行业展会及客户拜访")
    private String travelPurpose;

    /** 开始日期 */
    @TableField("start_date")
    @Schema(description = "开始日期", example = "2026-06-15")
    private LocalDate startDate;

    /** 结束日期 */
    @TableField("end_date")
    @Schema(description = "结束日期", example = "2026-06-17")
    private LocalDate endDate;

    /** 出差天数 */
    @TableField("days")
    @Schema(description = "出差天数", example = "3")
    private Integer days;

    /**
     * 预估预算（单位：分）
     * 前端传入元，后端转换为分存储
     */
    @TableField("estimated_budget")
    @Schema(description = "预估预算（分）", example = "500000")
    private BigDecimal estimatedBudget;

    /**
     * 实际花费（单位：分）
     * 报销结算后回填
     */
    @TableField("actual_amount")
    @Schema(description = "实际花费（分）", example = "450000")
    private BigDecimal actualAmount;

    /**
     * 预支金额（单位：分）
     * 出差前的预付款项
     */
    @TableField("advance_payment")
    @Schema(description = "预支金额（分）", example = "200000")
    private BigDecimal advancePayment;

    /**
     * 交通方式
     * plane=飞机, train=火车, car=汽车, bus=大巴
     */
    @TableField("transport_type")
    @Schema(description = "交通方式", example = "train")
    private String transportType;

    /** 是否需要住宿 */
    @TableField("hotel_required")
    @Schema(description = "是否需要住宿", example = "true")
    private Boolean hotelRequired;

    /** 住宿城市（可能与目的地不同） */
    @TableField("hotel_city")
    @Schema(description = "住宿城市", example = "上海浦东新区")
    private String hotelCity;

    /** 关联任务ID（如因公出差关联的任务） */
    @TableField("related_task_id")
    @Schema(description = "关联任务ID", example = "task001")
    private String relatedTaskId;

    /**
     * 日补标准（单位：分/天）
     * 根据目的地城市级别确定
     */
    @TableField("daily_allowance")
    @Schema(description = "日补标准（分/天）", example = "30000")
    private BigDecimal dailyAllowance;

    /** 创建时间 */
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    /** 更新时间 */
    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;

    /** 逻辑删除标记（0=未删除 1=已删除） */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记")
    private Integer deleted;

    /** 创建人ID */
    @TableField("created_by")
    @Schema(description = "创建人ID")
    private String createdBy;

    /** 更新人ID */
    @TableField("updated_by")
    @Schema(description = "更新人ID")
    private String updatedBy;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * 获取请求ID（别名方法，委托给 getId()）
     * 兼容历史调用方使用 requestId 语义访问主键
     * @return 请求ID
     */
    public String getRequestId() {
        return this.id;
    }

    /**
     * 设置请求ID（别名方法，委托给 setId()）
     * 兼容历史调用方使用 requestId 语义访问主键
     * @param requestId 请求ID
     */
    public void setRequestId(String requestId) {
        this.id = requestId;
    }

    public String getApprovalId() {
        return approvalId;
    }

    public void setApprovalId(String approvalId) {
        this.approvalId = approvalId;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getTravelPurpose() {
        return travelPurpose;
    }

    public void setTravelPurpose(String travelPurpose) {
        this.travelPurpose = travelPurpose;
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

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public BigDecimal getEstimatedBudget() {
        return estimatedBudget;
    }

    public void setEstimatedBudget(BigDecimal estimatedBudget) {
        this.estimatedBudget = estimatedBudget;
    }

    public BigDecimal getActualAmount() {
        return actualAmount;
    }

    public void setActualAmount(BigDecimal actualAmount) {
        this.actualAmount = actualAmount;
    }

    public BigDecimal getAdvancePayment() {
        return advancePayment;
    }

    public void setAdvancePayment(BigDecimal advancePayment) {
        this.advancePayment = advancePayment;
    }

    public String getTransportType() {
        return transportType;
    }

    public void setTransportType(String transportType) {
        this.transportType = transportType;
    }

    public Boolean getHotelRequired() {
        return hotelRequired;
    }

    public void setHotelRequired(Boolean hotelRequired) {
        this.hotelRequired = hotelRequired;
    }

    public String getHotelCity() {
        return hotelCity;
    }

    public void setHotelCity(String hotelCity) {
        this.hotelCity = hotelCity;
    }

    public String getRelatedTaskId() {
        return relatedTaskId;
    }

    public void setRelatedTaskId(String relatedTaskId) {
        this.relatedTaskId = relatedTaskId;
    }

    public BigDecimal getDailyAllowance() {
        return dailyAllowance;
    }

    public void setDailyAllowance(BigDecimal dailyAllowance) {
        this.dailyAllowance = dailyAllowance;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public LocalDateTime getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(LocalDateTime updatedTime) {
        this.updatedTime = updatedTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
