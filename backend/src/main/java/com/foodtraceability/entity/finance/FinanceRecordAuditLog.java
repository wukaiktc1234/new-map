package com.foodtraceability.entity.finance;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;

/**
 * 财务收支记录审核日志实体类
 * @author example
 * @since 2025-12-07
 */
@TableName("finance_record_audit_log")
public class FinanceRecordAuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 财务记录ID
     */
    @TableField("record_id")
    private Long recordId;

    /**
     * 审核状态：待审核(PENDING)、已通过(APPROVED)、已拒绝(REJECTED)
     */
    @TableField("audit_status")
    private String auditStatus;

    /**
     * 审核人ID
     */
    @TableField("auditor_id")
    private Long auditorId;

    /**
     * 审核人名称
     */
    @TableField("auditor_name")
    private String auditorName;

    /**
     * 审核时间
     */
    @TableField("audit_time")
    private LocalDateTime auditTime;

    /**
     * 审核备注
     */
    @TableField("audit_remark")
    private String auditRemark;

    /**
     * 审核结果：通过(PASS)、拒绝(REJECT)
     */
    @TableField("audit_result")
    private String auditResult;

    /**
     * 审核类型：自动(AUTO)、手动(MANUAL)
     */
    @TableField("audit_type")
    private String auditType;

    // getter and setter methods
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public String getAuditStatus() {
        return auditStatus;
    }

    public void setAuditStatus(String auditStatus) {
        this.auditStatus = auditStatus;
    }

    public Long getAuditorId() {
        return auditorId;
    }

    public void setAuditorId(Long auditorId) {
        this.auditorId = auditorId;
    }

    public String getAuditorName() {
        return auditorName;
    }

    public void setAuditorName(String auditorName) {
        this.auditorName = auditorName;
    }

    public LocalDateTime getAuditTime() {
        return auditTime;
    }

    public void setAuditTime(LocalDateTime auditTime) {
        this.auditTime = auditTime;
    }

    public String getAuditRemark() {
        return auditRemark;
    }

    public void setAuditRemark(String auditRemark) {
        this.auditRemark = auditRemark;
    }

    public String getAuditResult() {
        return auditResult;
    }

    public void setAuditResult(String auditResult) {
        this.auditResult = auditResult;
    }

    public String getAuditType() {
        return auditType;
    }

    public void setAuditType(String auditType) {
        this.auditType = auditType;
    }

    @Override
    public String toString() {
        return "FinanceRecordAuditLog{" +
            "id=" + id +
            ", recordId=" + recordId +
            ", auditStatus='" + auditStatus + '\'' +
            ", auditorId=" + auditorId +
            ", auditorName='" + auditorName + '\'' +
            ", auditTime=" + auditTime +
            ", auditRemark='" + auditRemark + '\'' +
            ", auditResult='" + auditResult + '\'' +
            ", auditType='" + auditType + '\'' +
            '}';
    }
}
