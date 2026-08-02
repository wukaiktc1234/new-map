package com.foodtraceability.dto.trace;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 临期预警统计VO
 * 用于临期预警模块统计信息展示
 */
@Schema(description = "临期预警统计信息")
public class ExpiryStatisticsVO {

    /** 预警总数 */
    @Schema(description = "预警总数")
    private Integer totalAlerts;

    /** 红色预警数（已过期） */
    @Schema(description = "红色预警数")
    private Integer redAlerts;

    /** 黄色预警数（7天内） */
    @Schema(description = "黄色预警数")
    private Integer yellowAlerts;

    /** 绿色预警数（7天外） */
    @Schema(description = "绿色预警数")
    private Integer greenAlerts;

    /** 待处理数量 */
    @Schema(description = "待处理数量")
    private Integer pendingCount;

    /** 已报损数量 */
    @Schema(description = "已报损数量")
    private Integer scrappedCount;

    /** 已退货数量 */
    @Schema(description = "已退货数量")
    private Integer returnedCount;

    /** 已处理数量 */
    @Schema(description = "已处理数量")
    private Integer resolvedCount;

    // Getter和Setter方法

    public Integer getTotalAlerts() { return totalAlerts; }
    public void setTotalAlerts(Integer totalAlerts) { this.totalAlerts = totalAlerts; }
    public Integer getRedAlerts() { return redAlerts; }
    public void setRedAlerts(Integer redAlerts) { this.redAlerts = redAlerts; }
    public Integer getYellowAlerts() { return yellowAlerts; }
    public void setYellowAlerts(Integer yellowAlerts) { this.yellowAlerts = yellowAlerts; }
    public Integer getGreenAlerts() { return greenAlerts; }
    public void setGreenAlerts(Integer greenAlerts) { this.greenAlerts = greenAlerts; }
    public Integer getPendingCount() { return pendingCount; }
    public void setPendingCount(Integer pendingCount) { this.pendingCount = pendingCount; }
    public Integer getScrappedCount() { return scrappedCount; }
    public void setScrappedCount(Integer scrappedCount) { this.scrappedCount = scrappedCount; }
    public Integer getReturnedCount() { return returnedCount; }
    public void setReturnedCount(Integer returnedCount) { this.returnedCount = returnedCount; }
    public Integer getResolvedCount() { return resolvedCount; }
    public void setResolvedCount(Integer resolvedCount) { this.resolvedCount = resolvedCount; }
}
