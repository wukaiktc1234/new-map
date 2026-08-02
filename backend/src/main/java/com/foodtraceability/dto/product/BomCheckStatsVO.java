package com.foodtraceability.dto.product;

import java.io.Serializable;
import java.util.List;

/**
 * BOM 检查统计 VO
 * 用于 BOM 检查统计页面展示
 */
public class BomCheckStatsVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 总检查次数 */
    private Integer totalChecks;
    /** 阻塞订单数 */
    private Integer blockedOrders;
    /** 平均健康度评分 */
    private Double avgHealthScore;
    /** 短缺物料 TOP 列表 */
    private List<TopShortageMaterial> topShortageMaterials;

    /**
     * 短缺物料 TOP 项
     */
    public static class TopShortageMaterial implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 物料名称 */
        private String name;
        /** 短缺次数 */
        private Integer shortageCount;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getShortageCount() { return shortageCount; }
        public void setShortageCount(Integer shortageCount) { this.shortageCount = shortageCount; }
    }

    public Integer getTotalChecks() { return totalChecks; }
    public void setTotalChecks(Integer totalChecks) { this.totalChecks = totalChecks; }
    public Integer getBlockedOrders() { return blockedOrders; }
    public void setBlockedOrders(Integer blockedOrders) { this.blockedOrders = blockedOrders; }
    public Double getAvgHealthScore() { return avgHealthScore; }
    public void setAvgHealthScore(Double avgHealthScore) { this.avgHealthScore = avgHealthScore; }
    public List<TopShortageMaterial> getTopShortageMaterials() { return topShortageMaterials; }
    public void setTopShortageMaterials(List<TopShortageMaterial> topShortageMaterials) { this.topShortageMaterials = topShortageMaterials; }
}
