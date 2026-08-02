package com.foodtraceability.dto.schedule;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

/**
 * 班次类型创建DTO
 */
@Schema(description = "班次类型创建DTO")
public class ScheduleShiftTypeCreateDTO {

    @NotBlank(message = "班次编码不能为空")
    @Schema(description = "班次编码(内部标识)", example = "morning", required = true)
    private String shiftCode;

    @NotBlank(message = "班次名称不能为空")
    @Schema(description = "班次显示名称", example = "早班(A)", required = true)
    private String shiftName;

    @NotNull(message = "门店ID不能为空")
    @Schema(description = "门店ID", example = "1", required = true)
    private Long storeId;

    @NotNull(message = "开始时间不能为空")
    @Schema(description = "开始时间(HH:mm格式)", example = "08:00", required = true)
    private LocalTime startTime;

    @NotNull(message = "结束时间不能为空")
    @Schema(description = "结束时间(HH:mm格式,跨夜用次日时间如01:00)", example = "16:00", required = true)
    private LocalTime endTime;

    @Schema(description = "颜色标识(CSS颜色值或CSS变量名)", example = "#409EFF")
    private String color;

    @Schema(description = "图标", example = "sunny")
    private String icon;

    @Schema(description = "是否休息班次(休息不计工时)", example = "false")
    private Boolean isRest;

    @Schema(description = "显示顺序", example = "1")
    private Integer sortOrder;

    public String getShiftCode() {
        return shiftCode;
    }

    public void setShiftCode(String shiftCode) {
        this.shiftCode = shiftCode;
    }

    public String getShiftName() {
        return shiftName;
    }

    public void setShiftName(String shiftName) {
        this.shiftName = shiftName;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Boolean getIsRest() {
        return isRest;
    }

    public void setIsRest(Boolean isRest) {
        this.isRest = isRest;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
