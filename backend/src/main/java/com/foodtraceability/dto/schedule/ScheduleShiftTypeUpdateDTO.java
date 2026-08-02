package com.foodtraceability.dto.schedule;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalTime;

/**
 * 班次类型更新DTO
 */
@Schema(description = "班次类型更新DTO")
public class ScheduleShiftTypeUpdateDTO {

    @NotBlank(message = "班次名称不能为空")
    @Schema(description = "班次显示名称", example = "早班(A)", required = true)
    private String shiftName;

    @Schema(description = "开始时间(HH:mm格式)", example = "08:00")
    private LocalTime startTime;

    @Schema(description = "结束时间(HH:mm格式,跨夜用次日时间如01:00)", example = "16:00")
    private LocalTime endTime;

    @Schema(description = "颜色标识(CSS颜色值或CSS变量名)", example = "#409EFF")
    private String color;

    @Schema(description = "图标", example = "sunny")
    private String icon;

    @Schema(description = "是否休息班次(休息不计工时)")
    private Boolean isRest;

    public String getShiftName() {
        return shiftName;
    }

    public void setShiftName(String shiftName) {
        this.shiftName = shiftName;
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
}
