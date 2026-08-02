package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 召回记录实体类
 * 记录每一次召回操作的审计信息，符合食品安全法召回留痕要求
 *
 * @author demo
 * @since 2026-07-17
 */
@TableName("recall_record")
@Schema(description = "召回记录")
public class RecallRecord {

    /** 召回记录ID */
    @TableId(value = "recall_id", type = IdType.AUTO)
    private Long recallId;

    /** 关联批次号 */
    @TableField("batch_no")
    private String batchNo;

    /** 关联追溯码（可选，单码召回时填写） */
    @TableField("trace_code")
    private String traceCode;

    /** 召回原因 */
    @TableField("recall_reason")
    private String recallReason;

    /** 发起人ID */
    @TableField("initiator_id")
    private Long initiatorId;

    /** 发起人姓名 */
    @TableField("initiator_name")
    private String initiatorName;

    /** 召回发起时间 */
    @TableField("recall_time")
    private LocalDateTime recallTime;

    /** 受影响订单数（执行召回时快照） */
    @TableField("affected_order_count")
    private Integer affectedOrderCount;

    /** 受影响客户数（执行召回时快照） */
    @TableField("affected_customer_count")
    private Integer affectedCustomerCount;

    /** 受影响追溯码数（执行召回时快照） */
    @TableField("affected_trace_code_count")
    private Integer affectedTraceCodeCount;

    /** 召回状态：1-进行中，2-已完成，3-已取消 */
    @TableField("status")
    private Integer status;

    /** 备注 */
    @TableField("remark")
    private String remark;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标记（0-正常，1-删除） */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    public RecallRecord() {
    }

    public Long getRecallId() {
        return this.recallId;
    }

    public String getBatchNo() {
        return this.batchNo;
    }

    public String getTraceCode() {
        return this.traceCode;
    }

    public String getRecallReason() {
        return this.recallReason;
    }

    public Long getInitiatorId() {
        return this.initiatorId;
    }

    public String getInitiatorName() {
        return this.initiatorName;
    }

    public LocalDateTime getRecallTime() {
        return this.recallTime;
    }

    public Integer getAffectedOrderCount() {
        return this.affectedOrderCount;
    }

    public Integer getAffectedCustomerCount() {
        return this.affectedCustomerCount;
    }

    public Integer getAffectedTraceCodeCount() {
        return this.affectedTraceCodeCount;
    }

    public Integer getStatus() {
        return this.status;
    }

    public String getRemark() {
        return this.remark;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    public Integer getDeleted() {
        return this.deleted;
    }

    public void setRecallId(final Long recallId) {
        this.recallId = recallId;
    }

    public void setBatchNo(final String batchNo) {
        this.batchNo = batchNo;
    }

    public void setTraceCode(final String traceCode) {
        this.traceCode = traceCode;
    }

    public void setRecallReason(final String recallReason) {
        this.recallReason = recallReason;
    }

    public void setInitiatorId(final Long initiatorId) {
        this.initiatorId = initiatorId;
    }

    public void setInitiatorName(final String initiatorName) {
        this.initiatorName = initiatorName;
    }

    public void setRecallTime(final LocalDateTime recallTime) {
        this.recallTime = recallTime;
    }

    public void setAffectedOrderCount(final Integer affectedOrderCount) {
        this.affectedOrderCount = affectedOrderCount;
    }

    public void setAffectedCustomerCount(final Integer affectedCustomerCount) {
        this.affectedCustomerCount = affectedCustomerCount;
    }

    public void setAffectedTraceCodeCount(final Integer affectedTraceCodeCount) {
        this.affectedTraceCodeCount = affectedTraceCodeCount;
    }

    public void setStatus(final Integer status) {
        this.status = status;
    }

    public void setRemark(final String remark) {
        this.remark = remark;
    }

    public void setCreateTime(final LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public void setUpdateTime(final LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public void setDeleted(final Integer deleted) {
        this.deleted = deleted;
    }
}
