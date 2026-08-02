package com.foodtraceability.entity.marketing;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 储值财务流水实体类
 * 预留财务系统对接接口，记录每笔本金/赠送变动
 *
 * 流水类型（finance_type）：recharge/consume/refund/bonus_expire/bonus_grant
 */
@TableName("recharge_finance_logs")
public class RechargeFinanceLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 日志ID */
    @TableId(value = "log_id", type = IdType.ASSIGN_ID)
    private String logId;

    /** 关联流水号 */
    @TableField("record_no")
    private String recordNo;

    /** 会员ID */
    @TableField("member_id")
    private String memberId;

    /** 会员姓名 */
    @TableField("member_name")
    private String memberName;

    /** 流水类型 */
    @TableField("finance_type")
    private String financeType;

    /** 本金变动（分，正=增加负=减少） */
    @TableField("principal_change")
    private Long principalChange;

    /** 赠送变动（分） */
    @TableField("bonus_change")
    private Long bonusChange;

    /** 收入变动（分） */
    @TableField("revenue_change")
    private Long revenueChange;

    /** 变动后余额（分） */
    @TableField("balance_after")
    private Long balanceAfter;

    /** 变动后本金余额（分） */
    @TableField("principal_after")
    private Long principalAfter;

    /** 变动后赠送余额（分） */
    @TableField("bonus_after")
    private Long bonusAfter;

    /** 备注 */
    @TableField("remark")
    private String remark;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    // ==================== 状态常量 ====================
    /** 流水类型：充值 */
    public static final String TYPE_RECHARGE = "recharge";
    /** 流水类型：消费 */
    public static final String TYPE_CONSUME = "consume";
    /** 流水类型：退款 */
    public static final String TYPE_REFUND = "refund";
    /** 流水类型：赠送过期 */
    public static final String TYPE_BONUS_EXPIRE = "bonus_expire";
    /** 流水类型：赠送发放 */
    public static final String TYPE_BONUS_GRANT = "bonus_grant";

    // ==================== Getter & Setter ====================

    public String getLogId() { return logId; }
    public void setLogId(String logId) { this.logId = logId; }

    public String getRecordNo() { return recordNo; }
    public void setRecordNo(String recordNo) { this.recordNo = recordNo; }

    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public String getFinanceType() { return financeType; }
    public void setFinanceType(String financeType) { this.financeType = financeType; }

    public Long getPrincipalChange() { return principalChange; }
    public void setPrincipalChange(Long principalChange) { this.principalChange = principalChange; }

    public Long getBonusChange() { return bonusChange; }
    public void setBonusChange(Long bonusChange) { this.bonusChange = bonusChange; }

    public Long getRevenueChange() { return revenueChange; }
    public void setRevenueChange(Long revenueChange) { this.revenueChange = revenueChange; }

    public Long getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(Long balanceAfter) { this.balanceAfter = balanceAfter; }

    public Long getPrincipalAfter() { return principalAfter; }
    public void setPrincipalAfter(Long principalAfter) { this.principalAfter = principalAfter; }

    public Long getBonusAfter() { return bonusAfter; }
    public void setBonusAfter(Long bonusAfter) { this.bonusAfter = bonusAfter; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
