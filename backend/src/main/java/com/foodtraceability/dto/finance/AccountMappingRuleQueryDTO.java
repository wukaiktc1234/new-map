package com.foodtraceability.dto.finance;

import java.io.Serializable;

/**
 * 科目映射规则查询条件DTO
 */
public class AccountMappingRuleQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 事件类型 */
    private String eventType;

    /** 关键词（模糊匹配规则名称） */
    private String keyword;

    /** 当前页码 */
    private Integer pageNum = 1;

    /** 每页条数 */
    private Integer pageSize = 10;

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
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
