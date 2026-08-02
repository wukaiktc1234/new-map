package com.foodtraceability.dto.finance;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 财务统计查询DTO
 *
 * <p>Sprint 3.1 P0 F-002：用于 FinanceStatisticsController 查询条件，
 * 含 startDate / endDate / period（可选）。
 * 不传 period 时默认查询当月与上月对比。</p>
 */
public class FinanceStatisticsQueryDTO {

    /** 起始日期（与 period 二选一） */
    private LocalDate startDate;

    /** 结束日期（与 period 二选一） */
    private LocalDate endDate;

    /** 统计周期（如 "2026-06"，可选，优先于 startDate/endDate） */
    @Size(max = 10, message = "周期长度不能超过10个字符")
    private String period;

    public FinanceStatisticsQueryDTO() {
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }
}
