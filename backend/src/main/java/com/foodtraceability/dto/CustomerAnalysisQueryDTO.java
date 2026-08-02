package com.foodtraceability.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;

/**
 * 客户分析查询条件DTO
 * 用于封装客户分析的查询条件
 */
public class CustomerAnalysisQueryDTO {

    /**
     * 统计周期
     */
    @Size(max = 20, message = "周期长度不能超过20个字符")
    private String period;

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
    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
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
