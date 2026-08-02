package com.foodtraceability.dto.operations;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * 经营报表统一查询DTO
 * 所有经营报表查询接口统一参数
 */
public class OperationsReportQueryDTO {

    /**
     * 开始日期 yyyy-MM-dd
     */
    @NotBlank(message = "开始日期不能为空")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "开始日期格式必须为yyyy-MM-dd")
    private String startDate;

    /**
     * 结束日期 yyyy-MM-dd
     */
    @NotBlank(message = "结束日期不能为空")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "结束日期格式必须为yyyy-MM-dd")
    private String endDate;

    /**
     * 门店ID列表（空表示全部有权限门店）
     */
    @Size(max = 10, message = "最多选择10个门店")
    private List<String> storeIds;

    /**
     * 渠道：dine_in/takeout/self_pickup
     */
    private String channel;

    /**
     * 对比维度：yoy/mom/none
     */
    private String compareType;

    // ========== Getter & Setter ==========

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public List<String> getStoreIds() {
        return storeIds;
    }

    public void setStoreIds(List<String> storeIds) {
        this.storeIds = storeIds;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getCompareType() {
        return compareType;
    }

    public void setCompareType(String compareType) {
        this.compareType = compareType;
    }

    /**
     * 校验日期范围是否合法（开始日期 <= 结束日期）
     * @return 校验结果
     */
    public boolean isDateRangeValid() {
        if (startDate == null || endDate == null) {
            return false;
        }
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            return !start.isAfter(end);
        } catch (Exception e) {
            return false;
        }
    }
}
