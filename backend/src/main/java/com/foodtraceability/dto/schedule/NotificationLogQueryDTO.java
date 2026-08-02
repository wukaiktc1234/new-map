package com.foodtraceability.dto.schedule;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 通知日志查询DTO
 */
@Schema(description = "通知日志查询条件")
public class NotificationLogQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页码 */
    @Schema(description = "当前页码", example = "1")
    private Integer page = 1;

    /** 每页条数 */
    @Schema(description = "每页条数", example = "10")
    private Integer size = 10;

    /** 渠道筛选 */
    @Schema(description = "渠道筛选: in_app/sms/email/wechat_work")
    private String channel;

    /** 业务类型筛选 */
    @Schema(description = "业务类型筛选")
    private String businessType;

    /** 开始日期 */
    @Schema(description = "开始日期 (YYYY-MM-DD)")
    private String startDate;

    /** 结束日期 */
    @Schema(description = "结束日期 (YYYY-MM-DD)")
    private String endDate;

    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }

    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
}
