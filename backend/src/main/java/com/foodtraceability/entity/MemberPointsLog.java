package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会员积分变动日志实体类
 * 记录会员积分的所有变动，包括获取、使用、过期等
 */
@TableName("member_points_log")
public class MemberPointsLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 日志ID（自增主键） */
    @TableId(type = IdType.AUTO)
    @TableField("log_id")
    private Long logId;

    /** 会员ID */
    @TableField("member_id")
    private Long memberId;

    /**
     * 变动类型
     * 1消费获得 2使用抵扣 3签到奖励 4活动赠送
     * 5管理员调整 6过期清零 7充值赠送 8推荐奖励
     */
    @TableField("change_type")
    private Integer changeType;

    /** 变动积分数（正数增加，负数减少） */
    @TableField("change_points")
    private Integer changePoints;

    /** 变动后余额 */
    @TableField("balance_after")
    private Integer balanceAfter;

    /** 关联单据号：ORDERxxx/ACTxxx/RECxxx */
    @TableField("reference_no")
    private String referenceNo;

    /**
     * 关联单据类型
     * 1订单 2活动 3签到 4管理员 5充值 6推荐
     */
    @TableField("reference_type")
    private Integer referenceType;

    /** 操作人ID（管理员调整时） */
    @TableField("operator_id")
    private Long operatorId;

    /** 操作人名称 */
    @TableField("operator_name")
    private String operatorName;

    /** 备注 */
    @TableField("remark")
    private String remark;

    /** 过期时间（NULL=永不过期） */
    @TableField("expire_time")
    private LocalDateTime expireTime;

    /** 创建时间 */
    @TableField("create_time")
    private LocalDateTime createTime;

    // ==================== 非持久化字段 ====================

    /** 会员手机号（用于展示） */
    @TableField(exist = false)
    private String memberPhone;

    /** 会员昵称（用于展示） */
    @TableField(exist = false)
    private String memberNickname;

    // ==================== Getter & Setter ====================

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

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

    public Integer getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(Integer balanceAfter) {
        this.balanceAfter = balanceAfter;
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

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getMemberPhone() {
        return memberPhone;
    }

    public void setMemberPhone(String memberPhone) {
        this.memberPhone = memberPhone;
    }

    public String getMemberNickname() {
        return memberNickname;
    }

    public void setMemberNickname(String memberNickname) {
        this.memberNickname = memberNickname;
    }
}
