package com.foodtraceability.dto.schedule;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 班次类型视图对象
 */
@Schema(description = "班次类型VO")
public class ScheduleShiftTypeVO {

    @Schema(description = "主键ID")
    private Long shiftTypeId;

    @Schema(description = "班次编码(内部标识)")
    private String shiftCode;

    @Schema(description = "班次显示名称")
    private String shiftName;

    @Schema(description = "门店ID")
    private Long storeId;

    @Schema(description = "开始时间(HH:mm格式)")
    private LocalTime startTime;

    @Schema(description = "结束时间(HH:mm格式)")
    private LocalTime endTime;

    @Schema(description = "颜色标识")
    private String color;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "持续时长(分钟)")
    private Integer durationMinutes;

    @Schema(description = "是否休息班次")
    private Boolean isRest;

    @Schema(description = "显示顺序")
    private Integer sortOrder;

    @Schema(description = "状态(active:启用, inactive:停用)")
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    // ==================== Getter & Setter ====================

    public Long getShiftTypeId() {
        return shiftTypeId;
    }

    public void setShiftTypeId(Long shiftTypeId) {
        this.shiftTypeId = shiftTypeId;
    }

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

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
}
