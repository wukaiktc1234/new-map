package com.foodtraceability.dto.marketing;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 财务流水查询DTO（分页）
 * 对应前端 GET /v1/recharge-finance
 */
@Schema(description = "财务流水查询请求")
public class RechargeFinanceLogQueryDTO {

    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @Schema(description = "每页条数", example = "10")
    private Integer size = 10;

    @Schema(description = "流水类型: recharge/consume/refund/bonus_expire/bonus_grant")
    private String financeType;

    @Schema(description = "开始时间（YYYY-MM-DD HH:mm:ss）")
    private String startTime;

    @Schema(description = "结束时间（YYYY-MM-DD HH:mm:ss）")
    private String endTime;

    // ==================== Getter & Setter ====================

    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }

    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }

    public String getFinanceType() { return financeType; }
    public void setFinanceType(String financeType) { this.financeType = financeType; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
}
