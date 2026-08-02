package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 叫号队列表实体类
 * 管理排队取号信息
 */
@TableName("call_number_queues")
@Schema(description = "叫号队列实体")
public class CallNumberQueueNew {

    /** 队列ID，主键自增 */
    @TableId(value = "queue_id", type = IdType.AUTO)
    @Schema(description = "队列ID", example = "1")
    private Long queueId;

    /** 店铺ID */
    @TableField("store_id")
    @Schema(description = "店铺ID", example = "1")
    private Long storeId;

    /** 取号号码 */
    @TableField("ticket_number")
    @Schema(description = "取号号码", example = "A001")
    private String ticketNumber;

    /**
     * 排队类型：
     * 1堂食 2外卖 3自提
     */
    @TableField("queue_type")
    @Schema(description = "排队类型: 1堂食 2外卖 3自提", example = "1")
    private Integer queueType;

    /** 用餐人数 */
    @TableField("people_count")
    @Schema(description = "用餐人数", example = "4")
    private Integer peopleCount;

    /** 桌型偏好（大桌/小桌/包厢） */
    @TableField("table_preference")
    @Schema(description = "桌型偏好", example = "大桌")
    private String tablePreference;

    /**
     * 状态：
     * 1等待 2已叫号 3已过号 4已用餐 5已取消
     */
    @TableField("status")
    @Schema(description = "状态: 1等待 2已叫号 3已过号 4已用餐 5已取消", example = "1")
    private Integer status;

    /** 叫号时间 */
    @TableField("call_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "叫号时间")
    private LocalDateTime callTime;

    /** 叫号次数 */
    @TableField("called_count")
    @Schema(description = "叫号次数", example = "0")
    private Integer calledCount;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记", example = "0")
    private Integer deleted;

    // Getter方法
    public Long getQueueId() { return queueId; }
    public Long getStoreId() { return storeId; }
    public String getTicketNumber() { return ticketNumber; }
    public Integer getQueueType() { return queueType; }
    public Integer getPeopleCount() { return peopleCount; }
    public String getTablePreference() { return tablePreference; }
    public Integer getStatus() { return status; }
    public LocalDateTime getCallTime() { return callTime; }
    public Integer getCalledCount() { return calledCount; }
    public LocalDateTime getCreateTime() { return createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public Integer getDeleted() { return deleted; }

    // Setter方法
    public void setQueueId(Long queueId) { this.queueId = queueId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public void setTicketNumber(String ticketNumber) { this.ticketNumber = ticketNumber; }
    public void setQueueType(Integer queueType) { this.queueType = queueType; }
    public void setPeopleCount(Integer peopleCount) { this.peopleCount = peopleCount; }
    public void setTablePreference(String tablePreference) { this.tablePreference = tablePreference; }
    public void setStatus(Integer status) { this.status = status; }
    public void setCallTime(LocalDateTime callTime) { this.callTime = callTime; }
    public void setCalledCount(Integer calledCount) { this.calledCount = calledCount; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
