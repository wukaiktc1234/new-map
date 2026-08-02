package com.foodtraceability.entity.finance;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 发票报销审批记录实体
 *
 * <p>Sprint 3.1 P0 F-001：从 entity/ 根目录迁移至 entity/finance/，
 * 时间字段 created_at/updated_at → create_time/update_time，
 * 主键字段 id → record_id（雪花算法）。无金额字段。</p>
 */
@TableName("invoice_reimbursement_approval_record")
public class InvoiceReimbursementApprovalRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 审批记录ID（雪花算法） */
    @TableId(value = "record_id", type = IdType.ASSIGN_ID)
    private Long recordId;

    /** 租户ID */
    @TableField("tenant_id")
    private Long tenantId;

    /** 报销单ID（外键 → invoice_reimbursement.reimbursement_id） */
    @TableField("reimbursement_id")
    private Long reimbursementId;

    /** 操作人ID */
    @TableField("operator_id")
    private Long operatorId;

    /** 操作人姓名 */
    @TableField("operator_name")
    private String operatorName;

    /** 操作动作（submit/approve/reject/cancel/pay） */
    @TableField("action")
    private String action;

    /** 操作备注 */
    @TableField("remark")
    private String remark;

    /** 操作时间 */
    @TableField("operate_time")
    private LocalDateTime operateTime;

    /** 创建人ID */
    @TableField("created_by")
    private Long createdBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新人ID */
    @TableField("updated_by")
    private Long updatedBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标识（0-未删除 1-已删除） */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    public InvoiceReimbursementApprovalRecord() {
    }

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getReimbursementId() {
        return reimbursementId;
    }

    public void setReimbursementId(Long reimbursementId) {
        this.reimbursementId = reimbursementId;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getOperateTime() {
        return operateTime;
    }

    public void setOperateTime(LocalDateTime operateTime) {
        this.operateTime = operateTime;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
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
