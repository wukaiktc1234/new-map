package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/**
 * 叫号记录实体类
 * 用于管理门店的排队叫号系统
 */
@TableName("call_number_records")
public class CallNumberRecord {

    /** 叫号记录ID */
    @TableId(type = IdType.AUTO)
    private Long callRecordId;

    /** 门店ID */
    private Long storeId;

    /** 取号号码 */
    private String ticketNumber;

    /**
     * 排队类型
     * 1-堂食 2-外卖 3-自提
     */
    private Integer queueType;

    /** 用餐人数 */
    private Integer customerCount;

    /** 桌号 */
    private String tableNumber;

    /** 叫号时间 */
    private LocalDateTime callTime;

    /** 用餐时间（开始用餐） */
    private LocalDateTime servedTime;

    /** 取消时间 */
    private LocalDateTime cancelTime;

    /**
     * 叫号状态
     * 1-等待 2-已叫号 3-已用餐 4-已取消
     */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    private Integer deleted;

    // ==================== Getter & Setter 方法 ====================

    public Long getCallRecordId() {
        return callRecordId;
    }

    public void setCallRecordId(Long callRecordId) {
        this.callRecordId = callRecordId;
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

    public Integer getCustomerCount() {
        return customerCount;
    }

    public void setCustomerCount(Integer customerCount) {
        this.customerCount = customerCount;
    }

    public String getTableNumber() {
        return tableNumber;
    }

    public void setTableNumber(String tableNumber) {
        this.tableNumber = tableNumber;
    }

    public LocalDateTime getCallTime() {
        return callTime;
    }

    public void setCallTime(LocalDateTime callTime) {
        this.callTime = callTime;
    }

    public LocalDateTime getServedTime() {
        return servedTime;
    }

    public void setServedTime(LocalDateTime servedTime) {
        this.servedTime = servedTime;
    }

    public LocalDateTime getCancelTime() {
        return cancelTime;
    }

    public void setCancelTime(LocalDateTime cancelTime) {
        this.cancelTime = cancelTime;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
