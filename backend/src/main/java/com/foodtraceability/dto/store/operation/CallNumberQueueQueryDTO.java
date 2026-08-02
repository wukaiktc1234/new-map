package com.foodtraceability.dto.store.operation;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 叫号记录查询DTO
 * 用于叫号队列的条件查询和分页
 */
@Schema(description = "叫号记录查询DTO")
public class CallNumberQueueQueryDTO {

    /** 门店ID */
    @Schema(description = "门店ID")
    private Long storeId;

    /** 状态：1等待 2已叫号 3已过号 4已用餐 5已取消 */
    @Schema(description = "状态：1等待 2已叫号 3已过号 4已用餐 5已取消")
    private Integer status;

    /** 排队类型：1堂食 2外卖 3自提 */
    @Schema(description = "排队类型：1堂食 2外卖 3自提")
    private Integer queueType;

    /** 关键词（号码） */
    @Schema(description = "关键词（号码）")
    private String keyword;

    /** 开始日期 */
    @Schema(description = "开始日期")
    private String startDate;

    /** 结束日期 */
    @Schema(description = "结束日期")
    private String endDate;

    /** 页码 */
    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    /** 每页条数 */
    @Schema(description = "每页条数", example = "20")
    private Integer size = 20;

    // ==================== Getter & Setter ====================

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getQueueType() {
        return queueType;
    }

    public void setQueueType(Integer queueType) {
        this.queueType = queueType;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

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

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
