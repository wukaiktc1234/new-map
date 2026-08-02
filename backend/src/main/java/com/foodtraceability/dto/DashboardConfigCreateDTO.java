package com.foodtraceability.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;

/**
 * 看板配置创建DTO
 * 用于创建或更新看板配置的请求参数
 */
public class DashboardConfigCreateDTO {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 看板类型
     * 1总览 2销售 3库存 4财务 5会员
     */
    @NotNull(message = "看板类型不能为空")
    @Min(value = 1, message = "看板类型最小值为1")
    @Max(value = 5, message = "看板类型最大值为5")
    private Integer dashboardType;

    /**
     * 组件布局配置JSON
     */
    private String widgetLayout;

    /**
     * 刷新间隔（秒）
     */
    @Min(value = 30, message = "刷新间隔最小为30秒")
    @Max(value = 3600, message = "刷新间隔最大为3600秒")
    private Integer refreshInterval = 300;

    /**
     * 是否默认配置
     */
    private Boolean isDefault = false;

    // getter和setter方法
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getDashboardType() {
        return dashboardType;
    }

    public void setDashboardType(Integer dashboardType) {
        this.dashboardType = dashboardType;
    }

    public String getWidgetLayout() {
        return widgetLayout;
    }

    public void setWidgetLayout(String widgetLayout) {
        this.widgetLayout = widgetLayout;
    }

    public Integer getRefreshInterval() {
        return refreshInterval;
    }

    public void setRefreshInterval(Integer refreshInterval) {
        this.refreshInterval = refreshInterval;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }
}
