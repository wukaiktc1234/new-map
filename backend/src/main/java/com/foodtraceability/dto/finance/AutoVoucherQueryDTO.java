package com.foodtraceability.dto.finance;

import java.io.Serializable;

/**
 * 自动凭证查询条件DTO
 */
public class AutoVoucherQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 起始日期（yyyy-MM-dd） */
    private String startDate;

    /** 结束日期（yyyy-MM-dd） */
    private String endDate;

    /** 凭证状态（pending/reviewed/posted/voided） */
    private String status;

    /** 事件类型 */
    private String eventType;

    /** 当前页码 */
    private Integer pageNum = 1;

    /** 每页条数 */
    private Integer pageSize = 10;

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
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
