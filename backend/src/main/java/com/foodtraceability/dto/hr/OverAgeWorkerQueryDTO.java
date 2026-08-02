package com.foodtraceability.dto.hr;

import java.io.Serializable;

/**
 * 超龄劳动者查询DTO
 */
public class OverAgeWorkerQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前页码
     */
    private Integer current;

    /**
     * 每页条数
     */
    private Integer size;

    /**
     * 关键字搜索（员工姓名/员工ID/协议编号）
     */
    private String keyword;

    /**
     * 健康体检结果筛选
     */
    private String healthCheckResult;

    public Integer getCurrent() {
        return current;
    }

    public void setCurrent(Integer current) {
        this.current = current;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getHealthCheckResult() {
        return healthCheckResult;
    }

    public void setHealthCheckResult(String healthCheckResult) {
        this.healthCheckResult = healthCheckResult;
    }
}
