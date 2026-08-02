package com.foodtraceability.entity.finance;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.util.Date;

/**
 * 财务审批实体类
 * 用于存储财务审批的基本信息
 */
@TableName("finance_approval")
public class FinanceApproval {
    
    /**
     * 审批ID
     */
    @TableId(value = "approval_id", type = IdType.AUTO)
    private Long approvalId;
    
    /**
     * 业务类型
     * INCOME: 收入审批
     * EXPENSE: 支出审批
     * PURCHASE: 采购审批
     * REFUND: 退款审批
     * OTHER: 其他
     */
    private String businessType;
    
    /**
     * 业务ID
     */
    private Long businessId;
    
    /**
     * 审批金额
     */
    private java.math.BigDecimal amount;
    
    /**
     * 审批状态
     * DRAFT: 草稿
     * SUBMITTED: 已提交
     * APPROVED: 已通过
     * REJECTED: 已拒绝
     * CANCELLED: 已取消
     */
    private String status;
    
    /**
     * 审批标题
     */
    private String title;
    
    /**
     * 审批内容
     */
    private String content;
    
    /**
     * 申请人ID
     */
    private Long applicantId;
    
    /**
     * 申请人姓名
     */
    private String applicantName;
    
    /**
     * 审批人ID
     */
    private Long approverId;
    
    /**
     * 审批人姓名
     */
    private String approverName;
    
    /**
     * 审批意见
     */
    private String approvalComment;
    
    /**
     * 申请时间
     */
    private Date applyTime;
    
    /**
     * 审批时间
     */
    private Date approveTime;
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 更新时间
     */
    private Date updateTime;
    
    // getter and setter methods
    public Long getApprovalId() {
        return approvalId;
    }
    
    public void setApprovalId(Long approvalId) {
        this.approvalId = approvalId;
    }
    
    public String getBusinessType() {
        return businessType;
    }
    
    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }
    
    public Long getBusinessId() {
        return businessId;
    }
    
    public void setBusinessId(Long businessId) {
        this.businessId = businessId;
    }
    
    public java.math.BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(java.math.BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public Long getApplicantId() {
        return applicantId;
    }
    
    public void setApplicantId(Long applicantId) {
        this.applicantId = applicantId;
    }
    
    public String getApplicantName() {
        return applicantName;
    }
    
    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }
    
    public Long getApproverId() {
        return approverId;
    }
    
    public void setApproverId(Long approverId) {
        this.approverId = approverId;
    }
    
    public String getApproverName() {
        return approverName;
    }
    
    public void setApproverName(String approverName) {
        this.approverName = approverName;
    }
    
    public String getApprovalComment() {
        return approvalComment;
    }
    
    public void setApprovalComment(String approvalComment) {
        this.approvalComment = approvalComment;
    }
    
    public Date getApplyTime() {
        return applyTime;
    }
    
    public void setApplyTime(Date applyTime) {
        this.applyTime = applyTime;
    }
    
    public Date getApproveTime() {
        return approveTime;
    }
    
    public void setApproveTime(Date approveTime) {
        this.approveTime = approveTime;
    }
    
    public Date getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
    
    public Date getUpdateTime() {
        return updateTime;
    }
    
    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
