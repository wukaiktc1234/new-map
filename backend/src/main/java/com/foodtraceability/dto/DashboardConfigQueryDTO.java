package com.foodtraceability.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 看板配置查询条件DTO
 * 用于封装看板配置的查询条件
 */
public class DashboardConfigQueryDTO {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 看板类型
     * 1总览 2销售 3库存 4财务 5会员
     */
    @Min(value = 1, message = "看板类型最小值为1")
    @Max(value = 5, message = "看板类型最大值为5")
    private Integer dashboardType;

    /**
     * 当前页码
     */
    @Min(value = 1, message = "页码最小值为1")
    private Integer pageNum = 1;

    /**
     * 每页条数
     */
    @Min(value = 1, message = "每页条数最小值为1")
    @Max(value = 100, message = "每页条数最大值为100")
    private Integer pageSize = 10;

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

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}
