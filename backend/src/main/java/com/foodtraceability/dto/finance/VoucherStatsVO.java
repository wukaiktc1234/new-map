package com.foodtraceability.dto.finance;

import java.io.Serializable;
import java.util.List;

/**
 * 凭证统计信息VO
 */
public class VoucherStatsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 凭证总数 */
    private Long totalVouchers;

    /** 待审核数 */
    private Long pendingReview;

    /** 今日已过账数 */
    private Long postedToday;

    /** 本月凭证数 */
    private Long thisMonthCount;

    /** 生成失败数 */
    private Long errorCount;

    /** 热门事件类型统计 */
    private List<EventTypeStat> topEventTypes;

    public Long getTotalVouchers() {
        return totalVouchers;
    }

    public void setTotalVouchers(Long totalVouchers) {
        this.totalVouchers = totalVouchers;
    }

    public Long getPendingReview() {
        return pendingReview;
    }

    public void setPendingReview(Long pendingReview) {
        this.pendingReview = pendingReview;
    }

    public Long getPostedToday() {
        return postedToday;
    }

    public void setPostedToday(Long postedToday) {
        this.postedToday = postedToday;
    }

    public Long getThisMonthCount() {
        return thisMonthCount;
    }

    public void setThisMonthCount(Long thisMonthCount) {
        this.thisMonthCount = thisMonthCount;
    }

    public Long getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(Long errorCount) {
        this.errorCount = errorCount;
    }

    public List<EventTypeStat> getTopEventTypes() {
        return topEventTypes;
    }

    public void setTopEventTypes(List<EventTypeStat> topEventTypes) {
        this.topEventTypes = topEventTypes;
    }

    /**
     * 事件类型统计
     */
    public static class EventTypeStat implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 事件类型 */
        private String type;

        /** 事件类型名称 */
        private String typeName;

        /** 凭证数量 */
        private Long count;

        public EventTypeStat() {
        }

        public EventTypeStat(String type, String typeName, Long count) {
            this.type = type;
            this.typeName = typeName;
            this.count = count;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getTypeName() {
            return typeName;
        }

        public void setTypeName(String typeName) {
            this.typeName = typeName;
        }

        public Long getCount() {
            return count;
        }

        public void setCount(Long count) {
            this.count = count;
        }
    }
}
