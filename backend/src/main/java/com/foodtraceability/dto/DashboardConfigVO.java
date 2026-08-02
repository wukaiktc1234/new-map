package com.foodtraceability.dto;

/**
 * 看板配置视图对象
 * 用于返回看板配置的详细信息给前端
 */
public class DashboardConfigVO {

    /**
     * 配置ID
     */
    private Long configId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 看板类型
     * 1总览 2销售 3库存 4财务 5会员
     */
    private Integer dashboardType;

    /**
     * 看板类型名称
     */
    private String dashboardTypeName;

    /**
     * 组件布局配置JSON
     */
    private String widgetLayout;

    /**
     * 刷新间隔（秒）
     */
    private Integer refreshInterval;

    /**
     * 是否默认配置
     */
    private Boolean isDefault;

    /**
     * 创建时间
     */
    private String createTime;

    // getter和setter方法
    public Long getConfigId() {
        return configId;
    }

    public void setConfigId(Long configId) {
        this.configId = configId;
    }

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

    public String getDashboardTypeName() {
        return dashboardTypeName;
    }

    public void setDashboardTypeName(String dashboardTypeName) {
        this.dashboardTypeName = dashboardTypeName;
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

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }
}
