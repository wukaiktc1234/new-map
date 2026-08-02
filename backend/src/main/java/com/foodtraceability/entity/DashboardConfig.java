package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableField;
import java.util.Date;

/**
 * 看板配置实体类
 * 用于存储用户个性化的看板配置信息
 */
@TableName("dashboard_configs")
public class DashboardConfig {

    /**
     * 配置ID
     */
    @TableId(type = IdType.AUTO)
    private Long configId;

    /**
     * 用户ID（支持个性化配置）
     */
    private Long userId;

    /**
     * 看板类型
     * 1总览 2销售 3库存 4财务 5会员
     */
    private Integer dashboardType;

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
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 删除标志
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

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

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
