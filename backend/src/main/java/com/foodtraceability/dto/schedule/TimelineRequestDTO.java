package com.foodtraceability.dto.schedule;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 时间线日历查询请求DTO
 * 用于获取指定周的排班时间线数据
 */
@Schema(description = "时间线日历查询请求DTO")
public class TimelineRequestDTO {

    /**
     * 周一日期(YYYY-MM-DD格式)
     * 系统根据此日期计算周一~周日的7天范围
     */
    @NotBlank(message = "周一起始日期不能为空")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "日期格式必须为YYYY-MM-DD")
    @Schema(description = "周一日期", example = "2026-05-11", requiredMode = Schema.RequiredMode.REQUIRED)
    private String weekStart;

    public String getWeekStart() {
        return weekStart;
    }

    public void setWeekStart(String weekStart) {
        this.weekStart = weekStart;
    }
}
