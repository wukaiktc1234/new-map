package com.foodtraceability.dto.schedule;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 排班方案统计概览VO
 * 用于展示各状态的排班方案数量
 */
@Schema(description = "排班方案统计概览VO")
public class SchedulePlanStatsVO {

    @Schema(description = "排班方案总数")
    private Long totalCount;

    @Schema(description = "草稿数量")
    private Long draftCount;

    @Schema(description = "已发布数量")
    private Long publishedCount;

    @Schema(description = "执行中数量")
    private Long executingCount;

    public SchedulePlanStatsVO() {
    }

    public SchedulePlanStatsVO(Long totalCount, Long draftCount,
                                Long publishedCount, Long executingCount) {
        this.totalCount = totalCount;
        this.draftCount = draftCount;
        this.publishedCount = publishedCount;
        this.executingCount = executingCount;
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }

    public Long getDraftCount() {
        return draftCount;
    }

    public void setDraftCount(Long draftCount) {
        this.draftCount = draftCount;
    }

    public Long getPublishedCount() {
        return publishedCount;
    }

    public void setPublishedCount(Long publishedCount) {
        this.publishedCount = publishedCount;
    }

    public Long getExecutingCount() {
        return executingCount;
    }

    public void setExecutingCount(Long executingCount) {
        this.executingCount = executingCount;
    }
}
