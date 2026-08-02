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
 * 赠送余额明细实体类
 * 支持赠送金额过期管理，每笔充值赠送独立记录
 *
 * 状态（status）：active-有效 expired-过期 refunded-已退
 */
@TableName("bonus_balance_details")
public class BonusBalanceDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 明细ID */
    @TableId(value = "bonus_id", type = IdType.ASSIGN_ID)
    private String bonusId;

    /** 会员ID */
    @TableField("member_id")
    private String memberId;

    /** 关联充值记录ID */
    @TableField("recharge_record_id")
    private String rechargeRecordId;

    /** 赠送金额（分） */
    @TableField("bonus_amount")
    private Long bonusAmount;

    /** 剩余金额（分） */
    @TableField("remaining_amount")
    private Long remainingAmount;

    /** 过期时间（空=永不过期） */
    @TableField("expire_time")
    private LocalDateTime expireTime;

    /** 状态 */
    @TableField("status")
    private String status;

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
    /** 状态：有效 */
    public static final String STATUS_ACTIVE = "active";
    /** 状态：过期 */
    public static final String STATUS_EXPIRED = "expired";
    /** 状态：已退 */
    public static final String STATUS_REFUNDED = "refunded";

    // ==================== Getter & Setter ====================

    public String getBonusId() { return bonusId; }
    public void setBonusId(String bonusId) { this.bonusId = bonusId; }

    public String getMemberId() { return memberId; }
    public void setMemberId(String memberId) { this.memberId = memberId; }

    public String getRechargeRecordId() { return rechargeRecordId; }
    public void setRechargeRecordId(String rechargeRecordId) { this.rechargeRecordId = rechargeRecordId; }

    public Long getBonusAmount() { return bonusAmount; }
    public void setBonusAmount(Long bonusAmount) { this.bonusAmount = bonusAmount; }

    public Long getRemainingAmount() { return remainingAmount; }
    public void setRemainingAmount(Long remainingAmount) { this.remainingAmount = remainingAmount; }

    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
