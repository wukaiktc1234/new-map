package com.foodtraceability.dto.trace;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 临期预警仪表盘VO
 * 用于临期预警首页仪表盘统计展示
 */
@Schema(description = "临期预警仪表盘统计")
public class ExpiryDashboardVO {

    /** 红色预警数（已过期） */
    @Schema(description = "红色预警数（已过期）")
    private Integer redCount;

    /** 黄色预警数（7天内） */
    @Schema(description = "黄色预警数（7天内）")
    private Integer yellowCount;

    /** 绿色预警数（7天外） */
    @Schema(description = "绿色预警数（7天外）")
    private Integer greenCount;

    /** 待处理总数 */
    @Schema(description = "待处理总数")
    private Integer totalPending;

    /** 已报损总数 */
    @Schema(description = "已报损总数")
    private Integer totalScrapped;

    /** 已退货总数 */
    @Schema(description = "已退货总数")
    private Integer totalReturned;

    /** 最近预警列表（最多10条） */
    @Schema(description = "最近预警列表（最多10条）")
    private List<ExpiryAlertVO> recentAlerts;

    // Getter和Setter方法

    public Integer getRedCount() { return redCount; }
    public void setRedCount(Integer redCount) { this.redCount = redCount; }
    public Integer getYellowCount() { return yellowCount; }
    public void setYellowCount(Integer yellowCount) { this.yellowCount = yellowCount; }
    public Integer getGreenCount() { return greenCount; }
    public void setGreenCount(Integer greenCount) { this.greenCount = greenCount; }
    public Integer getTotalPending() { return totalPending; }
    public void setTotalPending(Integer totalPending) { this.totalPending = totalPending; }
    public Integer getTotalScrapped() { return totalScrapped; }
    public void setTotalScrapped(Integer totalScrapped) { this.totalScrapped = totalScrapped; }
    public Integer getTotalReturned() { return totalReturned; }
    public void setTotalReturned(Integer totalReturned) { this.totalReturned = totalReturned; }
    public List<ExpiryAlertVO> getRecentAlerts() { return recentAlerts; }
    public void setRecentAlerts(List<ExpiryAlertVO> recentAlerts) { this.recentAlerts = recentAlerts; }
}
