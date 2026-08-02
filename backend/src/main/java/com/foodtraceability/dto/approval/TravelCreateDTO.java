package com.foodtraceability.dto.approval;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 出差申请创建DTO
 * 用于出差类型审批的详细表单数据
 * 包含目的地、目的、日期、预算、交通方式等信息
 */
@Schema(description = "出差申请创建DTO")
public class TravelCreateDTO {

    /** 目的地城市 */
    @NotBlank(message = "目的地不能为空")
    @Size(max = 100, message = "目的地长度不能超过100个字符")
    @Schema(description = "目的地城市", example = "上海",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String destination;

    /** 出差目的 */
    @NotBlank(message = "出差目的不能为空")
    @Size(max = 500, message = "出差目的长度不能超过500个字符")
    @Schema(description = "出差目的", example = "参加行业展会及客户拜访",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String travelPurpose;

    /** 开始日期 */
    @NotNull(message = "开始日期不能为空")
    @Schema(description = "开始日期", example = "2026-06-15",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate startDate;

    /** 结束日期 */
    @NotNull(message = "结束日期不能为空")
    @Schema(description = "结束日期", example = "2026-06-17",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate endDate;

    /**
     * 交通方式
     * plane=飞机, train=火车, car=汽车, bus=大巴
     * 可选，根据实际情况选择
     */
    @Pattern(regexp = "^(plane|train|car|bus)?$", message = "交通方式不合法")
    @Schema(description = "交通方式", example = "train",
            allowableValues = {"plane", "train", "car", "bus"})
    private String transportType;

    /** 是否需要住宿 */
    @Schema(description = "是否需要住宿", example = "true")
    private Boolean hotelRequired;

    /**
     * 预估预算（单位：元）
     * 前端传入元，后端转换为分存储到数据库
     */
    @NotNull(message = "预估预算不能为空")
    @DecimalMin(value = "0.01", message = "预估预算必须大于0")
    @Schema(description = "预估预算（单位：元）", example = "5000.00",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal estimatedBudget;

    /** 关联任务ID（如因公出差关联的任务） */
    @Schema(description = "关联任务ID", example = "task001")
    private String relatedTaskId;

    // ==================== Getter & Setter 方法 ====================

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

    public BigDecimal getEstimatedBudget() {
        return estimatedBudget;
    }

    public void setEstimatedBudget(BigDecimal estimatedBudget) {
        this.estimatedBudget = estimatedBudget;
    }

    public String getRelatedTaskId() {
        return relatedTaskId;
    }

    public void setRelatedTaskId(String relatedTaskId) {
        this.relatedTaskId = relatedTaskId;
    }
}
