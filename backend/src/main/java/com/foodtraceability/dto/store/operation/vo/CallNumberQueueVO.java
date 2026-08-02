package com.foodtraceability.dto.store.operation.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 叫号队列视图对象VO
 * 用于返回给前端的叫号排队信息
 */
@Schema(description = "叫号队列视图对象")
public class CallNumberQueueVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 队列ID */
    @Schema(description = "队列ID")
    private Long queueId;

    /** 门店ID */
    @Schema(description = "门店ID")
    private Long storeId;

    /** 取号号码 */
    @Schema(description = "取号号码")
    private String ticketNumber;

    /** 排队类型编码 */
    @Schema(description = "排队类型编码")
    private Integer queueType;

    /** 排队类型名称 */
    @Schema(description = "排队类型名称")
    private String queueTypeName;

    /** 用餐人数 */
    @Schema(description = "用餐人数")
    private Integer peopleCount;

    /** 桌型偏好 */
    @Schema(description = "桌型偏好")
    private String tablePreference;

    /** 状态编码 */
    @Schema(description = "状态编码")
    private Integer status;

    /** 状态名称 */
    @Schema(description = "状态名称")
    private String statusName;

    /** 叫号时间 */
    @Schema(description = "叫号时间")
    private String callTime;

    /** 叫号次数 */
    @Schema(description = "叫号次数")
    private Integer calledCount;

    /** 等待时长(分钟) */
    @Schema(description = "等待时长(分钟)")
    private Long waitMinutes;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private String createTime;

    /** 更新时间 */
    @Schema(description = "更新时间")
    private String updateTime;

    // ==================== Getter & Setter ====================

    public Long getQueueId() {
        return queueId;
    }

    public void setQueueId(Long queueId) {
        this.queueId = queueId;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public Integer getQueueType() {
        return queueType;
    }

    public void setQueueType(Integer queueType) {
        this.queueType = queueType;
    }

    public String getQueueTypeName() {
        return queueTypeName;
    }

    public void setQueueTypeName(String queueTypeName) {
        this.queueTypeName = queueTypeName;
    }

    public Integer getPeopleCount() {
        return peopleCount;
    }

    public void setPeopleCount(Integer peopleCount) {
        this.peopleCount = peopleCount;
    }

    public String getTablePreference() {
        return tablePreference;
    }

    public void setTablePreference(String tablePreference) {
        this.tablePreference = tablePreference;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }

    public String getCallTime() {
        return callTime;
    }

    public void setCallTime(String callTime) {
        this.callTime = callTime;
    }

    public Integer getCalledCount() {
        return calledCount;
    }

    public void setCalledCount(Integer calledCount) {
        this.calledCount = calledCount;
    }

    public Long getWaitMinutes() {
        return waitMinutes;
    }

    public void setWaitMinutes(Long waitMinutes) {
        this.waitMinutes = waitMinutes;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }
}
