package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 自动凭证日志实体类
 * 记录自动记账引擎的处理日志，用于审计追溯
 * @author example
 * @since 2026-04-04
 */
@TableName("auto_voucher_log")
public class AutoVoucherLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 日志ID（自增主键）
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 事件类型（如：POS_SALE_CASH、PURCHASE_INBOUND）
     */
    @TableField("event_type")
    private String eventType;

    /**
     * 源业务单据ID
     */
    @TableField("source_business_id")
    private String sourceBusinessId;

    /**
     * 源业务单据编号
     */
    @TableField("source_business_no")
    private String sourceBusinessNo;

    /**
     * 匹配的规则ID
     */
    @TableField("rule_id")
    private Long ruleId;

    /**
     * 生成的凭证ID
     */
    @TableField("voucher_id")
    private Long voucherId;

    /**
     * 生成的凭证编号
     */
    @TableField("voucher_no")
    private String voucherNo;

    /**
     * 处理状态：SUCCESS(成功)、FAILED(失败)、PENDING_REVIEW(待审核)、SKIPPED(跳过)、DUPLICATE(重复)
     */
    @TableField("status")
    private String status;

    /**
     * 质量评分（0-100分）
     */
    @TableField("quality_score")
    private Integer qualityScore;

    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 处理耗时（毫秒）
     */
    @TableField("process_time_ms")
    private Integer processTimeMs;

    /**
     * 操作人
     */
    @TableField("operator")
    private String operator;

    /**
     * 请求数据快照（JSON格式）
     */
    @TableField("request_payload")
    private String requestPayload;

    /**
     * 响应数据快照（JSON格式）
     */
    @TableField("response_snapshot")
    private String responseSnapshot;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // getter and setter methods
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getSourceBusinessId() {
        return sourceBusinessId;
    }

    public void setSourceBusinessId(String sourceBusinessId) {
        this.sourceBusinessId = sourceBusinessId;
    }

    public String getSourceBusinessNo() {
        return sourceBusinessNo;
    }

    public void setSourceBusinessNo(String sourceBusinessNo) {
        this.sourceBusinessNo = sourceBusinessNo;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public Long getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(Long voucherId) {
        this.voucherId = voucherId;
    }

    public String getVoucherNo() {
        return voucherNo;
    }

    public void setVoucherNo(String voucherNo) {
        this.voucherNo = voucherNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(Integer qualityScore) {
        this.qualityScore = qualityScore;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Integer getProcessTimeMs() {
        return processTimeMs;
    }

    public void setProcessTimeMs(Integer processTimeMs) {
        this.processTimeMs = processTimeMs;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getRequestPayload() {
        return requestPayload;
    }

    public void setRequestPayload(String requestPayload) {
        this.requestPayload = requestPayload;
    }

    public String getResponseSnapshot() {
        return responseSnapshot;
    }

    public void setResponseSnapshot(String responseSnapshot) {
        this.responseSnapshot = responseSnapshot;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    /**
     * 添加警告信息到错误消息
     * @param warning 警告信息
     */
    public void addWarning(String warning) {
        if (this.errorMessage == null || this.errorMessage.isEmpty()) {
            this.errorMessage = warning;
        } else {
            this.errorMessage = this.errorMessage + "; " + warning;
        }
    }

    @Override
    public String toString() {
        return "AutoVoucherLog{" +
            "id=" + id +
            ", eventType='" + eventType + '\'' +
            ", sourceBusinessId='" + sourceBusinessId + '\'' +
            ", sourceBusinessNo='" + sourceBusinessNo + '\'' +
            ", ruleId=" + ruleId +
            ", voucherId=" + voucherId +
            ", voucherNo='" + voucherNo + '\'' +
            ", status='" + status + '\'' +
            ", qualityScore=" + qualityScore +
            ", processTimeMs=" + processTimeMs +
            ", createTime=" + createTime +
            '}';
    }
}
