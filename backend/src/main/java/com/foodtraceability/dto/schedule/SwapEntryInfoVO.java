package com.foodtraceability.dto.schedule;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 换班条目信息VO
 * 描述换班涉及的班次详情
 */
@Schema(description = "换班条目信息")
public class SwapEntryInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 条目ID */
    @Schema(description = "排班条目ID")
    private String entryId;

    /** 日期 (YYYY-MM-DD) */
    @Schema(description = "工作日期")
    private String date;

    /** 班次类型代码 */
    @Schema(description = "班次类型")
    private String shiftType;

    /** 班次名称 */
    @Schema(description = "班次名称")
    private String shiftName;

    /** 开始时间 (HH:mm) */
    @Schema(description = "开始时间")
    private String startTime;

    /** 结束时间 (HH:mm) */
    @Schema(description = "结束时间")
    private String endTime;

    public SwapEntryInfoVO() {}

    public SwapEntryInfoVO(String entryId, String date, String shiftType,
                           String shiftName, String startTime, String endTime) {
        this.entryId = entryId;
        this.date = date;
        this.shiftType = shiftType;
        this.shiftName = shiftName;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getEntryId() { return entryId; }
    public void setEntryId(String entryId) { this.entryId = entryId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getShiftType() { return shiftType; }
    public void setShiftType(String shiftType) { this.shiftType = shiftType; }

    public String getShiftName() { return shiftName; }
    public void setShiftName(String shiftName) { this.shiftName = shiftName; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
}
