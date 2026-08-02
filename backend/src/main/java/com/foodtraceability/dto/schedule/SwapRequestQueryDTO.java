package com.foodtraceability.dto.schedule;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 换班请求查询DTO
 */
@Schema(description = "换班请求查询条件")
public class SwapRequestQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页码 */
    @Schema(description = "当前页码", example = "1")
    private Integer page = 1;

    /** 每页条数 */
    @Schema(description = "每页条数", example = "10")
    private Integer size = 10;

    /** 状态筛选 */
    @Schema(description = "状态筛选: pending/approved/rejected/cancelled")
    private String status;

    /** 方案ID筛选 */
    @Schema(description = "排班方案ID筛选")
    private String planId;

    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }

    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPlanId() { return planId; }
    public void setPlanId(String planId) { this.planId = planId; }
}
