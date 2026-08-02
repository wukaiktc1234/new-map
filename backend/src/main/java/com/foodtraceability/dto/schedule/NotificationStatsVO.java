package com.foodtraceability.dto.schedule;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 通知发送统计VO
 */
@Schema(description = "通知发送统计")
public class NotificationStatsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 总数 */
    @Schema(description = "通知总数")
    private Integer total;

    /** 成功数 */
    @Schema(description = "发送成功数")
    private Integer success;

    /** 失败数 */
    @Schema(description = "发送失败数")
    private Integer failed;

    /** 待发送数 */
    @Schema(description = "待发送数")
    private Integer pending;

    /** 发送中数 */
    @Schema(description = "发送中数")
    private Integer sending;

    /** 成功率（百分比） */
    @Schema(description = "发送成功率(%)")
    private Double successRate;

    public Integer getTotal() { return total; }
    public void setTotal(Integer total) { this.total = total; }

    public Integer getSuccess() { return success; }
    public void setSuccess(Integer success) { this.success = success; }

    public Integer getFailed() { return failed; }
    public void setFailed(Integer failed) { this.failed = failed; }

    public Integer getPending() { return pending; }
    public void setPending(Integer pending) { this.pending = pending; }

    public Integer getSending() { return sending; }
    public void setSending(Integer sending) { this.sending = sending; }

    public Double getSuccessRate() { return successRate; }
    public void setSuccessRate(Double successRate) { this.successRate = successRate; }
}
