package com.foodtraceability.dto.marketing;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 积分变动请求DTO
 * 用于积分获取、使用、调整等操作
 */
public class PointsChangeDTO {

    /** 会员ID */
    @NotNull(message = "会员ID不能为空")
    private Long memberId;

    /**
     * 变动类型
     * 1消费获得 2使用抵扣 3签到奖励 4活动赠送
     * 5管理员调整 6过期清零 7充值赠送 8推荐奖励
     */
    @NotNull(message = "变动类型不能为空")
    private Integer changeType;

    /** 变动积分数（正数增加，负数减少） */
    @NotNull(message = "变动积分不能为空")
    private Integer changePoints;

    /** 关联单据号：ORDERxxx/ACTxxx/RECxxx */
    private String referenceNo;

    /**
     * 关联单据类型
     * 1订单 2活动 3签到 4管理员 5充值 6推荐
     */
    private Integer referenceType;

    /** 操作人ID（管理员调整时） */
    private Long operatorId;

    /** 操作人名称 */
    private String operatorName;

    /** 备注 */
    private String remark;

    // ==================== Getter & Setter ====================

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(Long memberId) {
        this.memberId = memberId;
    }

    public Integer getChangeType() {
        return changeType;
    }

    public void setChangeType(Integer changeType) {
        this.changeType = changeType;
    }

    public Integer getChangePoints() {
        return changePoints;
    }

    public void setChangePoints(Integer changePoints) {
        this.changePoints = changePoints;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public Integer getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(Integer referenceType) {
        this.referenceType = referenceType;
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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
