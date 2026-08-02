package com.foodtraceability.dto.trace;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 召回统计VO
 * 用于召回管理统计概览
 */
@Schema(description = "召回统计概览")
public class RecallStatisticsVO {

    /** 今日已召回数（status=4 且 update_time 在今天） */
    @Schema(description = "今日已召回数")
    private int totalRecalledToday;

    /** 待处理召回数（即将过期 status=2 且 risk_level>=2） */
    @Schema(description = "待处理召回数")
    private int pendingRecall;

    /** 高风险项数（risk_level=3） */
    @Schema(description = "高风险项数")
    private int highRiskItems;

    public int getTotalRecalledToday() { return totalRecalledToday; }
    public void setTotalRecalledToday(int totalRecalledToday) { this.totalRecalledToday = totalRecalledToday; }
    public int getPendingRecall() { return pendingRecall; }
    public void setPendingRecall(int pendingRecall) { this.pendingRecall = pendingRecall; }
    public int getHighRiskItems() { return highRiskItems; }
    public void setHighRiskItems(int highRiskItems) { this.highRiskItems = highRiskItems; }
}
