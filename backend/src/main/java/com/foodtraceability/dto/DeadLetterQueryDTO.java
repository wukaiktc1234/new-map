package com.foodtraceability.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;

/**
 * 死信分页查询 DTO
 * 对应 spec NC-006（死信队列处理）和 plan.md 第 5.1.2 节
 */
public class DeadLetterQueryDTO {

    /** 渠道筛选（EMAIL / SITE_MSG / SMS / WEBHOOK） */
    private String channel;

    /** 处理状态：0=未处理 1=已处理 */
    private Integer resolved;

    /** 业务类型筛选 */
    private String bizType;

    /** 死信开始时间 */
    private LocalDateTime startTime;

    /** 死信结束时间 */
    private LocalDateTime endTime;

    /** 当前页码（默认 1） */
    @Min(value = 1, message = "页码最小为1")
    private Integer current = 1;

    /** 每页条数（默认 20，最大 100） */
    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 100, message = "每页条数最大为100")
    private Integer size = 20;

    public DeadLetterQueryDTO() {
    }

    public String getChannel() {
        return this.channel;
    }

    public Integer getResolved() {
        return this.resolved;
    }

    public String getBizType() {
        return this.bizType;
    }

    public LocalDateTime getStartTime() {
        return this.startTime;
    }

    public LocalDateTime getEndTime() {
        return this.endTime;
    }

    public Integer getCurrent() {
        return this.current;
    }

    public Integer getSize() {
        return this.size;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public void setResolved(Integer resolved) {
        this.resolved = resolved;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public void setCurrent(Integer current) {
        this.current = current;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
