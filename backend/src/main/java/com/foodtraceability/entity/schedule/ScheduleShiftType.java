package com.foodtraceability.entity.schedule;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalTime;
import java.time.LocalDateTime;

/**
 * 班次类型配置实体类
 * 定义门店使用的班次类型(如早班/中班/晚班/休息)
 */
@TableName("schedule_shift_types")
public class ScheduleShiftType implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "shift_type_id", type = IdType.AUTO)
    private Long shiftTypeId;

    /**
     * 班次编码(内部标识)
     * morning-早班 noon-中班 evening-晚班 night_off-休息
     */
    @TableField("shift_code")
    private String shiftCode;

    /** 班次显示名称(如"早班(A)") */
    @TableField("shift_name")
    private String shiftName;

    /** 门店ID(班次按门店配置) */
    @TableField("store_id")
    private Long storeId;

    /** 开始时间(HH:mm格式) */
    @TableField("start_time")
    private LocalTime startTime;

    /** 结束时间(HH:mm格式,跨夜用次日时间如01:00) */
    @TableField("end_time")
    private LocalTime endTime;

    /**
     * 颜色标识(CSS颜色值或CSS变量名)
     * 如: #409EFF 或 --fts-primary
     */
    @TableField("color")
    private String color;

    /** 图标(可选) */
    @TableField("icon")
    private String icon;

    /** 持续时长(分钟,自动计算) */
    @TableField("duration_minutes")
    private Integer durationMinutes;

    /** 是否休息班次(休息不计工时) */
    @TableField("is_rest")
    private Boolean isRest;

    /** 显示顺序 */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 状态
     * active-启用 inactive-停用
     */
    @TableField("status")
    private String status;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // ==================== 班次编码常量 ====================

    /** 早班 */
    public static final String SHIFT_CODE_MORNING = "morning";
    /** 中班 */
    public static final String SHIFT_CODE_NOON = "noon";
    /** 晚班 */
    public static final String SHIFT_CODE_EVENING = "evening";
    /** 休息 */
    public static final String SHIFT_CODE_NIGHT_OFF = "night_off";

    // ==================== 状态常量 ====================

    /** 启用状态 */
    public static final String STATUS_ACTIVE = "active";
    /** 停用状态 */
    public static final String STATUS_INACTIVE = "inactive";

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

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
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
